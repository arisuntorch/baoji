package com.baoji.data.repo

import com.baoji.core.time.TimeSource
import com.baoji.data.db.BaojiDatabase
import com.baoji.data.db.DiaryEntity
import com.baoji.data.db.FreeLogEntity
import com.baoji.data.db.WeightLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Diary, body weight and free-text logs (加练 / 加餐). */
@Singleton
class JournalRepository @Inject constructor(
    private val db: BaojiDatabase,
    private val time: TimeSource,
) {
    fun observeDiaries(): Flow<List<DiaryEntity>> = db.diaryDao().observeAll()

    suspend fun diary(epochDay: Long): DiaryEntity? = db.diaryDao().byDate(epochDay)

    /** Autosave target. Keeps the row id stable so edits don't create duplicates. */
    suspend fun saveDiary(epochDay: Long, content: String) {
        val existing = db.diaryDao().byDate(epochDay)
        if (existing?.content == content) return
        if (existing == null && content.isBlank()) return
        db.diaryDao().upsert(
            DiaryEntity(existing?.id ?: UUID.randomUUID().toString(), epochDay, content, time.nowMillis()),
        )
    }

    fun observeWeights(): Flow<List<WeightLogEntity>> = db.weightDao().observeAll()

    /** Same-day entries overwrite. */
    suspend fun saveWeight(kg: Double, epochDay: Long = time.todayEpochDay()) {
        require(kg in MIN_KG..MAX_KG) { "weight out of range" }
        val existing = db.weightDao().byDate(epochDay)
        db.weightDao().upsert(WeightLogEntity(existing?.id ?: UUID.randomUUID().toString(), epochDay, kg, time.nowMillis()))
    }

    fun observeFreeLogs(type: String, epochDay: Long): Flow<List<FreeLogEntity>> = db.freeLogDao().observe(epochDay, type)

    suspend fun addFreeLog(type: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val now = time.nowMillis()
        db.freeLogDao().upsert(FreeLogEntity(UUID.randomUUID().toString(), time.todayEpochDay(), now, type, clean.take(MAX_TEXT), now))
    }

    companion object {
        const val MIN_KG = 20.0
        const val MAX_KG = 300.0
        const val MAX_TEXT = 500
    }
}
