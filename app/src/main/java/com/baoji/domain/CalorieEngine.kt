package com.baoji.domain

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Everything needed to compute a day's burn. */
data class BurnInput(
    val sex: Sex,
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val dailyActivityKcal: Double,
    val trainingKcal: Double,
)

/**
 * Pure calorie math. A day is the natural day 00:00–24:00 in the given zone;
 * DST days have 23 or 25 hours and are handled by measuring real seconds.
 */
object CalorieEngine {

    private const val ACTIVITY_FACTOR = 1.2

    /** Mifflin-St Jeor. */
    fun bmr(sex: Sex, weightKg: Double, heightCm: Double, age: Int): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return when (sex) {
            Sex.MALE -> base + 5.0
            Sex.FEMALE -> base - 161.0
        }
    }

    fun dailyUniform(input: BurnInput): Double =
        bmr(input.sex, input.weightKg, input.heightCm, input.age) * ACTIVITY_FACTOR + input.dailyActivityKcal

    /** Fraction of [now]'s calendar day that has elapsed, in [0, 1]. */
    fun dayFraction(now: ZonedDateTime): Double {
        val zone: ZoneId = now.zone
        val date: LocalDate = now.toLocalDate()
        val start = date.atStartOfDay(zone)
        val end = date.plusDays(1).atStartOfDay(zone)
        val total = Duration.between(start, end).toMillis().toDouble()
        val elapsed = Duration.between(start, now).toMillis().toDouble()
        return (elapsed / total).coerceIn(0.0, 1.0)
    }

    /** Total burn so far today: time-proportional uniform part plus training. */
    fun burnedAt(input: BurnInput, now: ZonedDateTime): Double =
        dailyUniform(input) * dayFraction(now) + input.trainingKcal

    /** Total burn for a finished day. */
    fun burnedFullDay(input: BurnInput): Double = dailyUniform(input) + input.trainingKcal

    /** Training kcal from completed sets: each entry is that set's kcal snapshot. */
    fun trainingKcal(setKcals: List<Double>): Double = setKcals.sum()

    fun intake(mealKcals: List<Double>): Double = mealKcals.sum()

    /**
     * Extra kcal of a timed activity. The resting 1 MET is already inside the BMR part of the day,
     * so only (MET − 1) is added: kcal = (MET − 1) × kg × hours.
     */
    fun activityKcal(met: Double, weightKg: Double, minutes: Double): Double =
        if (minutes <= 0) 0.0 else (met - 1.0).coerceAtLeast(0.0) * weightKg * minutes / 60.0

    /** Steps counted as walking at [STEP_MET] and [STEPS_PER_MINUTE]. */
    fun stepsKcal(steps: Int, weightKg: Double): Double =
        activityKcal(STEP_MET, weightKg, steps.coerceAtLeast(0) / STEPS_PER_MINUTE)

    private const val STEP_MET = 3.5
    private const val STEPS_PER_MINUTE = 110.0

    /** Default kcal when a meal omits it: 4/4/9. */
    fun kcalFromMacros(protein: Double, carbs: Double, fat: Double): Double =
        4.0 * protein + 4.0 * carbs + 9.0 * fat
}
