package com.baoji.data.repo

import com.baoji.data.db.ExerciseEntity
import com.baoji.data.db.SetLogEntity
import com.baoji.data.db.TrainingPlanEntity
import com.baoji.data.db.TrainingSessionEntity
import com.baoji.domain.OverloadRule
import java.time.LocalDate

data class ExerciseRow(
    val exercise: ExerciseEntity,
    val done: Int,
    val overload: Boolean,
) {
    val complete: Boolean get() = done >= exercise.sets
}

data class TrainingDay(
    val date: LocalDate,
    val isTrainingDay: Boolean,
    val plans: List<TrainingPlanEntity>,
    val rows: List<ExerciseRow>,
    val session: TrainingSessionEntity?,
) {
    val allComplete: Boolean get() = rows.isNotEmpty() && rows.all { it.complete }
}

object TrainingDayBuilder {

    fun plansFor(date: LocalDate, active: List<TrainingPlanEntity>): List<TrainingPlanEntity> =
        active.filter { date.dayOfWeek.value in it.dayList() }

    fun build(
        date: LocalDate,
        activePlans: List<TrainingPlanEntity>,
        activeExercises: List<ExerciseEntity>,
        todaySets: List<SetLogEntity>,
        lastSessionSets: List<SetLogEntity>,
        session: TrainingSessionEntity?,
    ): TrainingDay {
        val plans = plansFor(date, activePlans)
        val planOrder = plans.withIndex().associate { it.value.id to it.index }
        val exercises = activeExercises
            .filter { it.planId in planOrder }
            .sortedWith(compareBy({ planOrder[it.planId] }, { it.sortOrder }))
        val doneById = todaySets.groupingBy { it.exerciseId }.eachCount()
        val lastByName = lastSessionSets.groupBy { it.exerciseName }
        val rows = exercises.map { e ->
            val last = lastByName[e.name].orEmpty().map { it.reps }
            ExerciseRow(e, doneById[e.id] ?: 0, OverloadRule.shouldMark(last, e.sets, e.repsMax))
        }
        val isTrainingDay = plans.isNotEmpty() && rows.isNotEmpty()
        return TrainingDay(
            date = date,
            isTrainingDay = isTrainingDay,
            plans = plans,
            rows = rows,
            session = session,
        )
    }
}
