package io.github.meko123456.dghiuri.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MoodTest {

    @Test
    fun `range is one to five`() {
        assertEquals(1..5, Mood.range)
    }

    @Test
    fun `emoji for each mood`() {
        assertEquals("😞", moodEmoji(1))
        assertEquals("😕", moodEmoji(2))
        assertEquals("😐", moodEmoji(3))
        assertEquals("🙂", moodEmoji(4))
        assertEquals("😄", moodEmoji(5))
    }

    @Test
    fun `label for each mood`() {
        assertEquals("Awful", moodLabel(1))
        assertEquals("Meh", moodLabel(2))
        assertEquals("Okay", moodLabel(3))
        assertEquals("Good", moodLabel(4))
        assertEquals("Great", moodLabel(5))
    }

    @Test
    fun `null mood has empty emoji and a no-mood label`() {
        assertEquals("", moodEmoji(null))
        assertEquals("No mood", moodLabel(null))
    }

    @Test
    fun `out of range values are treated as no mood`() {
        assertEquals("", moodEmoji(0))
        assertEquals("", moodEmoji(6))
        assertEquals("No mood", moodLabel(0))
        assertEquals("No mood", moodLabel(6))
    }

    @Test
    fun `top-level helpers delegate to the Mood object`() {
        for (mood in Mood.range) {
            assertEquals(Mood.emoji(mood), moodEmoji(mood))
            assertEquals(Mood.label(mood), moodLabel(mood))
        }
    }
}
