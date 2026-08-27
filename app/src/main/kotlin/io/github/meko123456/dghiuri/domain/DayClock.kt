package io.github.meko123456.dghiuri.domain

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure arithmetic for "which day is it" and "when does the day roll over", so the home screen
 * can refresh its notion of today at local midnight instead of freezing it at the last database
 * emission.
 */
object DayClock {

    private const val MILLIS_PER_DAY = 86_400_000L

    /** Epoch day of a UTC midnight timestamp, as the Material date picker reports selections. */
    fun epochDayOfUtcMillis(utcMillis: Long): Long = Math.floorDiv(utcMillis, MILLIS_PER_DAY)

    /** UTC midnight timestamp of [epochDay], the form the Material date picker takes as input. */
    fun utcMillisOfEpochDay(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

    /** The local epoch day of [now]. */
    fun epochDay(now: ZonedDateTime): Long = now.toLocalDate().toEpochDay()

    /**
     * Milliseconds from [now] until the next local midnight in [now]'s zone. Always at least 1 so
     * a caller looping on it can never spin, even exactly at midnight or across a DST change
     * that makes the next day start early.
     */
    fun millisUntilNextMidnight(now: ZonedDateTime): Long {
        val zone: ZoneId = now.zone
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
        return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
    }
}
