package com.baoji.data.io

import com.baoji.data.db.DailyActivityEntity
import com.baoji.data.db.DiaryEntity
import com.baoji.data.db.ExerciseEntity
import com.baoji.data.db.ExportRecordEntity
import com.baoji.data.db.FreeLogEntity
import com.baoji.data.db.MealLogEntity
import com.baoji.data.db.MealTaskEntity
import com.baoji.data.db.ProfileEntity
import com.baoji.data.db.SetLogEntity
import com.baoji.data.db.TrainingPlanEntity
import com.baoji.data.db.TrainingSessionEntity
import com.baoji.data.db.WeightLogEntity
import java.time.LocalDate

/** Entity ⇄ DTO conversions. Kept separate so the file format can evolve without touching Room. */
object BackupMapper {

    fun toDto(e: DailyActivityEntity) = ActivityDto(e.id, e.name, e.kcal, e.archived, e.updatedAt)

    fun toDto(p: TrainingPlanEntity, exercises: List<ExerciseEntity>) = PlanDto(
        id = p.id, name = p.name, days = p.dayList(), order = p.sortOrder, archived = p.archived,
        updatedAt = p.updatedAt, exercises = exercises.sortedBy { it.sortOrder }.map(::toDto),
    )

    fun toDto(e: ExerciseEntity) = ExerciseDto(
        id = e.id, name = e.name, sets = e.sets, reps = listOf(e.repsMin, e.repsMax), weight = e.currentWeight,
        currentReps = e.currentReps, kcalPerSet = e.kcalPerSet, order = e.sortOrder, archived = e.archived,
        updatedAt = e.updatedAt,
    )

    fun toDto(e: MealTaskEntity) = MealTaskDto(
        e.id, e.name, e.amount, e.unit, e.protein, e.carbs, e.fat, e.kcal, e.sortOrder, e.archived, e.updatedAt,
    )

    fun toDto(e: SetLogEntity, clock: String) =
        SetDto(e.id, e.exerciseId, e.exerciseName, clock, e.timestamp, e.weight, e.reps, e.kcal, e.updatedAt)

    fun toDto(e: MealLogEntity, clock: String) = MealLogDto(
        e.id, e.mealTaskId, e.name, clock, e.timestamp, e.amount, e.unit, e.protein, e.carbs, e.fat, e.kcal, e.updatedAt,
    )

    fun toDto(e: FreeLogEntity, clock: String) = FreeDto(e.id, e.type, e.text, clock, e.timestamp, e.updatedAt)

    fun toDto(e: ExportRecordEntity) = ExportRecordDto(e.id, e.timestamp, e.rangeType, e.fromDate, e.toDate)

    // ---- DTO → Entity (restore) ----

    fun toEntity(d: ProfileDto) = ProfileEntity(
        heightCm = d.heightCm, sex = d.sex, enteredAge = d.enteredAge, enteredYear = d.enteredYear,
        avatarPath = null, updatedAt = d.updatedAt,
    )

    fun toEntity(d: ActivityDto) = DailyActivityEntity(d.id, d.name, d.kcal, d.archived, d.updatedAt)

    fun toEntity(d: PlanDto) = TrainingPlanEntity(d.id, d.name, d.days.joinToString(","), d.order, d.archived, d.updatedAt)

    fun toEntity(d: ExerciseDto, planId: String) = ExerciseEntity(
        id = d.id, planId = planId, name = d.name, sets = d.sets, repsMin = d.reps.first(), repsMax = d.reps.last(),
        kcalPerSet = d.kcalPerSet, currentWeight = d.weight, currentReps = d.currentReps, sortOrder = d.order,
        archived = d.archived, updatedAt = d.updatedAt,
    )

    fun toEntity(d: MealTaskDto) = MealTaskEntity(
        d.id, d.name, d.amount, d.unit, d.protein, d.carbs, d.fat, d.kcal, d.order, d.archived, d.updatedAt,
    )

    fun toEntity(d: SetDto, date: Long) =
        SetLogEntity(d.id, d.exerciseId, d.name, date, d.ts, d.weight, d.reps, d.kcal, d.updatedAt)

    fun toEntity(d: MealLogDto, date: Long) = MealLogEntity(
        d.id, d.mealTaskId, date, d.ts, d.name, d.amount, d.unit, d.protein, d.carbs, d.fat, d.kcal, d.updatedAt,
    )

    fun toEntity(d: FreeDto, date: Long) = FreeLogEntity(d.id, date, d.ts, d.type, d.text, d.updatedAt)

    fun toEntity(d: SessionDto, date: Long) = TrainingSessionEntity(d.id, date, d.planId, d.startTs, d.endTs, d.updatedAt)

    fun toEntity(d: ExportRecordDto) = ExportRecordEntity(d.id, d.ts, d.rangeType, d.from, d.to, d.ts)

    fun weightOf(day: DayDto): WeightLogEntity? = day.weight?.let { kg ->
        WeightLogEntity(day.ids?.weightId ?: "w-${day.date}", epochDay(day), kg, day.ids?.weightUpdatedAt ?: 0)
    }

    fun diaryOf(day: DayDto): DiaryEntity? = day.diary?.let { text ->
        DiaryEntity(day.ids?.diaryId ?: "d-${day.date}", epochDay(day), text, day.ids?.diaryUpdatedAt ?: 0)
    }

    fun epochDay(day: DayDto): Long = LocalDate.parse(day.date).toEpochDay()
}
