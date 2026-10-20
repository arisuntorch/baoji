package com.baoji.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class AgeCalculatorTest {
    @Test
    fun sameYearKeepsEnteredAge() {
        assertThat(AgeCalculator.currentAge(30, 2026, LocalDate.of(2026, 12, 31))).isEqualTo(30)
    }

    @Test
    fun janFirstNextYearAddsOne() {
        assertThat(AgeCalculator.currentAge(30, 2026, LocalDate.of(2027, 1, 1))).isEqualTo(31)
    }

    @Test
    fun severalYearsLater() {
        assertThat(AgeCalculator.currentAge(30, 2026, LocalDate.of(2030, 6, 1))).isEqualTo(34)
    }

    @Test
    fun clockBeforeEnteredYearNeverLowersAge() {
        assertThat(AgeCalculator.currentAge(30, 2026, LocalDate.of(2025, 6, 1))).isEqualTo(30)
    }
}

class OverloadRuleTest {
    @Test
    fun allSetsAtTopOfRangeMarks() {
        assertThat(OverloadRule.shouldMark(listOf(8, 8, 8, 8), plannedSets = 4, repsMax = 8)).isTrue()
    }

    @Test
    fun oneSetShortDoesNotMark() {
        assertThat(OverloadRule.shouldMark(listOf(8, 8, 7, 8), 4, 8)).isFalse()
    }

    @Test
    fun fewerSetsThanPlannedDoesNotMark() {
        assertThat(OverloadRule.shouldMark(listOf(8, 8, 8), 4, 8)).isFalse()
    }

    @Test
    fun noHistoryDoesNotMark() {
        assertThat(OverloadRule.shouldMark(emptyList(), 4, 8)).isFalse()
    }

    @Test
    fun extraSetsAboveMaxStillMark() {
        assertThat(OverloadRule.shouldMark(listOf(9, 8, 8, 8, 8), 4, 8)).isTrue()
    }
}

class WeightCurveTest {
    private fun pts(vararg ys: Double) = ys.mapIndexed { i, y -> CurvePoint(i.toDouble(), y) }

    @Test
    fun emaStartsAtFirstValueAndLags() {
        val out = WeightCurve.ema(pts(60.0, 62.0), alpha = 0.5)
        assertThat(out[0].y).isEqualTo(60.0)
        assertThat(out[1].y).isWithin(1e-9).of(61.0)
    }

    @Test
    fun emaOfEmptyIsEmpty() {
        assertThat(WeightCurve.ema(emptyList())).isEmpty()
    }

    @Test
    fun splinePassesThroughKnots() {
        val knots = pts(60.0, 61.0, 61.5, 61.2)
        val out = WeightCurve.monotoneSpline(knots, samplesPerSegment = 4)
        knots.forEach { k -> assertThat(out.first { it.x == k.x }.y).isWithin(1e-9).of(k.y) }
    }

    @Test
    fun splineNeverOvershootsMonotoneData() {
        val out = WeightCurve.monotoneSpline(pts(60.0, 60.0, 62.0, 62.1, 65.0), samplesPerSegment = 20)
        out.zipWithNext().forEach { (a, b) -> assertThat(b.y).isAtLeast(a.y - 1e-9) }
        assertThat(out.maxOf { it.y }).isAtMost(65.0 + 1e-9)
    }

    @Test
    fun fewerThanThreeKnotsReturnedAsIs() {
        val two = pts(60.0, 61.0)
        assertThat(WeightCurve.monotoneSpline(two)).isEqualTo(two)
    }
}
