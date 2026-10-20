package com.baoji.data.repo

import androidx.room.withTransaction
import com.baoji.core.time.TimeSource
import com.baoji.data.db.BaojiDatabase
import com.baoji.data.db.MealLogEntity
import com.baoji.data.db.MealTaskEntity
import com.baoji.data.prefs.NutritionTargets
import com.baoji.data.prefs.SettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class Macros(val protein: Double, val carbs: Double, val fat: Double, val kcal: Double) {
    companion object {
        val ZERO = Macros(0.0, 0.0, 0.0, 0.0)
    }
}

data class MealRow(val task: MealTaskEntity, val checked: Boolean)

data class DietDay(
    val date: LocalDate,
    val rows: List<MealRow>,
    val eaten: Macros,
    val targets: NutritionTargets,
)

@Singleton
class DietRepository @Inject constructor(
    private val db: BaojiDatabase,
    private val settings: SettingsStore,
    private val time: TimeSource,
) {
    private val meals get() = db.mealDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDay(days: Flow<LocalDate>): Flow<DietDay> = days.flatMapLatest { date ->
        combine(meals.observeActiveTasks(), meals.observeLogs(date.toEpochDay()), settings.targets) { tasks, logs, targets ->
            val checkedIds = logs.map { it.mealTaskId }.toSet()
            DietDay(date, tasks.map { MealRow(it, it.id in checkedIds) }, sum(logs), targets)
        }
    }

    /** Toggles today's check for a task. A check stores a nutrition snapshot of the task as it is now. */
    suspend fun toggle(taskId: String): Boolean = db.withTransaction {
        val today = time.todayEpochDay()
        val already = meals.logsOn(today).any { it.mealTaskId == taskId }
        if (already) {
            meals.deleteLog(taskId, today)
            return@withTransaction false
        }
        val task = meals.activeTasks().firstOrNull { it.id == taskId } ?: return@withTransaction false
        val now = time.nowMillis()
        meals.upsertLog(
            MealLogEntity(
                id = UUID.randomUUID().toString(), mealTaskId = task.id, date = today, timestamp = now,
                name = task.name, amount = task.amount, unit = task.unit, protein = task.protein,
                carbs = task.carbs, fat = task.fat, kcal = task.kcal, updatedAt = now,
            ),
        )
        true
    }

    companion object {
        fun sum(logs: List<MealLogEntity>) = Macros(
            protein = logs.sumOf { it.protein },
            carbs = logs.sumOf { it.carbs },
            fat = logs.sumOf { it.fat },
            kcal = logs.sumOf { it.kcal },
        )
    }
}
