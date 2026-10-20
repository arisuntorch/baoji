package com.baoji.data.io

import androidx.room.withTransaction
import com.baoji.core.time.TimeSource
import com.baoji.data.db.BaojiDatabase
import com.baoji.data.db.DailyActivityEntity
import com.baoji.data.db.ExerciseEntity
import com.baoji.data.db.MealTaskEntity
import com.baoji.data.db.TrainingPlanEntity
import com.baoji.data.prefs.NutritionTargets
import com.baoji.data.prefs.SettingsStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes validated plans. Each import runs in one transaction; any failure rolls back everything.
 * MERGE matches by name (same name → update, new name → insert). REPLACE archives every
 * active template, then inserts fresh rows, so history still resolves to the old templates.
 */
@Singleton
class PlanImporter @Inject constructor(
    private val db: BaojiDatabase,
    private val settings: SettingsStore,
    private val time: TimeSource,
) {
    private val plans get() = db.planDao()
    private val meals get() = db.mealDao()

    suspend fun importTraining(data: ParsedTraining, mode: ImportMode) {
        val now = time.nowMillis()
        db.withTransaction {
            if (mode == ImportMode.REPLACE) {
                plans.archiveAllPlans(now)
                plans.archiveAllExercises(now)
                plans.archiveAllActivities(now)
            }
            mergeActivities(data.daily, now)
            val existingPlans = plans.activePlans().associateBy { it.name }
            val existingExercises = plans.activeExercises().groupBy { it.planId }
            val baseOrder = (existingPlans.values.maxOfOrNull { it.sortOrder } ?: -1) + 1
            data.plans.forEachIndexed { index, p ->
                val old = existingPlans[p.name]
                val plan = TrainingPlanEntity(
                    id = old?.id ?: newId(),
                    name = p.name,
                    days = p.days.joinToString(","),
                    sortOrder = old?.sortOrder ?: (baseOrder + index),
                    archived = false,
                    updatedAt = now,
                )
                plans.upsertPlan(plan)
                mergeExercises(plan.id, p.exercises, existingExercises[plan.id].orEmpty(), now)
            }
        }
    }

    private suspend fun mergeActivities(items: List<ParsedActivity>, now: Long) {
        val existing = plans.activeActivities().associateBy { it.name }
        items.forEach { a ->
            plans.upsertActivity(
                DailyActivityEntity(existing[a.name]?.id ?: newId(), a.name, a.kcal, archived = false, updatedAt = now),
            )
        }
    }

    private suspend fun mergeExercises(planId: String, items: List<ParsedExercise>, existing: List<ExerciseEntity>, now: Long) {
        val byName = existing.associateBy { it.name }
        val baseOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        items.forEachIndexed { index, e ->
            val old = byName[e.name]
            plans.upsertExercise(
                ExerciseEntity(
                    id = old?.id ?: newId(),
                    planId = planId,
                    name = e.name,
                    sets = e.sets,
                    repsMin = e.repsMin,
                    repsMax = e.repsMax,
                    kcalPerSet = e.kcalPerSet,
                    currentWeight = e.weight,
                    currentReps = old?.currentReps?.coerceIn(e.repsMin, e.repsMax) ?: e.repsMin,
                    sortOrder = old?.sortOrder ?: (baseOrder + index),
                    archived = false,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun importDiet(data: ParsedDiet, mode: ImportMode) {
        val now = time.nowMillis()
        db.withTransaction {
            if (mode == ImportMode.REPLACE) meals.archiveAllTasks(now)
            val existing = meals.activeTasks().associateBy { it.name }
            val baseOrder = (existing.values.maxOfOrNull { it.sortOrder } ?: -1) + 1
            data.meals.forEachIndexed { index, m ->
                val old = existing[m.name]
                meals.upsertTask(
                    MealTaskEntity(
                        id = old?.id ?: newId(),
                        name = m.name,
                        amount = m.amount,
                        unit = m.unit,
                        protein = m.protein,
                        carbs = m.carbs,
                        fat = m.fat,
                        kcal = m.kcal,
                        sortOrder = old?.sortOrder ?: (baseOrder + index),
                        archived = false,
                        updatedAt = now,
                    ),
                )
            }
        }
        // DataStore is outside Room's transaction; only touch it once the DB write committed.
        data.targets?.let { settings.setTargets(NutritionTargets(it.protein, it.carbs, it.fat)) }
    }

    private fun newId() = UUID.randomUUID().toString()
}
