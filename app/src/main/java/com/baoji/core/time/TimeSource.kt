package com.baoji.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** All "now" reads go through here so tests can pin the clock. */
@Singleton
class TimeSource @Inject constructor(private val clock: Clock) {
    fun nowMillis(): Long = clock.millis()
    fun now(): ZonedDateTime = ZonedDateTime.now(clock)
    fun today(): LocalDate = LocalDate.now(clock)
    fun todayEpochDay(): Long = today().toEpochDay()

    /** Emits today's date, then again right after each midnight while collected. */
    fun days(): Flow<LocalDate> = flow {
        while (true) {
            val now = now()
            emit(now.toLocalDate())
            val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, next).toMillis().coerceAtLeast(1) + 50)
        }
    }.distinctUntilChanged()
}
