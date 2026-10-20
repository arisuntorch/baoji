package com.baoji.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        ProfileEntity::class,
        WeightLogEntity::class,
        DiaryEntity::class,
        TrainingPlanEntity::class,
        ExerciseEntity::class,
        SetLogEntity::class,
        TrainingSessionEntity::class,
        DailyActivityEntity::class,
        MealTaskEntity::class,
        MealLogEntity::class,
        FreeLogEntity::class,
        ExportRecordEntity::class,
    ],
    version = BaojiDatabase.VERSION,
    exportSchema = true,
)
abstract class BaojiDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun diaryDao(): DiaryDao
    abstract fun planDao(): PlanDao
    abstract fun trainingLogDao(): TrainingLogDao
    abstract fun mealDao(): MealDao
    abstract fun freeLogDao(): FreeLogDao
    abstract fun exportDao(): ExportDao
    abstract fun maintenanceDao(): MaintenanceDao

    companion object {
        const val VERSION = 1
        const val NAME = "baoji.db"

        /** Append each schema change as `Migration(n, n + 1)`; never fall back to destructive migration. */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
