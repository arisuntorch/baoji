package com.baoji.data.repo

import androidx.room.withTransaction
import com.baoji.core.time.TimeSource
import com.baoji.data.db.BaojiDatabase
import com.baoji.data.db.SetLogEntity
import com.baoji.data.db.TrainingSessionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class SetOutcome { SET_DONE, EXERCISE_DONE, DAY_DONE, NOTHING }

/** One point in the mini chart: the heaviest set of a day. */
data class DayMax(val date: LocalDate, val weight: Double)

@Singleton
class TrainingRepository @Inject constructor(
    private val db: BaojiDatabase,
    private val time: TimeSource,
) {
    private val plans get() = db.planDao()
    private val logs get() = db.trainingLogDao()

    /** Re-subscribes when [days] emits a new date (midnight rollover). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDay(days: Flow<LocalDate>): Flow<TrainingDay> = days.flatMapLatest { date ->
        val epoch = date.toEpochDay()
        combine(
            plans.observeActivePlans(),
            plans.observeActiveExercises(),
            logs.observeSets(epoch),
            logs.observeLastSessionSets(epoch),
            logs.observeSession(epoch),
        ) { activePlans, exercises, today, last, session ->
            TrainingDayBuilder.build(date, activePlans, exercises, today, last, session)
        }
    }

    /** Calendar marks for one month; see [TrainingCalendar.marks]. */
    fun observeMonthMarks(month: YearMonth): Flow<Map<LocalDate, DayMark>> = combine(
        plans.observeActivePlans(),
        logs.observeLoggedDates(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()),
    ) { activePlans, logged ->
        TrainingCalendar.marks(month, time.today(), activePlans, logged.toSet())
    }

    /** Read-only view of any day for the calendar. */
    fun observePreview(date: LocalDate): Flow<DayPreview> = combine(
        plans.observeActivePlans(),
        plans.observeActiveExercises(),
        logs.observeSets(date.toEpochDay()),
    ) { activePlans, exercises, sets ->
        TrainingCalendar.preview(date, time.today(), activePlans, exercises, sets)
    }

    fun observeHistory(exerciseName: String, limit: Int = 8): Flow<List<DayMax>> =
        logs.observeSetsByName(exerciseName).map { sets ->
            sets.groupBy { it.date }
                .map { (d, s) -> DayMax(LocalDate.ofEpochDay(d), s.maxOf { it.weight }) }
                .sortedBy { it.date }
                .takeLast(limit)
        }

    private suspend fun startSession(today: Long): TrainingSessionEntity {
        val now = time.nowMillis()
        val date = LocalDate.ofEpochDay(today)
        val planId = TrainingDayBuilder.plansFor(date, plans.activePlans()).firstOrNull()?.id
        val s = TrainingSessionEntity(UUID.randomUUID().toString(), today, planId, now, null, now)
        logs.upsertSession(s)
        return s
    }

    /** Records one set at the exercise's current weight × reps; starts today's session if needed. */
    suspend fun completeSet(exerciseId: String): SetOutcome = db.withTransaction {
        val today = time.todayEpochDay()
        val now = time.nowMillis()
        val exercise = plans.exercise(exerciseId) ?: return@withTransaction SetOutcome.NOTHING
        val doneBefore = logs.setsOn(today).count { it.exerciseId == exerciseId }
        if (doneBefore >= exercise.sets) return@withTransaction SetOutcome.NOTHING
        val session = logs.session(today) ?: startSession(today)
        logs.upsertSet(
            SetLogEntity(
                id = UUID.randomUUID().toString(),
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                date = today,
                timestamp = now,
                weight = exercise.currentWeight,
                reps = exercise.currentReps,
                kcal = exercise.kcalPerSet,
                updatedAt = now,
            ),
        )
        val day = currentDay(today)
        when {
            day.allComplete -> {
                logs.upsertSession(session.copy(finishedAt = now, updatedAt = now))
                SetOutcome.DAY_DONE
            }
            doneBefore + 1 >= exercise.sets -> SetOutcome.EXERCISE_DONE
            else -> SetOutcome.SET_DONE
        }
    }

    /** Removes the latest set of this exercise today. Reopens the session if it was finished. */
    suspend fun undoSet(exerciseId: String): Boolean = db.withTransaction {
        val today = time.todayEpochDay()
        val last = logs.lastSet(exerciseId, today) ?: return@withTransaction false
        logs.deleteSet(last.id)
        logs.session(today)?.takeIf { it.finishedAt != null }?.let {
            logs.upsertSession(it.copy(finishedAt = null, updatedAt = time.nowMillis()))
        }
        true
    }

    /** Changes weight/reps for upcoming sets only; logged sets keep their snapshot. */
    suspend fun adjust(exerciseId: String, weight: Double, reps: Int) {
        val e = plans.exercise(exerciseId) ?: return
        plans.upsertExercise(
            e.copy(currentWeight = weight.coerceAtLeast(0.0), currentReps = reps.coerceAtLeast(1), updatedAt = time.nowMillis()),
        )
    }

    /** Changes the planned set count; finishes or reopens today's session to match. */
    suspend fun setSets(exerciseId: String, sets: Int) = db.withTransaction {
        val e = plans.exercise(exerciseId) ?: return@withTransaction
        val now = time.nowMillis()
        plans.upsertExercise(e.copy(sets = sets.coerceIn(1, MAX_SETS), updatedAt = now))
        val today = time.todayEpochDay()
        val session = logs.session(today) ?: return@withTransaction
        val complete = currentDay(today).allComplete
        if (complete != (session.finishedAt != null)) {
            logs.upsertSession(session.copy(finishedAt = if (complete) now else null, updatedAt = now))
        }
    }

    private suspend fun currentDay(today: Long): TrainingDay = TrainingDayBuilder.build(
        LocalDate.ofEpochDay(today),
        plans.activePlans(),
        plans.activeExercises(),
        logs.setsOn(today),
        emptyList(),
        logs.session(today),
    )

    companion object {
        const val MAX_SETS = 20
    }
}
