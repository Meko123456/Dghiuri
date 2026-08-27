package io.github.meko123456.dghiuri.domain

import io.github.meko123456.dghiuri.data.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MarkdownExportTest {

    private fun day(year: Int, month: Int, dayOfMonth: Int): Long =
        LocalDate.of(year, month, dayOfMonth).toEpochDay()

    private fun entry(day: Long, markdown: String, mood: Int? = null) =
        Entry(epochDay = day, markdown = markdown, mood = mood, updatedAt = 0L)

    @Test
    fun `empty list renders only the header`() {
        assertEquals("# Dghiuri export\n", MarkdownExport.render(emptyList()))
    }

    @Test
    fun `single entry renders the exact document`() {
        val entries = listOf(entry(day(2026, 8, 27), "Went for a walk.", mood = 4))
        val expected = """
            |# Dghiuri export
            |
            |_1 entries, 2026-08-27 → 2026-08-27_
            |
            |## 2026-08-27 · 🙂 Good
            |
            |Went for a walk.
            |
        """.trimMargin()
        assertEquals(expected, MarkdownExport.render(entries))
    }

    @Test
    fun `entries are emitted in ascending date order regardless of input order`() {
        val entries = listOf(
            entry(day(2026, 8, 27), "third"),
            entry(day(2026, 8, 1), "first"),
            entry(day(2026, 8, 14), "second"),
        )
        val out = MarkdownExport.render(entries)
        val first = out.indexOf("## 2026-08-01")
        val second = out.indexOf("## 2026-08-14")
        val third = out.indexOf("## 2026-08-27")
        assertTrue(first in 0 until second)
        assertTrue(second < third)
        assertTrue(out.contains("_3 entries, 2026-08-01 → 2026-08-27_"))
    }

    @Test
    fun `heading has no mood suffix when mood is null`() {
        val out = MarkdownExport.render(listOf(entry(day(2026, 8, 27), "body")))
        assertTrue(out.contains("## 2026-08-27\n\nbody\n"))
        assertFalse(out.contains("·"))
    }

    @Test
    fun `body is trimmed and range line spans first to last day`() {
        val entries = listOf(
            entry(day(2026, 8, 3), "\n\n  padded body  \n\n\n", mood = 1),
            entry(day(2026, 7, 30), "  early  "),
        )
        val expected = """
            |# Dghiuri export
            |
            |_2 entries, 2026-07-30 → 2026-08-03_
            |
            |## 2026-07-30
            |
            |early
            |
            |## 2026-08-03 · 😞 Awful
            |
            |padded body
            |
        """.trimMargin()
        assertEquals(expected, MarkdownExport.render(entries))
    }

    @Test
    fun `output always ends with exactly one newline`() {
        val outputs = listOf(
            MarkdownExport.render(emptyList()),
            MarkdownExport.render(listOf(entry(day(2026, 8, 27), "a"))),
            MarkdownExport.render(listOf(entry(day(2026, 8, 27), "a\n\n\n"))),
        )
        for (out in outputs) {
            assertTrue(out.endsWith("\n"))
            assertFalse(out.endsWith("\n\n"))
        }
    }

    @Test
    fun `rendering is deterministic`() {
        val entries = listOf(
            entry(day(2026, 8, 27), "x", mood = 5),
            entry(day(2026, 8, 26), "y"),
        )
        assertEquals(MarkdownExport.render(entries), MarkdownExport.render(entries))
    }
}
