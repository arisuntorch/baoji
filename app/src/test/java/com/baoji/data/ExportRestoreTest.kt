package com.baoji.data

import com.baoji.data.db.FreeLogType
import com.baoji.data.io.BackupRestorer
import com.baoji.data.io.DbSnapshot
import com.baoji.data.io.ExportRange
import com.baoji.data.io.Exporter
import com.baoji.data.io.ImportMode
import com.baoji.data.io.ImportValidator
import com.baoji.data.io.PlanImporter
import com.baoji.data.io.ValidationResult
import com.baoji.data.repo.DietRepository
import com.baoji.data.repo.JournalRepository
import com.baoji.data.repo.ProfileInput
import com.baoji.data.repo.ProfileRepository
import com.baoji.data.repo.TrainingRepository
import com.baoji.domain.Sex
import com.baoji.testing.TestEnv
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExportRestoreTest {

    private val env = TestEnv(LocalDateTime.of(2026, 10, 12, 12, 0))
    private val exporter = Exporter(env.db, env.settings, env.time)
    private val restorer = BackupRestorer(env.db, env.settings)
    private val training = TrainingRepository(env.db, env.time)
    private val diet = DietRepository(env.db, env.settings, env.time)
    private val journal = JournalRepository(env.db, env.time)

    private val trainingJson = """
        { "app": "baoji", "type": "training", "version": 1,
          "daily": [ { "name": "步行去教室", "kcal": 120 } ],
          "plans": [ { "name": "上肢 A", "days": [1, 4], "exercises": [
              { "name": "卧推", "sets": 4, "reps": [6, 8], "weight": 60, "kcalPerSet": 8 } ] } ] }
    """.trimIndent()
    private val dietJson = """
        { "app": "baoji", "type": "diet", "version": 1,
          "targets": { "protein": 170, "carbs": 150, "fat": 50 },
          "meals": [ { "name": "练前鸡蛋", "amount": 2, "unit": "个", "protein": 12, "carbs": 1, "fat": 10 } ] }
    """.trimIndent()

    @Before
    fun seed() = runBlocking {
        ProfileRepository(env.db, env.time).create(ProfileInput(175.0, 75.0, Sex.MALE, 30))
        val importer = PlanImporter(env.db, env.settings, env.time)
        importer.importTraining((ImportValidator.validateTraining(trainingJson) as ValidationResult.Ok).value, ImportMode.REPLACE)
        importer.importDiet((ImportValidator.validateDiet(dietJson) as ValidationResult.Ok).value, ImportMode.REPLACE)
        val bench = env.db.planDao().activeExercises().single().id
        repeat(4) { training.completeSet(bench) }
        diet.toggle(env.db.mealDao().activeTasks().single().id)
        journal.saveDiary(env.time.todayEpochDay(), "状态不错")
        journal.addFreeLog(FreeLogType.EXTRA_TRAINING, "跑步 30 分钟")
        journal.addFreeLog(FreeLogType.EXTRA_MEAL, "711 茶叶蛋一个")
    }

    @After
    fun tearDown() = env.close()

    @Test
    fun exportGroupsByDayWithPlanSampleNumbers() = runBlocking {
        val dto = exporter.build(ExportRange.All)
        val day = dto.days.single()
        assertThat(day.date).isEqualTo("2026-10-12")
        assertThat(day.weight).isEqualTo(75.0)
        assertThat(day.diary).isEqualTo("状态不错")
        assertThat(day.sets).hasSize(4)
        assertThat(day.free.map { it.text }).containsExactly("跑步 30 分钟", "711 茶叶蛋一个")
        assertThat(day.kcalIn).isEqualTo(142.0)
        // Plan §11 sample: noon, 4 bench sets → 1111.25, rounded to one decimal.
        assertThat(day.kcalOut).isEqualTo(1111.3)
    }

    @Test
    fun exportClearRestoreRoundTrip() = runBlocking {
        val before = DbSnapshot.load(env.db)
        val json = exporter.export(ExportRange.All).json

        restorer.clearAll()
        assertThat(DbSnapshot.load(env.db).sets).isEmpty()

        val parsed = restorer.parse(json)
        assertThat(parsed).isInstanceOf(ValidationResult.Ok::class.java)
        restorer.restore((parsed as ValidationResult.Ok).value)

        val after = DbSnapshot.load(env.db)
        assertThat(after.sets).containsExactlyElementsIn(before.sets)
        assertThat(after.mealLogs).containsExactlyElementsIn(before.mealLogs)
        assertThat(after.freeLogs).containsExactlyElementsIn(before.freeLogs)
        assertThat(after.sessions).containsExactlyElementsIn(before.sessions)
        assertThat(after.diaries).containsExactlyElementsIn(before.diaries)
        assertThat(after.weights).containsExactlyElementsIn(before.weights)
        assertThat(after.plans).containsExactlyElementsIn(before.plans)
        assertThat(after.exercises).containsExactlyElementsIn(before.exercises)
        assertThat(after.mealTasks).containsExactlyElementsIn(before.mealTasks)
        assertThat(after.activities).containsExactlyElementsIn(before.activities)
        assertThat(after.profile!!.copy(avatarPath = null)).isEqualTo(before.profile!!.copy(avatarPath = null))
    }

    @Test
    fun partialExportCannotRestore() = runBlocking {
        val json = exporter.export(ExportRange.Dates(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12))).json
        assertThat(restorer.parse(json)).isInstanceOf(ValidationResult.Invalid::class.java)
    }

    @Test
    fun failedRestoreKeepsOldData() = runBlocking {
        val good = (restorer.parse(exporter.export(ExportRange.All).json) as ValidationResult.Ok).value
        val dup = good.days.single().sets.first()
        val broken = good.copy(days = listOf(good.days.single().let { it.copy(sets = it.sets + dup) }))
        val result = runCatching { restorer.restore(broken) }
        assertThat(result.isFailure).isTrue()
        assertThat(DbSnapshot.load(env.db).sets).hasSize(4)
    }

    @Test
    fun sinceLastExportOnlyIncludesTouchedDays() = runBlocking {
        exporter.record(ExportRange.All)
        env.clock.set(LocalDateTime.of(2026, 10, 13, 8, 0))
        assertThat(exporter.build(ExportRange.SinceLast).days).isEmpty()
        journal.saveWeight(74.6)
        val days = exporter.build(ExportRange.SinceLast).days
        assertThat(days.map { it.date }).containsExactly("2026-10-13")
        assertThat(days.single().weight).isEqualTo(74.6)
    }

    @Test
    fun dateRangeFilters() = runBlocking {
        assertThat(exporter.build(ExportRange.Dates(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 11))).days).isEmpty()
        assertThat(exporter.build(ExportRange.Dates(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 20))).days).hasSize(1)
    }
}
