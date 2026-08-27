package io.github.meko123456.dghiuri.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class DayClockTest {

    private val tbilisi = ZoneId.of("Asia/Tbilisi")

    @Test
    fun `epoch day follows the local calendar, not UTC`() {
        // 00:30 in Tbilisi (UTC+4) is still the previous day in UTC.
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 8, 28, 0, 30), tbilisi)
        assertEquals(LocalDate.of(2026, 8, 28).toEpochDay(), DayClock.epochDay(now))
        assertEquals(LocalDate.of(2026, 8, 27).toEpochDay(), DayClock.epochDay(now.withZoneSameInstant(ZoneOffset.UTC)))
    }

    @Test
    fun `millis until midnight counts down to the next local day`() {
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 8, 27, 23, 59, 30), tbilisi)
        assertEquals(30_000L, DayClock.millisUntilNextMidnight(now))

        val morning = ZonedDateTime.of(LocalDateTime.of(2026, 8, 27, 8, 0), tbilisi)
        assertEquals(16 * 60 * 60 * 1000L, DayClock.millisUntilNextMidnight(morning))
    }

    @Test
    fun `exactly at midnight the wait is a full day`() {
        val midnight = ZonedDateTime.of(LocalDateTime.of(2026, 8, 27, 0, 0), tbilisi)
        assertEquals(24 * 60 * 60 * 1000L, DayClock.millisUntilNextMidnight(midnight))
    }

    @Test
    fun `the wait is always positive so a loop cannot spin`() {
        val justBefore = ZonedDateTime.of(LocalDateTime.of(2026, 8, 27, 23, 59, 59, 999_999_999), tbilisi)
        assertTrue(DayClock.millisUntilNextMidnight(justBefore) >= 1L)
    }

    @Test
    fun `a day boundary lands on the next midnight after a DST change`() {
        // Europe/Berlin, the night the clocks go back: the day is 25 hours long.
        val berlin = ZoneId.of("Europe/Berlin")
        val start = ZonedDateTime.of(LocalDateTime.of(2026, 10, 25, 0, 0), berlin)
        assertEquals(25 * 60 * 60 * 1000L, DayClock.millisUntilNextMidnight(start))
        val next = start.plus(java.time.Duration.ofMillis(DayClock.millisUntilNextMidnight(start)))
        assertEquals(LocalDate.of(2026, 10, 26), next.toLocalDate())
    }

    @Test
    fun `utc millis and epoch day round-trip`() {
        val day = LocalDate.of(2026, 8, 27).toEpochDay()
        val millis = DayClock.utcMillisOfEpochDay(day)
        assertEquals(day, DayClock.epochDayOfUtcMillis(millis))
        // Anywhere inside that UTC day is still that day; one millisecond before is the day before.
        assertEquals(day, DayClock.epochDayOfUtcMillis(millis + 86_399_999L))
        assertEquals(day - 1, DayClock.epochDayOfUtcMillis(millis - 1L))
    }
}
