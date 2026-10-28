package com.baoji.data.io

import androidx.room.withTransaction
import com.baoji.data.db.ActivityLogEntity
import com.baoji.data.db.BaojiDatabase
import com.baoji.data.db.DailyActivityEntity
import com.baoji.data.db.DiaryEntity
import com.baoji.data.db.ExerciseEntity
import com.baoji.data.db.ExportRecordEntity
import com.baoji.data.db.FreeLogEntity
import com.baoji.data.db.MealLogEntity
import com.baoji.data.db.MealTaskEntity
import com.baoji.data.db.ProfileEntity
import com.baoji.data.db.SetLogEntity
import com.baoji.data.db.TrainingPlanEntity
import com.baoji.data.db.TrainingSessionEntity
import com.baoji.data.db.WeightLogEntity

/** Every table read in one consistent transaction. A personal log stays small enough for this. */
data class DbSnapshot(
    val profile: ProfileEntity?,
    val weights: List<WeightLogEntity>,
    val diaries: List<DiaryEntity>,
    val plans: List<TrainingPlanEntity>,
    val exercises: List<ExerciseEntity>,
    val sets: List<SetLogEntity>,
    val sessions: List<TrainingSessionEntity>,
    val activities: List<DailyActivityEntity>,
    val mealTasks: List<MealTaskEntity>,
    val mealLogs: List<MealLogEntity>,
    val freeLogs: List<FreeLogEntity>,
    val exports: List<ExportRecordEntity>,
    val activityLogs: List<ActivityLogEntity>,
) {
    fun allDates(): Set<Long> =
        (weights.map { it.date } + diaries.map { it.date } + sets.map { it.date } + sessions.map { it.date } +
            mealLogs.map { it.date } + freeLogs.map { it.date } + activityLogs.map { it.date }).toSet()

    /** Days with any per-day record updated after [since]. Whole days are exported, not single rows. */
    fun datesTouchedAfter(since: Long): Set<Long> =
        (weights.filter { it.updatedAt > since }.map { it.date } +
            diaries.filter { it.updatedAt > since }.map { it.date } +
            sets.filter { it.updatedAt > since }.map { it.date } +
            sessions.filter { it.updatedAt > since }.map { it.date } +
            mealLogs.filter { it.updatedAt > since }.map { it.date } +
            freeLogs.filter { it.updatedAt > since }.map { it.date } +
            activityLogs.filter { it.updatedAt > since }.map { it.date }).toSet()

    companion object {
        suspend fun load(db: BaojiDatabase): DbSnapshot = db.withTransaction {
            DbSnapshot(
                profile = db.profileDao().get(),
                weights = db.weightDao().all(),
                diaries = db.diaryDao().all(),
                plans = db.planDao().allPlans(),
                exercises = db.planDao().allExercises(),
                sets = db.trainingLogDao().allSets(),
                sessions = db.trainingLogDao().allSessions(),
                activities = db.planDao().allActivities(),
                mealTasks = db.mealDao().allTasks(),
                mealLogs = db.mealDao().allLogs(),
                freeLogs = db.freeLogDao().all(),
                exports = db.exportDao().all(),
                activityLogs = db.activityLogDao().all(),
            )
        }
    }
}
