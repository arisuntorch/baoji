package com.baoji.data.repo

import com.baoji.core.time.TimeSource
import com.baoji.data.catalog.ExerciseItem
import com.baoji.data.db.ActivityKind
import com.baoji.data.db.ActivityLogEntity
import com.baoji.data.db.BaojiDatabase
import com.baoji.domain.CalorieEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A day's activity outside the plan. [steps] is null until a count is entered;
 * [workouts] holds timed activities and strength sets in the order they were logged.
 */
data class ActivityDay(val steps: ActivityLogEntity?, val workouts: List<ActivityLogEntity>)

@Singleton
class ActivityRepository @Inject constructor(
    private val db: BaojiDatabase,
    private val time: TimeSource,
) {
    private val logs get() = db.activityLogDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDay(days: Flow<LocalDate>): Flow<ActivityDay> = days.flatMapLatest { date ->
        logs.observe(date.toEpochDay()).map { rows ->
            ActivityDay(
                steps = rows.firstOrNull { it.kind == ActivityKind.STEPS },
                workouts = rows.filter { it.kind != ActivityKind.STEPS },
            )
        }
    }

    /** Latest body weight on or before each day; null before the first weigh-in. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeWeightKg(days: Flow<LocalDate>): Flow<Double?> = days.flatMapLatest { date ->
        db.weightDao().observeLatestOnOrBefore(date.toEpochDay()).map { it?.kg }
    }

    /** Logs [minutes] of a catalog activity; false when no body weight is known yet. */
    suspend fun addTimed(item: ExerciseItem, minutes: Double): Boolean {
        val today = time.todayEpochDay()
        val kg = weightKg(today) ?: return false
        val now = time.nowMillis()
        logs.upsert(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(), date = today, timestamp = now, kind = ActivityKind.TIMED,
                name = item.name, amount = minutes, unit = MINUTES,
                kcal = round1(CalorieEngine.activityKcal(item.met, kg, minutes)), updatedAt = now,
            ),
        )
        return true
    }

    /** Logs a strength move; a null [weightKg] means bodyweight. False when no body weight is known yet. */
    suspend fun addSets(item: ExerciseItem, sets: Int, reps: Int, weightKg: Double?): Boolean {
        val today = time.todayEpochDay()
        val kg = weightKg(today) ?: return false
        val now = time.nowMillis()
        val count = sets.coerceIn(1, MAX_SETS)
        logs.upsert(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(), date = today, timestamp = now, kind = ActivityKind.SETS,
                name = item.name, amount = count.toDouble(), unit = SETS_UNIT,
                kcal = round1(CalorieEngine.setsKcal(item.met, kg, count)), updatedAt = now,
                reps = reps.coerceIn(1, MAX_REPS), weightKg = weightKg?.takeIf { it > 0 }?.coerceAtMost(MAX_LIFT_KG),
            ),
        )
        return true
    }

    /** Sets today's step count; 0 clears it. False when no body weight is known yet. */
    suspend fun setSteps(steps: Int): Boolean {
        val today = time.todayEpochDay()
        val id = ActivityLogEntity.stepsId(today)
        if (steps <= 0) {
            logs.delete(id)
            return true
        }
        val kg = weightKg(today) ?: return false
        val now = time.nowMillis()
        val count = steps.coerceAtMost(MAX_STEPS)
        logs.upsert(
            ActivityLogEntity(
                id = id, date = today, timestamp = now, kind = ActivityKind.STEPS, name = STEPS_NAME,
                amount = count.toDouble(), unit = STEPS_UNIT, kcal = round1(CalorieEngine.stepsKcal(count, kg)),
                updatedAt = now,
            ),
        )
        return true
    }

    suspend fun delete(id: String): Boolean = logs.delete(id) > 0

    /** Puts back a deleted row exactly as it was. */
    suspend fun restore(log: ActivityLogEntity) = logs.upsert(log)

    private suspend fun weightKg(today: Long): Double? = db.weightDao().latestOnOrBefore(today)?.kg

    companion object {
        const val MINUTES = "分钟"
        const val STEPS_NAME = "每日步数"
        const val STEPS_UNIT = "步"
        const val MAX_STEPS = 200_000
        const val MAX_MINUTES = 1_440
        const val SETS_UNIT = "组"
        const val MAX_SETS = 20
        const val MAX_REPS = 100
        const val MAX_LIFT_KG = 500.0

        private fun round1(v: Double) = Math.round(v * 10) / 10.0
    }
}
