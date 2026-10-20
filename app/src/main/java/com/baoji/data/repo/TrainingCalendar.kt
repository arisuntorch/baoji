package com.baoji.data.repo

import com.baoji.data.db.ExerciseEntity
import com.baoji.data.db.SetLogEntity
import com.baoji.data.db.TrainingPlanEntity
import java.time.LocalDate
import java.time.YearMonth

/** Small mark under a calendar day. */
enum class DayMark {
    /** Sets were logged that day. */
    LOGGED,

    /** Today or later, and a plan falls on that weekday. */
    PLANNED,
}

enum class DayTense { PAST, TODAY, FUTURE }

/** What one exercise looked like on a past day. */
data class LoggedExercise(val name: String, val sets: List<SetLogEntity>)

/** Read-only look at another day, shown under the calendar. */
data class DayPreview(
    val date: LocalDate,
    val tense: DayTense,
    val planNames: List<String>,
    /** Exercises the current plans schedule on this weekday. */
    val planned: List<ExerciseEntity>,
    /** What was actually done, in the order it was done. Empty for today-without-sets and the future. */
    val logged: List<LoggedExercise>,
)

object TrainingCalendar {

    /**
     * Past days are marked only when something was logged, so a skipped day looks like any other
     * day rather than a miss. Today and later show the plan.
     */
    fun marks(
        month: YearMonth,
        today: LocalDate,
        activePlans: List<TrainingPlanEntity>,
        loggedDays: Set<Long>,
    ): Map<LocalDate, DayMark> {
        val plannedWeekdays = activePlans.flatMap { it.dayList() }.toSet()
        return (1..month.lengthOfMonth()).mapNotNull { d ->
            val date = month.atDay(d)
            val mark = when {
                date.toEpochDay() in loggedDays -> DayMark.LOGGED
                !date.isBefore(today) && date.dayOfWeek.value in plannedWeekdays -> DayMark.PLANNED
                else -> null
            }
            mark?.let { date to it }
        }.toMap()
    }

    fun preview(
        date: LocalDate,
        today: LocalDate,
        activePlans: List<TrainingPlanEntity>,
        activeExercises: List<ExerciseEntity>,
        sets: List<SetLogEntity>,
    ): DayPreview {
        val plans = TrainingDayBuilder.plansFor(date, activePlans)
        val order = plans.withIndex().associate { it.value.id to it.index }
        val planned = activeExercises
            .filter { it.planId in order }
            .sortedWith(compareBy({ order[it.planId] }, { it.sortOrder }))
        val logged = sets.sortedBy { it.timestamp }
            .groupBy { it.exerciseName }
            .map { (name, s) -> LoggedExercise(name, s) }
        val tense = when {
            date.isBefore(today) -> DayTense.PAST
            date == today -> DayTense.TODAY
            else -> DayTense.FUTURE
        }
        return DayPreview(date, tense, plans.map { it.name }, planned, logged)
    }
}
