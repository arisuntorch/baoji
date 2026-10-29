package com.baoji.data

import com.baoji.data.catalog.ExerciseItem
import com.baoji.data.catalog.FoodItem
import com.baoji.data.db.ActivityKind
import com.baoji.data.io.BackupRestorer
import com.baoji.data.io.DbSnapshot
import com.baoji.data.io.ExportRange
import com.baoji.data.io.Exporter
import com.baoji.data.io.ImportMode
import com.baoji.data.io.ImportValidator
import com.baoji.data.io.PlanImporter
import com.baoji.data.io.ValidationResult
import com.baoji.data.repo.ActivityRepository
import com.baoji.data.repo.DietRepository
import com.baoji.data.repo.EnergyRepository
import com.baoji.data.repo.ProfileInput
import com.baoji.data.repo.ProfileRepository
import com.baoji.data.repo.TrainingRepository
import com.baoji.domain.Sex
import com.baoji.feature.training.activityDetail
import com.baoji.testing.TestEnv
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/** Reset, swipe-delete, catalog foods, timed activities and steps. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExtraLoggingTest {

    // 2026-10-12 is a Monday, matching the plan's days.
    private val env = TestEnv(LocalDateTime.of(2026, 10, 12, 12, 0))
    private val training = TrainingRepository(env.db, env.time)
    private val diet = DietRepository(env.db, env.settings, env.time)
    private val activity = ActivityRepository(env.db, env.time)
    private val rice = FoodItem("米饭（蒸，代表值）", "谷类", 116.0, 2.6, 0.3, 25.9)
    private val run = ExerciseItem("跑步（约 9.5 km/h）", "跑步", 9.8)
    private val pushdown = ExerciseItem("绳索下压［三头下压］", "手臂", 3.5, strength = true)
    private lateinit var benchId: String
    private lateinit var rowId: String
    private lateinit var mealId: String

    @Before
    fun seed() = runBlocking {
        ProfileRepository(env.db, env.time).create(ProfileInput(175.0, 70.0, Sex.MALE, 30))
        val importer = PlanImporter(env.db, env.settings, env.time)
        val trainingJson = """
            { "app": "baoji", "type": "training", "version": 1, "plans": [ { "name": "上肢 A", "days": [1], "exercises": [
                { "name": "卧推", "sets": 2, "reps": [6, 8], "weight": 60, "kcalPerSet": 8 },
                { "name": "划船", "sets": 1, "reps": [8, 10], "weight": 50, "kcalPerSet": 6 } ] } ] }
        """.trimIndent()
        val dietJson = """
            { "app": "baoji", "type": "diet", "version": 1, "targets": { "protein": 150, "carbs": 150, "fat": 50 },
              "meals": [ { "name": "练前鸡蛋", "amount": 2, "unit": "个", "protein": 12, "carbs": 1, "fat": 10 } ] }
        """.trimIndent()
        importer.importTraining((ImportValidator.validateTraining(trainingJson) as ValidationResult.Ok).value, ImportMode.REPLACE)
        importer.importDiet((ImportValidator.validateDiet(dietJson) as ValidationResult.Ok).value, ImportMode.REPLACE)
        val ex = env.db.planDao().activeExercises()
        benchId = ex.first { it.name == "卧推" }.id
        rowId = ex.first { it.name == "划船" }.id
        mealId = env.db.mealDao().activeTasks().single().id
    }

    @After
    fun tearDown() = env.close()

    private suspend fun trainingDay() = training.observeDay(flowOf(env.time.today())).first()
    private suspend fun dietDay() = diet.observeDay(flowOf(env.time.today())).first()

    @Test
    fun resetClearsAllSetsOfOneExerciseAndReopensTheSession() = runBlocking {
        training.completeSet(benchId); training.completeSet(benchId); training.completeSet(rowId)
        assertThat(trainingDay().session!!.finishedAt).isNotNull()

        assertThat(training.reset(benchId)).isTrue()
        val day = trainingDay()
        assertThat(day.rows.first { it.exercise.id == benchId }.done).isEqualTo(0)
        assertThat(day.rows.first { it.exercise.id == rowId }.done).isEqualTo(1)
        assertThat(day.session!!.finishedAt).isNull()
        assertThat(training.reset(benchId)).isFalse()
    }

    @Test
    fun archivingTheLastOpenExerciseFinishesTheDayAndKeepsHistory() = runBlocking {
        training.completeSet(benchId); training.completeSet(benchId); training.completeSet(rowId)
        training.reset(rowId)
        training.archive(rowId)
        val day = trainingDay()
        assertThat(day.rows.map { it.exercise.id }).containsExactly(benchId)
        assertThat(day.session!!.finishedAt).isNotNull()
        assertThat(env.db.planDao().allExercises().first { it.id == rowId }.archived).isTrue()
    }

    @Test
    fun deletingEveryExerciseKeepsAFinishedDayFinishedAndUndoRestores() = runBlocking {
        training.completeSet(benchId); training.completeSet(benchId); training.completeSet(rowId)
        training.archive(benchId)
        training.archive(rowId)
        assertThat(trainingDay().session!!.finishedAt).isNotNull()

        training.unarchive(benchId)
        val day = trainingDay()
        assertThat(day.rows.map { it.exercise.id }).containsExactly(benchId)
        assertThat(day.rows.single().done).isEqualTo(2)
    }

    @Test
    fun deletedExtrasCanBePutBack(): Unit = runBlocking {
        diet.addFood(rice, 100.0)
        val log = dietDay().extras.single()
        diet.deleteExtra(log.id)
        diet.restoreExtra(log)
        assertThat(dietDay().extras).containsExactly(log)

        activity.addTimed(run, 20.0)
        val timed = activity.observeDay(flowOf(env.time.today())).first().workouts.single()
        activity.delete(timed.id)
        activity.restore(timed)
        assertThat(activity.observeDay(flowOf(env.time.today())).first().workouts).containsExactly(timed)
    }

    @Test
    fun mealResetAndArchive() = runBlocking {
        diet.toggle(mealId)
        assertThat(diet.reset(mealId)).isTrue()
        assertThat(dietDay().rows.single().checked).isFalse()
        assertThat(diet.reset(mealId)).isFalse()

        diet.toggle(mealId)
        diet.archive(mealId)
        val day = dietDay()
        assertThat(day.rows).isEmpty()
        // The check made before deleting still counts for today.
        assertThat(day.eaten.kcal).isEqualTo(142.0)

        diet.unarchive(mealId)
        assertThat(dietDay().rows.single().checked).isTrue()
    }

    @Test
    fun catalogFoodsAreLoggedByGramsAndCanBeDeleted() = runBlocking {
        diet.addFood(rice, 250.0)
        diet.addFood(rice, 100.0)
        val day = dietDay()
        assertThat(day.extras).hasSize(2)
        assertThat(day.extras.first().kcal).isEqualTo(290.0)
        assertThat(day.extras.first().unit).isEqualTo("克")
        assertThat(day.eaten.kcal).isWithin(1e-9).of(406.0)
        assertThat(day.rows.single().checked).isFalse()

        assertThat(diet.deleteExtra(day.extras.first().id)).isTrue()
        assertThat(dietDay().eaten.kcal).isWithin(1e-9).of(116.0)
    }

    @Test
    fun stepsAreOnePerDayAndZeroClears() = runBlocking {
        assertThat(activity.setSteps(5000)).isTrue()
        assertThat(activity.setSteps(8000)).isTrue()
        val steps = activity.observeDay(flowOf(env.time.today())).first().steps!!
        assertThat(steps.amount).isEqualTo(8000.0)
        assertThat(steps.kind).isEqualTo(ActivityKind.STEPS)
        assertThat(steps.kcal).isWithin(0.1).of(212.1)

        activity.setSteps(0)
        assertThat(activity.observeDay(flowOf(env.time.today())).first().steps).isNull()
    }

    @Test
    fun strengthMovesAreLoggedBySetsRepsAndWeight(): Unit = runBlocking {
        assertThat(activity.addSets(pushdown, sets = 3, reps = 12, weightKg = 15.0)).isTrue()
        assertThat(activity.addSets(pushdown, sets = 2, reps = 15, weightKg = null)).isTrue()
        val (loaded, bodyweight) = activity.observeDay(flowOf(env.time.today())).first().workouts
        assertThat(loaded.kind).isEqualTo(ActivityKind.SETS)
        assertThat(loaded.amount).isEqualTo(3.0)
        assertThat(loaded.reps).isEqualTo(12)
        assertThat(loaded.weightKg).isEqualTo(15.0)
        // (3.5 − 1) × 70 kg × 6 min / 60.
        assertThat(loaded.kcal).isWithin(0.05).of(17.5)
        assertThat(bodyweight.weightKg).isNull()
        assertThat(activityDetail(loaded)).isEqualTo("3 组 × 12 次 · 15kg")
        assertThat(activityDetail(bodyweight)).isEqualTo("2 组 × 15 次 · 自重")
    }

    @Test
    fun activityKcalCountsTowardsBurnAndExport(): Unit = runBlocking {
        val energy = EnergyRepository(env.db, env.time)
        val before = energy.observeDay(flowOf(env.time.today())).first().burnInput!!.trainingKcal
        activity.addTimed(run, 30.0)
        activity.setSteps(8000)
        val after = energy.observeDay(flowOf(env.time.today())).first().burnInput!!.trainingKcal
        assertThat(after - before).isWithin(0.2).of(308.0 + 212.1)

        val day = Exporter(env.db, env.settings, env.time).build(ExportRange.All).days.single()
        assertThat(day.activities.map { it.kind }).containsExactly(ActivityKind.TIMED, ActivityKind.STEPS)
    }

    @Test
    fun activitiesSurviveBackupRoundTrip(): Unit = runBlocking {
        activity.addTimed(run, 45.0)
        activity.addSets(pushdown, sets = 4, reps = 10, weightKg = 20.0)
        activity.setSteps(6000)
        diet.addFood(rice, 200.0)
        val before = DbSnapshot.load(env.db)
        val json = Exporter(env.db, env.settings, env.time).export(ExportRange.All).json
        val restorer = BackupRestorer(env.db, env.settings)
        restorer.clearAll()
        restorer.restore((restorer.parse(json) as ValidationResult.Ok).value)
        val after = DbSnapshot.load(env.db)
        assertThat(after.activityLogs).containsExactlyElementsIn(before.activityLogs)
        assertThat(after.mealLogs).containsExactlyElementsIn(before.mealLogs)
    }
}
