package com.baoji.data.repo

import com.baoji.core.time.TimeSource
import com.baoji.data.db.BaojiDatabase
import com.baoji.domain.AgeCalculator
import com.baoji.domain.BurnInput
import com.baoji.domain.CalorieEngine
import com.baoji.domain.Sex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the home screen needs except the clock tick. */
data class EnergyDay(
    val date: LocalDate,
    val intakeKcal: Double,
    val burnInput: BurnInput?,
    val isTrainingDay: Boolean,
)

@Singleton
class EnergyRepository @Inject constructor(
    private val db: BaojiDatabase,
    private val time: TimeSource,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDay(days: Flow<LocalDate>): Flow<EnergyDay> = days.flatMapLatest { date ->
        val epoch = date.toEpochDay()
        val inputs = combine(
            db.profileDao().observe(),
            db.weightDao().observeLatestOnOrBefore(epoch),
            db.planDao().observeActivities(),
            db.trainingLogDao().observeSets(epoch),
            db.activityLogDao().observe(epoch),
        ) { profile, weight, activities, sets, extra ->
            if (profile == null || weight == null) {
                null
            } else {
                BurnInput(
                    sex = runCatching { Sex.valueOf(profile.sex) }.getOrDefault(Sex.MALE),
                    weightKg = weight.kg,
                    heightCm = profile.heightCm,
                    age = AgeCalculator.currentAge(profile.enteredAge, profile.enteredYear, date),
                    dailyActivityKcal = activities.sumOf { it.kcal },
                    trainingKcal = CalorieEngine.trainingKcal(sets.map { it.kcal } + extra.map { it.kcal }),
                )
            }
        }
        combine(
            inputs,
            db.mealDao().observeLogs(epoch),
            db.planDao().observeActivePlans(),
            db.planDao().observeActiveExercises(),
        ) { input, meals, plans, exercises ->
            val todays = TrainingDayBuilder.plansFor(date, plans).map { it.id }.toSet()
            EnergyDay(
                date = date,
                intakeKcal = CalorieEngine.intake(meals.map { it.kcal }),
                burnInput = input,
                isTrainingDay = exercises.any { it.planId in todays },
            )
        }
    }

    fun burnedNow(day: EnergyDay): Double? = day.burnInput?.let { CalorieEngine.burnedAt(it, time.now()) }
}
