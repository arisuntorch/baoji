package com.baoji.data.io

import androidx.room.withTransaction
import com.baoji.data.db.BaojiDatabase
import com.baoji.data.prefs.NutritionTargets
import com.baoji.data.prefs.SettingsStore
import kotlinx.serialization.SerializationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRestorer @Inject constructor(
    private val db: BaojiDatabase,
    private val settings: SettingsStore,
) {
    /** Checks the file before anything is touched. Only a full ("all") export can be restored. */
    fun parse(text: String): ValidationResult<BackupDto> {
        val env = ImportValidator.parseEnvelope(text, ImportKind.BACKUP)
        if (env is ValidationResult.Invalid) return env
        val dto = try {
            Exporter.JSON.decodeFromString(BackupDto.serializer(), text)
        } catch (e: SerializationException) {
            return ValidationResult.Invalid(listOf(ImportIssue(1, "备份文件结构不完整：${e.message?.take(80)}")))
        } catch (e: IllegalArgumentException) {
            return ValidationResult.Invalid(listOf(ImportIssue(1, "备份文件结构不完整：${e.message?.take(80)}")))
        }
        if (dto.range.type != ExportRange.All.wire) {
            return ValidationResult.Invalid(listOf(ImportIssue(1, "只能用「全部」导出的文件恢复")))
        }
        return ValidationResult.Ok(dto)
    }

    /** Replaces every row in one transaction; on failure the old data stays intact. */
    suspend fun restore(dto: BackupDto) {
        val m = db.maintenanceDao()
        db.withTransaction {
            m.clearAll()
            dto.profile?.let { m.insertProfile(BackupMapper.toEntity(it)) }
            m.insertActivities(dto.daily.map(BackupMapper::toEntity))
            m.insertPlans(dto.plans.map(BackupMapper::toEntity))
            m.insertExercises(dto.plans.flatMap { p -> p.exercises.map { BackupMapper.toEntity(it, p.id) } })
            m.insertMealTasks(dto.meals.map(BackupMapper::toEntity))
            m.insertWeights(dto.days.mapNotNull(BackupMapper::weightOf))
            m.insertDiaries(dto.days.mapNotNull(BackupMapper::diaryOf))
            m.insertSessions(dto.days.mapNotNull { d -> d.training?.let { BackupMapper.toEntity(it, BackupMapper.epochDay(d)) } })
            m.insertSets(dto.days.flatMap { d -> d.sets.map { BackupMapper.toEntity(it, BackupMapper.epochDay(d)) } })
            m.insertMealLogs(dto.days.flatMap { d -> d.meals.map { BackupMapper.toEntity(it, BackupMapper.epochDay(d)) } })
            m.insertFreeLogs(dto.days.flatMap { d -> d.free.map { BackupMapper.toEntity(it, BackupMapper.epochDay(d)) } })
            m.insertActivityLogs(dto.days.flatMap { d -> d.activities.map { BackupMapper.toEntity(it, BackupMapper.epochDay(d)) } })
            m.insertExports(dto.exports.map(BackupMapper::toEntity))
        }
        settings.setTargets(NutritionTargets(dto.targets.protein, dto.targets.carbs, dto.targets.fat))
    }

    suspend fun clearAll() {
        db.maintenanceDao().clearAll()
        settings.clear()
    }
}
