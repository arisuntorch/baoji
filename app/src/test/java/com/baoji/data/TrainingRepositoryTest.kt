package com.baoji.data

import com.baoji.data.io.ImportMode
import com.baoji.data.io.ParsedExercise
import com.baoji.data.io.ParsedPlan
import com.baoji.data.io.ParsedTraining
import com.baoji.data.io.PlanImporter
import com.baoji.data.repo.SetOutcome
import com.baoji.data.repo.TrainingRepository
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrainingRepositoryTest {

    // 2026-10-12 is a Monday (ISO 1).
    private val env = TestEnv(LocalDateTime.of(2026, 10, 12, 9, 0))
    private val repo = TrainingRepository(env.db, env.time)
    private lateinit var benchId: String
    private lateinit var rowId: String

    @Before
    fun setUp() = runBlocking {
        PlanImporter(env.db, env.settings, env.time).importTraining(
            ParsedTraining(
                emptyList(),
                listOf(
                    ParsedPlan(
                        "上肢 A", listOf(1, 4),
                        listOf(ParsedExercise("卧推", 2, 6, 8, 60.0, 8.0), ParsedExercise("划船", 1, 8, 10, 50.0, 6.0)),
                    ),
                ),
            ),
            ImportMode.REPLACE,
        )
        val ex = env.db.planDao().activeExercises()
        benchId = ex.first { it.name == "卧推" }.id
        rowId = ex.first { it.name == "划船" }.id
    }

    @After
    fun tearDown() = env.close()

    private suspend fun today() = repo.observeDay(flowOf(env.time.today())).first()

    @Test
    fun firstSetStartsSession() = runBlocking {
        assertThat(today().session).isNull()
        assertThat(repo.completeSet(benchId)).isEqualTo(SetOutcome.SET_DONE)
        assertThat(today().session).isNotNull()
    }

    @Test
    fun setSetsChangesTargetAndSyncsFinish() = runBlocking {
        repo.completeSet(benchId); repo.completeSet(benchId); repo.completeSet(rowId)
        assertThat(today().session!!.finishedAt).isNotNull()
        repo.setSets(benchId, 3)
        val reopened = today()
        assertThat(reopened.rows.first { it.exercise.id == benchId }.exercise.sets).isEqualTo(3)
        assertThat(reopened.session!!.finishedAt).isNull()
        assertThat(repo.completeSet(benchId)).isEqualTo(SetOutcome.DAY_DONE)
        repo.setSets(benchId, 0)
        assertThat(today().rows.first { it.exercise.id == benchId }.exercise.sets).isEqualTo(1)
    }

    @Test
    fun setOutcomesAndAutoFinish() = runBlocking {
        assertThat(repo.completeSet(benchId)).isEqualTo(SetOutcome.SET_DONE)
        assertThat(repo.completeSet(benchId)).isEqualTo(SetOutcome.EXERCISE_DONE)
        assertThat(repo.completeSet(benchId)).isEqualTo(SetOutcome.NOTHING)
        assertThat(repo.completeSet(rowId)).isEqualTo(SetOutcome.DAY_DONE)
        val day = today()
        assertThat(day.allComplete).isTrue()
        assertThat(day.session!!.finishedAt).isNotNull()
    }

    @Test
    fun undoRemovesLatestSetAndReopensDay() = runBlocking {
        repo.completeSet(benchId); repo.completeSet(benchId); repo.completeSet(rowId)
        assertThat(repo.undoSet(rowId)).isTrue()
        val day = today()
        assertThat(day.rows.first { it.exercise.id == rowId }.done).isEqualTo(0)
        assertThat(day.session!!.finishedAt).isNull()
        assertThat(repo.undoSet(rowId)).isFalse()
    }

    @Test
    fun adjustOnlyAffectsLaterSets() = runBlocking {
        repo.completeSet(benchId)
        repo.adjust(benchId, 62.5, 7)
        repo.completeSet(benchId)
        val sets = env.db.trainingLogDao().setsOn(env.time.todayEpochDay())
        assertThat(sets.map { it.weight }).containsExactly(60.0, 62.5).inOrder()
        assertThat(sets.map { it.reps }).containsExactly(6, 7).inOrder()
    }

    @Test
    fun overloadMarkAfterHittingTopOfRange() = runBlocking {
        repo.adjust(benchId, 60.0, 8)
        repo.completeSet(benchId); repo.completeSet(benchId)
        // Next Thursday (ISO 4) the same plan runs again.
        env.clock.set(LocalDateTime.of(2026, 10, 15, 9, 0))
        val row = today().rows.first { it.exercise.id == benchId }
        assertThat(row.overload).isTrue()
        assertThat(today().rows.first { it.exercise.id == rowId }.overload).isFalse()
    }

    @Test
    fun restDayHasNoRows() = runBlocking {
        env.clock.set(LocalDateTime.of(2026, 10, 13, 9, 0)) // Tuesday
        val day = today()
        assertThat(day.isTrainingDay).isFalse()
        assertThat(day.rows).isEmpty()
    }

    @Test
    fun historyKeepsMaxPerDay() = runBlocking {
        repo.completeSet(benchId)
        repo.adjust(benchId, 65.0, 6)
        repo.completeSet(benchId)
        val history = repo.observeHistory("卧推").first()
        assertThat(history.single().weight).isEqualTo(65.0)
    }
}
