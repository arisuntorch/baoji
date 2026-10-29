package com.baoji.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class CalorieEngineTest {

    private val shanghai = ZoneId.of("Asia/Shanghai")
    private val noon = ZonedDateTime.of(LocalDateTime.of(2026, 10, 11, 12, 0), shanghai)

    private fun input(sex: Sex = Sex.MALE, activity: Double = 120.0, training: Double = 0.0) =
        BurnInput(sex, weightKg = 75.0, heightCm = 175.0, age = 30, dailyActivityKcal = activity, trainingKcal = training)

    @Test
    fun bmrMaleMatchesMifflinStJeor() {
        assertThat(CalorieEngine.bmr(Sex.MALE, 75.0, 175.0, 30)).isWithin(1e-9).of(1698.75)
    }

    @Test
    fun bmrFemaleIs166LowerThanMale() {
        val m = CalorieEngine.bmr(Sex.MALE, 75.0, 175.0, 30)
        val f = CalorieEngine.bmr(Sex.FEMALE, 75.0, 175.0, 30)
        assertThat(m - f).isWithin(1e-9).of(166.0)
    }

    @Test
    fun planSampleNoonWithFourBenchSets() {
        val training = CalorieEngine.trainingKcal(List(4) { 8.0 })
        assertThat(CalorieEngine.burnedAt(input(training = training), noon)).isWithin(1e-6).of(1111.25)
    }

    @Test
    fun zeroDailyActivityOnlyDropsThatPart() {
        assertThat(CalorieEngine.burnedAt(input(activity = 0.0), noon)).isWithin(1e-6).of(1698.75 * 1.2 * 0.5)
    }

    @Test
    fun undoingASetSubtractsItsKcal() {
        val four = CalorieEngine.burnedAt(input(training = CalorieEngine.trainingKcal(List(4) { 8.0 })), noon)
        val three = CalorieEngine.burnedAt(input(training = CalorieEngine.trainingKcal(List(3) { 8.0 })), noon)
        assertThat(four - three).isWithin(1e-9).of(8.0)
    }

    @Test
    fun dayFractionBoundaries() {
        val midnight = noon.withHour(0)
        assertThat(CalorieEngine.dayFraction(midnight)).isEqualTo(0.0)
        val late = midnight.plusHours(23).plusMinutes(59).plusSeconds(59)
        assertThat(CalorieEngine.dayFraction(late)).isWithin(1e-4).of(1.0)
    }

    @Test
    fun dayFractionUsesRealLengthOnDstDay() {
        val ny = ZoneId.of("America/New_York")
        // 2026-03-08 is 23 hours long in New York; at 12:00 local, 11 real hours have passed.
        val dstNoon = ZonedDateTime.of(LocalDateTime.of(2026, 3, 8, 12, 0), ny)
        assertThat(CalorieEngine.dayFraction(dstNoon)).isWithin(1e-9).of(11.0 / 23.0)
    }

    @Test
    fun fullDayBurnIsUniformPlusTraining() {
        assertThat(CalorieEngine.burnedFullDay(input(training = 32.0))).isWithin(1e-9).of(1698.75 * 1.2 + 120 + 32)
    }

    @Test
    fun intakeSumsSnapshotsAndMacrosDefaultTo449() {
        assertThat(CalorieEngine.intake(listOf(100.0, 250.5))).isWithin(1e-9).of(350.5)
        assertThat(CalorieEngine.kcalFromMacros(12.0, 1.0, 10.0)).isWithin(1e-9).of(142.0)
    }

    @Test
    fun timedActivityCountsOnlyTheMetAboveRest() {
        // Running 9.8 MET for 30 min at 70 kg: (9.8 − 1) × 70 × 0.5 h.
        assertThat(CalorieEngine.activityKcal(met = 9.8, weightKg = 70.0, minutes = 30.0)).isWithin(1e-9).of(308.0)
    }

    @Test
    fun activityBelowRestingMetBurnsNothingExtra() {
        assertThat(CalorieEngine.activityKcal(met = 0.9, weightKg = 70.0, minutes = 60.0)).isEqualTo(0.0)
        assertThat(CalorieEngine.activityKcal(met = 5.0, weightKg = 70.0, minutes = -5.0)).isEqualTo(0.0)
    }

    @Test
    fun stepsUseBriskWalkingPace() {
        // 3.5 MET at 110 steps/min → 8000 steps at 70 kg ≈ 212 kcal.
        assertThat(CalorieEngine.stepsKcal(8000, 70.0)).isWithin(0.5).of(212.1)
        assertThat(CalorieEngine.stepsKcal(0, 70.0)).isEqualTo(0.0)
    }

    @Test
    fun strengthSetsCountTwoMinutesEach() {
        // 4 sets of a 5 MET move at 70 kg: (5 − 1) × 70 × 8 min / 60 ≈ 37.3 kcal.
        assertThat(CalorieEngine.setsKcal(met = 5.0, weightKg = 70.0, sets = 4)).isWithin(0.05).of(37.3)
        assertThat(CalorieEngine.setsKcal(met = 5.0, weightKg = 70.0, sets = 0)).isEqualTo(0.0)
    }
}
