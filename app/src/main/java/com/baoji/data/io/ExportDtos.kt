package com.baoji.data.io

import kotlinx.serialization.Serializable

/**
 * Export / backup file. Grouped by day so an external AI can read it directly, while
 * ids and timestamps are kept so a full export can be restored losslessly.
 */
@Serializable
data class BackupDto(
    val app: String = ImportValidator.APP_ID,
    val type: String = "backup",
    val version: Int = ImportValidator.SUPPORTED_VERSION,
    val exportedAt: String,
    val range: RangeDto,
    val profile: ProfileDto? = null,
    val targets: TargetsDto,
    val daily: List<ActivityDto> = emptyList(),
    val plans: List<PlanDto> = emptyList(),
    val meals: List<MealTaskDto> = emptyList(),
    val days: List<DayDto> = emptyList(),
    val exports: List<ExportRecordDto> = emptyList(),
)

@Serializable
data class RangeDto(val type: String, val from: String? = null, val to: String? = null)

@Serializable
data class ProfileDto(
    val heightCm: Double,
    val sex: String,
    val age: Int,
    val enteredAge: Int,
    val enteredYear: Int,
    val updatedAt: Long = 0,
)

@Serializable
data class TargetsDto(val protein: Double, val carbs: Double, val fat: Double)

@Serializable
data class ActivityDto(
    val id: String,
    val name: String,
    val kcal: Double,
    val archived: Boolean = false,
    val updatedAt: Long = 0,
)

@Serializable
data class PlanDto(
    val id: String,
    val name: String,
    val days: List<Int>,
    val order: Int = 0,
    val archived: Boolean = false,
    val updatedAt: Long = 0,
    val exercises: List<ExerciseDto> = emptyList(),
)

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val sets: Int,
    val reps: List<Int>,
    val weight: Double,
    val currentReps: Int,
    val kcalPerSet: Double,
    val order: Int = 0,
    val archived: Boolean = false,
    val updatedAt: Long = 0,
)

@Serializable
data class MealTaskDto(
    val id: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val kcal: Double,
    val order: Int = 0,
    val archived: Boolean = false,
    val updatedAt: Long = 0,
)

@Serializable
data class DayDto(
    val date: String,
    val weight: Double? = null,
    val diary: String? = null,
    val training: SessionDto? = null,
    val sets: List<SetDto> = emptyList(),
    val meals: List<MealLogDto> = emptyList(),
    val free: List<FreeDto> = emptyList(),
    val kcalIn: Double,
    val kcalOut: Double? = null,
    val ids: DayIdsDto? = null,
)

/** Row ids and update stamps for the per-day singletons, needed only for restore. */
@Serializable
data class DayIdsDto(
    val weightId: String? = null,
    val weightUpdatedAt: Long = 0,
    val diaryId: String? = null,
    val diaryUpdatedAt: Long = 0,
)

@Serializable
data class SessionDto(
    val id: String,
    val planId: String? = null,
    val start: String,
    val end: String? = null,
    val startTs: Long,
    val endTs: Long? = null,
    val updatedAt: Long = 0,
)

@Serializable
data class SetDto(
    val id: String,
    val exerciseId: String,
    val name: String,
    val time: String,
    val ts: Long,
    val weight: Double,
    val reps: Int,
    val kcal: Double,
    val updatedAt: Long = 0,
)

@Serializable
data class MealLogDto(
    val id: String,
    val mealTaskId: String,
    val name: String,
    val time: String,
    val ts: Long,
    val amount: Double,
    val unit: String,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val kcal: Double,
    val updatedAt: Long = 0,
)

@Serializable
data class FreeDto(
    val id: String,
    val type: String,
    val text: String,
    val time: String,
    val ts: Long,
    val updatedAt: Long = 0,
)

@Serializable
data class ExportRecordDto(
    val id: String,
    val ts: Long,
    val rangeType: String,
    val from: Long? = null,
    val to: Long? = null,
)
