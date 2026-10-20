package com.baoji.data

import com.baoji.data.db.ExerciseEntity
import com.baoji.data.db.SetLogEntity
import com.baoji.data.db.TrainingPlanEntity
import com.baoji.data.repo.DayMark
import com.baoji.data.repo.DayTense
import com.baoji.data.repo.TrainingCalendar
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TrainingCalendarTest {

    // 2026-10-20 is a Tuesday.
    private val today = LocalDate.of(2026, 10, 20)
    private val upper = TrainingPlanEntity("p1", "上肢", "1,4", 0, false, 0) // Mon, Thu
    private val lower = TrainingPlanEntity("p2", "下肢", "3", 1, false, 0) // Wed
    private val bench = ExerciseEntity("e1", "p1", "卧推", 4, 6, 8, 8.0, 60.0, 8, 0, false, 0)
    private val row = ExerciseEntity("e2", "p1", "划船", 3, 8, 10, 7.0, 50.0, 10, 1, false, 0)
    private val squat = ExerciseEntity("e3", "p2", "深蹲", 4, 5, 6, 10.0, 80.0, 5, 0, false, 0)

    private fun set(name: String, date: LocalDate, ts: Long, weight: Double = 60.0, reps: Int = 8) =
        SetLogEntity("s$ts", "x", name, date.toEpochDay(), ts, weight, reps, 8.0, ts)

    @Test
    fun futurePlannedDaysAreMarked() {
        val marks = TrainingCalendar.marks(YearMonth.of(2026, 10), today, listOf(upper, lower), emptySet())
        assertThat(marks[LocalDate.of(2026, 10, 21)]).isEqualTo(DayMark.PLANNED) // Wed
        assertThat(marks[LocalDate.of(2026, 10, 22)]).isEqualTo(DayMark.PLANNED) // Thu
        assertThat(marks[LocalDate.of(2026, 10, 24)]).isNull() // Sat, rest
    }

    @Test
    fun skippedPastDaysLookLikeAnyOtherDay() {
        // Mon 10/19 was planned but nothing was logged: no mark, no "missed" signal.
        val marks = TrainingCalendar.marks(YearMonth.of(2026, 10), today, listOf(upper), emptySet())
        assertThat(marks[LocalDate.of(2026, 10, 19)]).isNull()
    }

    @Test
    fun loggedDaysAreMarkedEvenOffPlan() {
        val sat = LocalDate.of(2026, 10, 17)
        val marks = TrainingCalendar.marks(YearMonth.of(2026, 10), today, listOf(upper), setOf(sat.toEpochDay()))
        assertThat(marks[sat]).isEqualTo(DayMark.LOGGED)
    }

    @Test
    fun futurePreviewShowsThatWeekdaysPlanInOrder() {
        val thu = LocalDate.of(2026, 10, 22)
        val p = TrainingCalendar.preview(thu, today, listOf(upper, lower), listOf(row, squat, bench), emptyList())
        assertThat(p.tense).isEqualTo(DayTense.FUTURE)
        assertThat(p.planNames).containsExactly("上肢")
        assertThat(p.planned.map { it.name }).containsExactly("卧推", "划船").inOrder()
        assertThat(p.logged).isEmpty()
    }

    @Test
    fun pastPreviewGroupsLoggedSetsByExerciseInOrderDone() {
        val mon = LocalDate.of(2026, 10, 19)
        val sets = listOf(set("划船", mon, 3), set("卧推", mon, 1), set("卧推", mon, 2, weight = 62.5, reps = 6))
        val p = TrainingCalendar.preview(mon, today, listOf(upper), listOf(bench, row), sets)
        assertThat(p.tense).isEqualTo(DayTense.PAST)
        assertThat(p.logged.map { it.name }).containsExactly("卧推", "划船").inOrder()
        assertThat(p.logged.first().sets.map { it.weight }).containsExactly(60.0, 62.5).inOrder()
    }

    @Test
    fun restDayHasNoPlan() {
        val sat = LocalDate.of(2026, 10, 24)
        val p = TrainingCalendar.preview(sat, today, listOf(upper, lower), listOf(bench, squat), emptyList())
        assertThat(p.planned).isEmpty()
        assertThat(p.planNames).isEmpty()
    }
}
