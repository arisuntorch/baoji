package com.baoji.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 'me'")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 'me'")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_log ORDER BY date")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log ORDER BY date")
    suspend fun all(): List<WeightLogEntity>

    @Query("SELECT * FROM weight_log WHERE date <= :date ORDER BY date DESC LIMIT 1")
    suspend fun latestOnOrBefore(date: Long): WeightLogEntity?

    @Query("SELECT * FROM weight_log WHERE date <= :date ORDER BY date DESC LIMIT 1")
    fun observeLatestOnOrBefore(date: Long): Flow<WeightLogEntity?>

    @Query("SELECT * FROM weight_log WHERE date = :date")
    suspend fun byDate(date: Long): WeightLogEntity?

    @Upsert
    suspend fun upsert(log: WeightLogEntity)
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary ORDER BY date DESC")
    fun observeAll(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary ORDER BY date")
    suspend fun all(): List<DiaryEntity>

    @Query("SELECT * FROM diary WHERE date = :date")
    suspend fun byDate(date: Long): DiaryEntity?

    @Upsert
    suspend fun upsert(diary: DiaryEntity)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM training_plan WHERE archived = 0 ORDER BY sortOrder")
    fun observeActivePlans(): Flow<List<TrainingPlanEntity>>

    @Query("SELECT * FROM training_plan WHERE archived = 0 ORDER BY sortOrder")
    suspend fun activePlans(): List<TrainingPlanEntity>

    @Query("SELECT * FROM training_plan")
    suspend fun allPlans(): List<TrainingPlanEntity>

    @Query("SELECT * FROM exercise WHERE archived = 0 ORDER BY sortOrder")
    fun observeActiveExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE archived = 0 ORDER BY sortOrder")
    suspend fun activeExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise")
    suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun exercise(id: String): ExerciseEntity?

    @Upsert
    suspend fun upsertPlan(plan: TrainingPlanEntity)

    @Upsert
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Query("UPDATE training_plan SET archived = 1, updatedAt = :now WHERE archived = 0")
    suspend fun archiveAllPlans(now: Long)

    @Query("UPDATE exercise SET archived = 1, updatedAt = :now WHERE archived = 0")
    suspend fun archiveAllExercises(now: Long)

    @Query("SELECT * FROM daily_activity WHERE archived = 0")
    fun observeActivities(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity WHERE archived = 0")
    suspend fun activeActivities(): List<DailyActivityEntity>

    @Query("SELECT * FROM daily_activity")
    suspend fun allActivities(): List<DailyActivityEntity>

    @Upsert
    suspend fun upsertActivity(activity: DailyActivityEntity)

    @Query("UPDATE daily_activity SET archived = 1, updatedAt = :now WHERE archived = 0")
    suspend fun archiveAllActivities(now: Long)
}

@Dao
interface TrainingLogDao {
    @Query("SELECT * FROM set_log WHERE date = :date ORDER BY timestamp")
    fun observeSets(date: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_log WHERE date = :date ORDER BY timestamp")
    suspend fun setsOn(date: Long): List<SetLogEntity>

    @Query("SELECT * FROM set_log ORDER BY timestamp")
    suspend fun allSets(): List<SetLogEntity>

    /** All sets of one exercise before a date; newest date first. */
    @Query("SELECT * FROM set_log WHERE exerciseId = :exerciseId AND date < :beforeDate ORDER BY date DESC, timestamp")
    suspend fun setsBefore(exerciseId: String, beforeDate: Long): List<SetLogEntity>

    /** For every exercise name, the sets of its most recent day before [beforeDate]. */
    @Query(
        """
        SELECT * FROM set_log AS s WHERE s.date < :beforeDate AND s.date = (
            SELECT MAX(s2.date) FROM set_log AS s2 WHERE s2.exerciseName = s.exerciseName AND s2.date < :beforeDate
        ) ORDER BY s.timestamp
        """,
    )
    fun observeLastSessionSets(beforeDate: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_log WHERE exerciseName = :name ORDER BY date, timestamp")
    fun observeSetsByName(name: String): Flow<List<SetLogEntity>>

    @Upsert
    suspend fun upsertSet(set: SetLogEntity)

    @Query("SELECT * FROM set_log WHERE exerciseId = :exerciseId AND date = :date ORDER BY timestamp DESC LIMIT 1")
    suspend fun lastSet(exerciseId: String, date: Long): SetLogEntity?

    @Query("DELETE FROM set_log WHERE id = :id")
    suspend fun deleteSet(id: String)

    @Query("SELECT * FROM training_session WHERE date = :date")
    fun observeSession(date: Long): Flow<TrainingSessionEntity?>

    @Query("SELECT * FROM training_session WHERE date = :date")
    suspend fun session(date: Long): TrainingSessionEntity?

    @Query("SELECT * FROM training_session ORDER BY date")
    suspend fun allSessions(): List<TrainingSessionEntity>

    @Upsert
    suspend fun upsertSession(session: TrainingSessionEntity)
}

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_task WHERE archived = 0 ORDER BY sortOrder")
    fun observeActiveTasks(): Flow<List<MealTaskEntity>>

    @Query("SELECT * FROM meal_task WHERE archived = 0 ORDER BY sortOrder")
    suspend fun activeTasks(): List<MealTaskEntity>

    @Query("SELECT * FROM meal_task")
    suspend fun allTasks(): List<MealTaskEntity>

    @Upsert
    suspend fun upsertTask(task: MealTaskEntity)

    @Query("UPDATE meal_task SET archived = 1, updatedAt = :now WHERE archived = 0")
    suspend fun archiveAllTasks(now: Long)

    @Query("SELECT * FROM meal_log WHERE date = :date ORDER BY timestamp")
    fun observeLogs(date: Long): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_log WHERE date = :date ORDER BY timestamp")
    suspend fun logsOn(date: Long): List<MealLogEntity>

    @Query("SELECT * FROM meal_log ORDER BY timestamp")
    suspend fun allLogs(): List<MealLogEntity>

    @Upsert
    suspend fun upsertLog(log: MealLogEntity)

    @Query("DELETE FROM meal_log WHERE mealTaskId = :taskId AND date = :date")
    suspend fun deleteLog(taskId: String, date: Long)
}

@Dao
interface FreeLogDao {
    @Query("SELECT * FROM free_log WHERE date = :date AND type = :type ORDER BY timestamp")
    fun observe(date: Long, type: String): Flow<List<FreeLogEntity>>

    @Query("SELECT * FROM free_log ORDER BY timestamp")
    suspend fun all(): List<FreeLogEntity>

    @Upsert
    suspend fun upsert(log: FreeLogEntity)
}

@Dao
interface ExportDao {
    @Query("SELECT * FROM export_record ORDER BY timestamp DESC LIMIT 1")
    fun observeLast(): Flow<ExportRecordEntity?>

    @Query("SELECT * FROM export_record ORDER BY timestamp DESC LIMIT 1")
    suspend fun last(): ExportRecordEntity?

    @Query("SELECT * FROM export_record ORDER BY timestamp")
    suspend fun all(): List<ExportRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExportRecordEntity)
}

/** Bulk operations used by restore and "clear all data". */
@Dao
abstract class MaintenanceDao {
    @Query("DELETE FROM profile") abstract suspend fun clearProfile()
    @Query("DELETE FROM weight_log") abstract suspend fun clearWeights()
    @Query("DELETE FROM diary") abstract suspend fun clearDiary()
    @Query("DELETE FROM training_plan") abstract suspend fun clearPlans()
    @Query("DELETE FROM exercise") abstract suspend fun clearExercises()
    @Query("DELETE FROM set_log") abstract suspend fun clearSets()
    @Query("DELETE FROM training_session") abstract suspend fun clearSessions()
    @Query("DELETE FROM daily_activity") abstract suspend fun clearActivities()
    @Query("DELETE FROM meal_task") abstract suspend fun clearMealTasks()
    @Query("DELETE FROM meal_log") abstract suspend fun clearMealLogs()
    @Query("DELETE FROM free_log") abstract suspend fun clearFreeLogs()
    @Query("DELETE FROM export_record") abstract suspend fun clearExports()

    @Transaction
    open suspend fun clearAll() {
        clearProfile(); clearWeights(); clearDiary(); clearPlans(); clearExercises(); clearSets()
        clearSessions(); clearActivities(); clearMealTasks(); clearMealLogs(); clearFreeLogs(); clearExports()
    }

    @Insert abstract suspend fun insertWeights(items: List<WeightLogEntity>)
    @Insert abstract suspend fun insertDiaries(items: List<DiaryEntity>)
    @Insert abstract suspend fun insertPlans(items: List<TrainingPlanEntity>)
    @Insert abstract suspend fun insertExercises(items: List<ExerciseEntity>)
    @Insert abstract suspend fun insertSets(items: List<SetLogEntity>)
    @Insert abstract suspend fun insertSessions(items: List<TrainingSessionEntity>)
    @Insert abstract suspend fun insertActivities(items: List<DailyActivityEntity>)
    @Insert abstract suspend fun insertMealTasks(items: List<MealTaskEntity>)
    @Insert abstract suspend fun insertMealLogs(items: List<MealLogEntity>)
    @Insert abstract suspend fun insertFreeLogs(items: List<FreeLogEntity>)
    @Insert abstract suspend fun insertExports(items: List<ExportRecordEntity>)
    @Insert abstract suspend fun insertProfile(item: ProfileEntity)
}
