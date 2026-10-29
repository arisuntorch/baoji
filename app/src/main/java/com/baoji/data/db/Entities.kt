package com.baoji.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Single-row table; id is always [ProfileEntity.SINGLETON]. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String = SINGLETON,
    val heightCm: Double,
    val sex: String,
    val enteredAge: Int,
    val enteredYear: Int,
    val avatarPath: String?,
    val updatedAt: Long,
) {
    companion object {
        const val SINGLETON = "me"
    }
}

@Entity(tableName = "weight_log", indices = [Index(value = ["date"], unique = true)])
data class WeightLogEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val kg: Double,
    val updatedAt: Long,
)

@Entity(tableName = "diary", indices = [Index(value = ["date"], unique = true)])
data class DiaryEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val content: String,
    val updatedAt: Long,
)

/** [days] is a comma-separated list of ISO weekdays 1–7. */
@Entity(tableName = "training_plan")
data class TrainingPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val days: String,
    val sortOrder: Int,
    val archived: Boolean,
    val updatedAt: Long,
) {
    fun dayList(): List<Int> = days.split(',').mapNotNull { it.trim().toIntOrNull() }
}

@Entity(tableName = "exercise", indices = [Index("planId")])
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val name: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val kcalPerSet: Double,
    val currentWeight: Double,
    val currentReps: Int,
    val sortOrder: Int,
    val archived: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "set_log", indices = [Index("exerciseId"), Index("date")])
data class SetLogEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val date: Long,
    val timestamp: Long,
    val weight: Double,
    val reps: Int,
    val kcal: Double,
    val updatedAt: Long,
)

@Entity(tableName = "training_session", indices = [Index(value = ["date"], unique = true)])
data class TrainingSessionEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val planId: String?,
    val startedAt: Long,
    val finishedAt: Long?,
    val updatedAt: Long,
)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kcal: Double,
    val archived: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "meal_task")
data class MealTaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val kcal: Double,
    val sortOrder: Int,
    val archived: Boolean,
    val updatedAt: Long,
)

@Entity(
    tableName = "meal_log",
    indices = [Index("date"), Index(value = ["mealTaskId", "date"], unique = true)],
)
data class MealLogEntity(
    @PrimaryKey val id: String,
    val mealTaskId: String,
    val date: Long,
    val timestamp: Long,
    val name: String,
    val amount: Double,
    val unit: String,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val kcal: Double,
    val updatedAt: Long,
)

object ActivityKind {
    /** Picked from the built-in exercise list; [ActivityLogEntity.amount] is minutes. */
    const val TIMED = "timed"

    /** The day's step count; at most one row per day, id from [ActivityLogEntity.stepsId]. */
    const val STEPS = "steps"

    /** A strength move from the built-in list; [ActivityLogEntity.amount] is sets, with reps and weight. */
    const val SETS = "sets"
}

/**
 * Extra activity outside the plan: catalog workouts, strength sets and daily steps. [kcal] is a snapshot.
 * [reps] and [weightKg] are only set for [ActivityKind.SETS]; a null weight means bodyweight.
 */
@Entity(tableName = "activity_log", indices = [Index("date")])
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val timestamp: Long,
    val kind: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val kcal: Double,
    val updatedAt: Long,
    val reps: Int? = null,
    val weightKg: Double? = null,
) {
    companion object {
        fun stepsId(date: Long) = "steps-$date"
    }
}

object FreeLogType {
    const val EXTRA_TRAINING = "training"
    const val EXTRA_MEAL = "meal"
}

@Entity(tableName = "free_log", indices = [Index("date")])
data class FreeLogEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val timestamp: Long,
    val type: String,
    val text: String,
    val updatedAt: Long,
)

@Entity(tableName = "export_record")
data class ExportRecordEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val rangeType: String,
    val fromDate: Long?,
    val toDate: Long?,
    val updatedAt: Long,
)
