package io.github.meko123456.dghiuri.domain

import io.github.meko123456.dghiuri.data.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EntryStatsTest {

    private val today = LocalDate.of(2026, 8, 27).toEpochDay()

    private fun entry(day: Long, markdown: String = "hello world", mood: Int? = null) =
        Entry(epochDay = day, markdown = markdown, mood = mood, updatedAt = 0L)

    @Test
    fun `empty list gives zeros and a null average mood`() {
        val stats = EntryStats.from(emptyList(), today)
        assertEquals(0, stats.total)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.longestStreak)
        assertNull(stats.averageMood)
        assertEquals(0, stats.totalWords)
        assertEquals(0, stats.writtenThisMonth)
        assertEquals(EntryStats.EMPTY, stats)
    }

    @Test
    fun `total counts every entry`() {
        val stats = EntryStats.from(listOf(entry(today), entry(today - 1), entry(today - 10)), today)
        assertEquals(3, stats.total)
    }

    @Test
    fun `average mood ignores entries without a mood`() {
        val entries = listOf(
            entry(today, mood = 5),
            entry(today - 1, mood = null),
            entry(today - 2, mood = 2),
            entry(today - 3, mood = null),
        )
        assertEquals(3.5, EntryStats.from(entries, today).averageMood!!, 1e-9)
    }

    @Test
    fun `average mood is null when no entry has a mood`() {
        val entries = listOf(entry(today), entry(today - 1))
        assertNull(EntryStats.from(entries, today).averageMood)
    }

    @Test
    fun `written this month includes the first and last day of the month only`() {
        val firstOfMonth = LocalDate.of(2026, 8, 1).toEpochDay()
        val lastOfMonth = LocalDate.of(2026, 8, 31).toEpochDay()
        val lastOfPrevious = LocalDate.of(2026, 7, 31).toEpochDay()
        val firstOfNext = LocalDate.of(2026, 9, 1).toEpochDay()
        val entries = listOf(
            entry(firstOfMonth),
            entry(lastOfMonth),
            entry(lastOfPrevious),
            entry(firstOfNext),
        )
        assertEquals(2, EntryStats.from(entries, today).writtenThisMonth)
    }

    @Test
    fun `written this month distinguishes the same month in another year`() {
        val sameMonthLastYear = LocalDate.of(2025, 8, 15).toEpochDay()
        val entries = listOf(entry(sameMonthLastYear), entry(today))
        assertEquals(1, EntryStats.from(entries, today).writtenThisMonth)
    }

    @Test
    fun `streaks delegate to StreakEngine`() {
        // Current run: today, yesterday. Older, longer run: 4 days ending a week ago.
        val entries = listOf(
            entry(today),
            entry(today - 1),
            entry(today - 7),
            entry(today - 8),
            entry(today - 9),
            entry(today - 10),
        )
        val writtenDays = entries.map { it.epochDay }.toSet()
        val stats = EntryStats.from(entries, today)
        assertEquals(2, stats.currentStreak)
        assertEquals(4, stats.longestStreak)
        assertEquals(StreakEngine.currentStreak(writtenDays, today), stats.currentStreak)
        assertEquals(StreakEngine.longestStreak(writtenDays), stats.longestStreak)
    }

    @Test
    fun `current streak survives when today is not yet written`() {
        val entries = listOf(entry(today - 1), entry(today - 2), entry(today - 3))
        assertEquals(3, EntryStats.from(entries, today).currentStreak)
    }

    @Test
    fun `total words sums StreakEngine word counts`() {
        val entries = listOf(
            entry(today, markdown = "one two three"),
            entry(today - 1, markdown = "  four\n\nfive  "),
            entry(today - 2, markdown = ""),
        )
        val expected = entries.sumOf { StreakEngine.wordCount(it.markdown) }
        val stats = EntryStats.from(entries, today)
        assertEquals(5, stats.totalWords)
        assertEquals(expected, stats.totalWords)
    }
}
