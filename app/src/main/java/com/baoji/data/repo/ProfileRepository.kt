package com.baoji.data.repo

import androidx.room.withTransaction
import com.baoji.core.time.TimeSource
import com.baoji.data.db.BaojiDatabase
import com.baoji.data.db.ProfileEntity
import com.baoji.data.db.WeightLogEntity
import com.baoji.domain.AgeCalculator
import com.baoji.domain.Sex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class Profile(
    val heightCm: Double,
    val sex: Sex,
    val age: Int,
    val latestWeightKg: Double?,
    val avatarPath: String?,
)

data class ProfileInput(val heightCm: Double, val weightKg: Double, val sex: Sex, val age: Int) {
    fun errors(): List<String> = buildList {
        if (heightCm !in 100.0..250.0) add("身高应在 100–250 cm")
        if (weightKg !in JournalRepository.MIN_KG..JournalRepository.MAX_KG) add("体重应在 20–300 kg")
        if (age !in 10..100) add("年龄应在 10–100")
    }
}

@Singleton
class ProfileRepository @Inject constructor(
    private val db: BaojiDatabase,
    private val time: TimeSource,
) {
    fun observe(): Flow<Profile?> = combine(
        db.profileDao().observe(),
        db.weightDao().observeLatestOnOrBefore(Long.MAX_VALUE),
    ) { p, w -> p?.let { toProfile(it, w) } }

    private fun toProfile(p: ProfileEntity, w: WeightLogEntity?) = Profile(
        heightCm = p.heightCm,
        sex = runCatching { Sex.valueOf(p.sex) }.getOrDefault(Sex.MALE),
        age = AgeCalculator.currentAge(p.enteredAge, p.enteredYear, time.today()),
        latestWeightKg = w?.kg,
        avatarPath = p.avatarPath,
    )

    /** Onboarding: profile + first weight, atomically. */
    suspend fun create(input: ProfileInput) {
        require(input.errors().isEmpty()) { input.errors().joinToString() }
        val now = time.nowMillis()
        db.withTransaction {
            db.profileDao().upsert(
                ProfileEntity(
                    heightCm = input.heightCm, sex = input.sex.name, enteredAge = input.age,
                    enteredYear = time.today().year, avatarPath = null, updatedAt = now,
                ),
            )
            val today = time.todayEpochDay()
            val existing = db.weightDao().byDate(today)
            db.weightDao().upsert(WeightLogEntity(existing?.id ?: UUID.randomUUID().toString(), today, input.weightKg, now))
        }
    }

    suspend fun updateHeight(cm: Double) = update { it.copy(heightCm = cm) }

    suspend fun updateSex(sex: Sex) = update { it.copy(sex = sex.name) }

    /** Entering an age also records the year, so it advances every Jan 1 from here. */
    suspend fun updateAge(age: Int) = update { it.copy(enteredAge = age, enteredYear = time.today().year) }

    suspend fun updateAvatar(path: String?) = update { it.copy(avatarPath = path) }

    private suspend fun update(change: (ProfileEntity) -> ProfileEntity) {
        val p = db.profileDao().get() ?: return
        db.profileDao().upsert(change(p).copy(updatedAt = time.nowMillis()))
    }
}
