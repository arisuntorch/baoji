package com.baoji.testing

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baoji.core.time.TimeSource
import com.baoji.data.db.BaojiDatabase
import com.baoji.data.prefs.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock the test can move. */
class MutableClock(var instant: Instant, private val zone: ZoneId = ZoneId.of("Asia/Shanghai")) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)
    override fun instant(): Instant = instant

    fun set(local: LocalDateTime) {
        instant = local.atZone(zone).toInstant()
    }

    companion object {
        fun at(local: LocalDateTime) = MutableClock(local.toInstant(ZoneOffset.ofHours(8)))
    }
}

/** In-memory database + throwaway DataStore + controllable clock. */
class TestEnv(start: LocalDateTime = LocalDateTime.of(2026, 10, 12, 9, 0)) {
    private val context: Context = ApplicationProvider.getApplicationContext()
    val clock = MutableClock.at(start)
    val time = TimeSource(clock)
    val db: BaojiDatabase = Room.inMemoryDatabaseBuilder(context, BaojiDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storeFile = File(context.cacheDir, "test-${System.nanoTime()}.preferences_pb")
    val settings = SettingsStore(PreferenceDataStoreFactory.create(scope = scope) { storeFile })

    fun close() {
        db.close()
        scope.cancel()
        storeFile.delete()
    }
}
