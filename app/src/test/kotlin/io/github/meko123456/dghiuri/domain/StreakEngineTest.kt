package io.github.meko123456.dghiuri.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakEngineTest {

    private val today = 20_000L

    @Test
    fun `no entries means no streak`() {
        assertEquals(0, StreakEngine.currentStreak(emptySet(), today))
        assertEquals(0, StreakEngine.longestStreak(emptySet()))
    }

    @Test
    fun `current streak counts back from today`() {
        val days = setOf(today, today - 1, today - 2)
        assertEquals(3, StreakEngine.currentStreak(days, today))
    }

    @Test
    fun `streak survives when today is not written yet but yesterday is`() {
        val days = setOf(today - 1, today - 2)
        assertEquals(2, StreakEngine.currentStreak(days, today))
    }

    @Test
    fun `streak is broken by a gap before yesterday`() {
        val days = setOf(today - 2, today - 3, today - 4)
        assertEquals(0, StreakEngine.currentStreak(days, today))
    }

    @Test
    fun `a gap ends the current run but older days do not extend it`() {
        val days = setOf(today, today - 1, today - 3, today - 4, today - 5)
        assertEquals(2, StreakEngine.currentStreak(days, today))
    }

    @Test
    fun `longest streak finds the best run anywhere in history`() {
        val days = setOf(today, 100L, 101L, 102L, 103L, 200L, 201L)
        assertEquals(4, StreakEngine.longestStreak(days))
    }

    @Test
    fun `longest streak of a single day is one`() {
        assertEquals(1, StreakEngine.longestStreak(setOf(42L)))
    }

    @Test
    fun `longest streak is at least the current streak`() {
        val days = setOf(today, today - 1, today - 2, today - 3, today - 4, 10L)
        val current = StreakEngine.currentStreak(days, today)
        assertEquals(5, current)
        assertEquals(current, StreakEngine.longestStreak(days))
    }

    @Test
    fun `heatmap intensity buckets by word count and never drops to zero`() {
        assertEquals(1, StreakEngine.intensity(0))
        assertEquals(1, StreakEngine.intensity(49))
        assertEquals(2, StreakEngine.intensity(50))
        assertEquals(3, StreakEngine.intensity(150))
        assertEquals(4, StreakEngine.intensity(400))
        assertEquals(4, StreakEngine.intensity(5_000))
    }

    @Test
    fun `heatmap counts map each written day to its bucket`() {
        val counts = StreakEngine.heatmapCounts(mapOf(1L to 10, 2L to 200, 3L to 999))
        assertEquals(mapOf(1L to 1, 2L to 3, 3L to 4), counts)
    }

    @Test
    fun `word count ignores blank runs and markdown whitespace`() {
        assertEquals(0, StreakEngine.wordCount(""))
        assertEquals(0, StreakEngine.wordCount("  \n\n  "))
        assertEquals(5, StreakEngine.wordCount("# Title\n\nsome **bold**  words\n"))
    }

    @Test
    fun `intensity never exceeds the pinned maximum`() {
        for (words in listOf(0, 49, 50, 149, 150, 399, 400, 100_000)) {
            val level = StreakEngine.intensity(words)
            assertEquals(true, level in 1..StreakEngine.MAX_INTENSITY)
        }
    }
}
