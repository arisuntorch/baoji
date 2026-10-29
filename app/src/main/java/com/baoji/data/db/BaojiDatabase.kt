package com.baoji.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        ActivityLogEntity::class,
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
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        const val VERSION = 3
        const val NAME = "baoji.db"

        /** Append each schema change as `Migration(n, n + 1)`; never fall back to destructive migration. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `activity_log` (`id` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `kind` TEXT NOT NULL, `name` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                        "`unit` TEXT NOT NULL, `kcal` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_log_date` ON `activity_log` (`date`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `activity_log` ADD COLUMN `reps` INTEGER")
                db.execSQL("ALTER TABLE `activity_log` ADD COLUMN `weightKg` REAL")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
