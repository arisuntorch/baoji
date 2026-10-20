package com.baoji.data

import com.baoji.data.io.ImportMode
import com.baoji.data.io.ParsedActivity
import com.baoji.data.io.ParsedDiet
import com.baoji.data.io.ParsedExercise
import com.baoji.data.io.ParsedMeal
import com.baoji.data.io.ParsedPlan
import com.baoji.data.io.ParsedTargets
import com.baoji.data.io.ParsedTraining
import com.baoji.data.io.PlanImporter
import com.baoji.data.prefs.NutritionTargets
import com.baoji.data.repo.TrainingRepository
import com.baoji.testing.TestEnv
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlanImporterTest {

    private val env = TestEnv()
    private val importer = PlanImporter(env.db, env.settings, env.time)

    @After
    fun tearDown() = env.close()

    private fun training(vararg plans: ParsedPlan, daily: List<ParsedActivity> = emptyList()) =
        ParsedTraining(daily, plans.toList())

    private val bench = ParsedExercise("卧推", 4, 6, 8, 60.0, 8.0)
    private val pulldown = ParsedExercise("高位下拉", 4, 8, 10, 50.0, 7.0)

    @Test
    fun mergeUpdatesSameNameAndAddsNew() = runBlocking {
        importer.importTraining(training(ParsedPlan("上肢 A", listOf(1, 4), listOf(bench)), daily = listOf(ParsedActivity("步行", 120.0))), ImportMode.MERGE)
        val firstId = env.db.planDao().activeExercises().single().id

        importer.importTraining(
            training(ParsedPlan("上肢 A", listOf(1, 4), listOf(bench.copy(weight = 62.5), pulldown)), daily = listOf(ParsedActivity("步行", 150.0))),
            ImportMode.MERGE,
        )
        val exercises = env.db.planDao().activeExercises()
        assertThat(exercises.map { it.name }).containsExactly("卧推", "高位下拉")
        val updated = exercises.first { it.name == "卧推" }
        assertThat(updated.id).isEqualTo(firstId)
        assertThat(updated.currentWeight).isEqualTo(62.5)
        assertThat(env.db.planDao().activePlans()).hasSize(1)
        assertThat(env.db.planDao().activeActivities().single().kcal).isEqualTo(150.0)
    }

    @Test
    fun replaceArchivesOldTemplatesButHistoryStillResolves() = runBlocking {
        importer.importTraining(training(ParsedPlan("上肢 A", listOf(1), listOf(bench))), ImportMode.REPLACE)
        // 2026-10-12 is a Monday.
        val repo = TrainingRepository(env.db, env.time)
        val oldId = env.db.planDao().activeExercises().single().id
        repo.completeSet(oldId)

        importer.importTraining(training(ParsedPlan("下肢", listOf(2), listOf(ParsedExercise("深蹲", 3, 5, 5, 80.0, 10.0)))), ImportMode.REPLACE)

        assertThat(env.db.planDao().activeExercises().map { it.name }).containsExactly("深蹲")
        val archived = env.db.planDao().exercise(oldId)
        assertThat(archived!!.archived).isTrue()
        val set = env.db.trainingLogDao().allSets().single()
        assertThat(set.exerciseId).isEqualTo(oldId)
        assertThat(set.exerciseName).isEqualTo("卧推")
    }

    @Test
    fun dietImportStoresTargetsAndTasks() = runBlocking {
        importer.importDiet(
            ParsedDiet(ParsedTargets(170.0, 150.0, 50.0), listOf(ParsedMeal("奶昔", 1.0, "份", 79.0, 48.0, 7.0, 571.0))),
            ImportMode.REPLACE,
        )
        assertThat(env.settings.targets.first()).isEqualTo(NutritionTargets(170.0, 150.0, 50.0))
        assertThat(env.db.mealDao().activeTasks().single().kcal).isEqualTo(571.0)
    }
}
