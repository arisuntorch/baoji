package com.baoji.data.io

import com.baoji.core.time.TimeSource
import com.baoji.data.db.ActivityKind
import com.baoji.data.db.BaojiDatabase
import com.baoji.data.db.ExportRecordEntity
import com.baoji.data.prefs.SettingsStore
import com.baoji.domain.AgeCalculator
import com.baoji.domain.BurnInput
import com.baoji.domain.CalorieEngine
import com.baoji.domain.Sex
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ExportRange {
    val wire: String

    data object All : ExportRange { override val wire = "all" }
    data object SinceLast : ExportRange { override val wire = "since_last" }
    data class Dates(val from: LocalDate, val to: LocalDate) : ExportRange {
        override val wire = "dates"
        init { require(!from.isAfter(to)) { "from must not be after to" } }
    }
}

data class ExportFile(val fileName: String, val json: String)

@Singleton
class Exporter @Inject constructor(
    private val db: BaojiDatabase,
    private val settings: SettingsStore,
    private val time: TimeSource,
) {
    suspend fun build(range: ExportRange): BackupDto {
        val snapshot = DbSnapshot.load(db)
        val since = if (range is ExportRange.SinceLast) db.exportDao().last()?.timestamp ?: 0L else null
        val zone = time.now().zone
        val today = time.today()

        val dates: Set<Long> = when (range) {
            ExportRange.All -> snapshot.allDates()
            is ExportRange.Dates -> snapshot.allDates().filter { it in range.from.toEpochDay()..range.to.toEpochDay() }.toSet()
            ExportRange.SinceLast -> snapshot.datesTouchedAfter(since ?: 0L)
        }
        val days = dates.sorted().map { buildDay(snapshot, it, zone, today) }
        val profile = snapshot.profile?.let {
            ProfileDto(it.heightCm, it.sex, AgeCalculator.currentAge(it.enteredAge, it.enteredYear, today), it.enteredAge, it.enteredYear, it.updatedAt)
        }
        val t = settings.currentTargets()
        val isFull = range == ExportRange.All
        return BackupDto(
            exportedAt = time.now().toOffsetDateTime().toString(),
            range = rangeDto(range, since, zone),
            profile = profile,
            targets = TargetsDto(t.protein, t.carbs, t.fat),
            daily = snapshot.activities.filter { isFull || !it.archived }.map(BackupMapper::toDto),
            plans = snapshot.plans.filter { isFull || !it.archived }.map { p ->
                BackupMapper.toDto(p, snapshot.exercises.filter { it.planId == p.id && (isFull || !it.archived) })
            },
            meals = snapshot.mealTasks.filter { isFull || !it.archived }.map(BackupMapper::toDto),
            days = days,
            exports = if (isFull) snapshot.exports.map(BackupMapper::toDto) else emptyList(),
        )
    }

    suspend fun export(range: ExportRange): ExportFile {
        val json = JSON.encodeToString(BackupDto.serializer(), build(range))
        return ExportFile("baoji-${time.today()}.json", json)
    }

    /** Call once the file was actually saved or shared, so "since last export" moves forward. */
    suspend fun record(range: ExportRange) {
        val now = time.nowMillis()
        db.exportDao().insert(
            ExportRecordEntity(
                id = UUID.randomUUID().toString(),
                timestamp = now,
                rangeType = range.wire,
                fromDate = (range as? ExportRange.Dates)?.from?.toEpochDay(),
                toDate = (range as? ExportRange.Dates)?.to?.toEpochDay(),
                updatedAt = now,
            ),
        )
    }

    private fun buildDay(s: DbSnapshot, epochDay: Long, zone: ZoneId, today: LocalDate): DayDto {
        val date = LocalDate.ofEpochDay(epochDay)
        val sets = s.sets.filter { it.date == epochDay }
        val meals = s.mealLogs.filter { it.date == epochDay }
        val activities = s.activityLogs.filter { it.date == epochDay }
        val weightRow = s.weights.firstOrNull { it.date == epochDay }
        val diaryRow = s.diaries.firstOrNull { it.date == epochDay }
        val session = s.sessions.firstOrNull { it.date == epochDay }
        val kcalIn = CalorieEngine.intake(meals.map { it.kcal })
        return DayDto(
            date = date.toString(),
            weight = weightRow?.kg,
            diary = diaryRow?.content,
            training = session?.let {
                SessionDto(it.id, it.planId, clock(it.startedAt, zone), it.finishedAt?.let { f -> clock(f, zone) }, it.startedAt, it.finishedAt, it.updatedAt)
            },
            sets = sets.map { BackupMapper.toDto(it, clock(it.timestamp, zone)) },
            meals = meals.map { BackupMapper.toDto(it, clock(it.timestamp, zone)) },
            free = s.freeLogs.filter { it.date == epochDay }.map { BackupMapper.toDto(it, clock(it.timestamp, zone)) },
            activities = activities.map { BackupMapper.toDto(it, clock(it.timestamp, zone)) },
            kcalIn = round1(kcalIn),
            kcalOut = kcalOut(
                s, date, sets.sumOf { it.kcal } + activities.sumOf { it.kcal },
                hasSteps = activities.any { it.kind == ActivityKind.STEPS }, zone, today,
            )?.let(::round1),
            ids = DayIdsDto(weightRow?.id, weightRow?.updatedAt ?: 0, diaryRow?.id, diaryRow?.updatedAt ?: 0)
                .takeIf { weightRow != null || diaryRow != null },
        )
    }

    private fun kcalOut(s: DbSnapshot, date: LocalDate, trainingKcal: Double, hasSteps: Boolean, zone: ZoneId, today: LocalDate): Double? {
        val p = s.profile ?: return null
        val kg = s.weights.filter { it.date <= date.toEpochDay() }.maxByOrNull { it.date }?.kg ?: return null
        val sex = runCatching { Sex.valueOf(p.sex) }.getOrNull() ?: return null
        val input = BurnInput(
            sex = sex,
            weightKg = kg,
            heightCm = p.heightCm,
            age = AgeCalculator.currentAge(p.enteredAge, p.enteredYear, date),
            dailyActivityKcal = s.activities.filter { !it.archived }.sumOf { it.kcal },
            trainingKcal = trainingKcal,
            defaultStepsKcal = if (hasSteps) 0.0 else CalorieEngine.stepsKcal(CalorieEngine.DEFAULT_STEPS, kg),
        )
        return if (date == today) CalorieEngine.burnedAt(input, time.now().withZoneSameInstant(zone))
        else CalorieEngine.burnedFullDay(input)
    }

    private fun rangeDto(range: ExportRange, since: Long?, zone: ZoneId) = when (range) {
        ExportRange.All -> RangeDto(range.wire)
        ExportRange.SinceLast -> RangeDto(range.wire, from = since?.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toString() })
        is ExportRange.Dates -> RangeDto(range.wire, range.from.toString(), range.to.toString())
    }

    companion object {
        val JSON = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        private val CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        internal fun clock(ts: Long, zone: ZoneId): String = Instant.ofEpochMilli(ts).atZone(zone).format(CLOCK_FORMAT)
        internal fun round1(v: Double): Double = Math.round(v * 10) / 10.0
    }
}
