package io.github.meko123456.dghiuri.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HeatmapGeometryTest {

    /** A Thursday, so the last column has rows 0..4 drawn and rows 5..6 empty. */
    private val thursday = LocalDate.of(2026, 8, 27)
    private val endDay = thursday.toEpochDay()

    /** 20 columns at 10 px per step; the grid is 200 × 70 px. */
    private val weeks = 20
    private val width = 200f
    private val step = width / weeks

    @Test
    fun `end day of 2026-08-27 sits in row four`() {
        assertEquals(DayOfWeek.THURSDAY, thursday.dayOfWeek)
        assertEquals(4, HeatmapGeometry.rowOf(endDay))
    }

    @Test
    fun `first day for a single week is the Sunday starting that week`() {
        val first = HeatmapGeometry.firstDay(endDay, weeks = 1)
        val expected = thursday.minusDays(4)
        assertEquals(expected.toEpochDay(), first)
        assertEquals(DayOfWeek.SUNDAY, LocalDate.ofEpochDay(first).dayOfWeek)
    }

    @Test
    fun `first day for twenty weeks is the Sunday nineteen weeks before the end week`() {
        val first = HeatmapGeometry.firstDay(endDay, weeks)
        val expected = thursday.minusDays(4).minusWeeks(19)
        assertEquals(expected.toEpochDay(), first)
        assertEquals(DayOfWeek.SUNDAY, LocalDate.ofEpochDay(first).dayOfWeek)
        assertEquals(19 * 7L + 4, endDay - first)
    }

    @Test
    fun `tap in the bottom-right drawn cell returns the end day`() {
        val x = 19 * step + step / 2
        val y = 4 * step + step / 2
        assertEquals(endDay, HeatmapGeometry.dayAt(x, y, width, weeks, endDay))
    }

    @Test
    fun `tap in the top-left cell returns the first day`() {
        val first = HeatmapGeometry.firstDay(endDay, weeks)
        assertEquals(first, HeatmapGeometry.dayAt(step / 2, step / 2, width, weeks, endDay))
    }

    @Test
    fun `tap on an undrawn cell after the end day returns null`() {
        val x = 19 * step + step / 2
        assertNull(HeatmapGeometry.dayAt(x, 5 * step + step / 2, width, weeks, endDay))
        assertNull(HeatmapGeometry.dayAt(x, 6 * step + step / 2, width, weeks, endDay))
    }

    @Test
    fun `tap in the last column above the end day returns that week's earlier day`() {
        val x = 19 * step + step / 2
        val sunday = thursday.minusDays(4).toEpochDay()
        assertEquals(sunday, HeatmapGeometry.dayAt(x, step / 2, width, weeks, endDay))
        assertEquals(endDay - 1, HeatmapGeometry.dayAt(x, 3 * step + step / 2, width, weeks, endDay))
    }

    @Test
    fun `points outside the grid return null`() {
        val height = width * 7 / weeks
        assertNull(HeatmapGeometry.dayAt(-1f, 5f, width, weeks, endDay))
        assertNull(HeatmapGeometry.dayAt(5f, -1f, width, weeks, endDay))
        assertNull(HeatmapGeometry.dayAt(width, 5f, width, weeks, endDay))
        assertNull(HeatmapGeometry.dayAt(width + 50f, 5f, width, weeks, endDay))
        assertNull(HeatmapGeometry.dayAt(5f, height, width, weeks, endDay))
        assertNull(HeatmapGeometry.dayAt(5f, height + 50f, width, weeks, endDay))
    }

    @Test
    fun `a point exactly on a cell's top-left corner belongs to that cell`() {
        val first = HeatmapGeometry.firstDay(endDay, weeks)
        assertEquals(first, HeatmapGeometry.dayAt(0f, 0f, width, weeks, endDay))
        assertEquals(endDay, HeatmapGeometry.dayAt(19 * step, 4 * step, width, weeks, endDay))
        // One px before the corner is the neighbouring cell.
        assertEquals(endDay - 1, HeatmapGeometry.dayAt(19 * step, 4 * step - 1f, width, weeks, endDay))
        assertEquals(endDay - 7, HeatmapGeometry.dayAt(19 * step - 1f, 4 * step, width, weeks, endDay))
    }

    @Test
    fun `gutter between painted squares still resolves to the nearest cell`() {
        // The painted square is 82 % of the step; a tap in the remaining 18 % is still that cell.
        val x = 19 * step + step * 0.95f
        val y = 4 * step + step * 0.95f
        assertEquals(endDay, HeatmapGeometry.dayAt(x, y, width, weeks, endDay))
    }

    @Test
    fun `Sunday end day sits in row zero and the rest of its column is undrawn`() {
        val sunday = LocalDate.of(2026, 8, 30)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        val sundayEnd = sunday.toEpochDay()
        assertEquals(0, HeatmapGeometry.rowOf(sundayEnd))
        assertEquals(sundayEnd, HeatmapGeometry.firstDay(sundayEnd, weeks = 1))

        val fourWeeks = 4
        val w = 40f
        val s = w / fourWeeks
        assertEquals(sundayEnd, HeatmapGeometry.dayAt(3 * s + s / 2, s / 2, w, fourWeeks, sundayEnd))
        assertNull(HeatmapGeometry.dayAt(3 * s + s / 2, s + s / 2, w, fourWeeks, sundayEnd))
        assertNull(HeatmapGeometry.dayAt(3 * s + s / 2, 6 * s + s / 2, w, fourWeeks, sundayEnd))
    }

    @Test
    fun `every drawn cell round-trips through its centre`() {
        val first = HeatmapGeometry.firstDay(endDay, weeks)
        for (col in 0 until weeks) {
            for (row in 0 until 7) {
                val day = first + col * 7L + row
                val hit = HeatmapGeometry.dayAt(col * step + step / 2, row * step + step / 2, width, weeks, endDay)
                if (day > endDay) assertNull(hit) else assertEquals(day, hit)
            }
        }
    }

    @Test
    fun `dayAt rejects degenerate inputs`() {
        assertNull(HeatmapGeometry.dayAt(5f, 5f, 0f, weeks, endDay))
        assertNull(HeatmapGeometry.dayAt(5f, 5f, width, 0, endDay))
    }

    @Test
    fun `weeksThatFit divides width by the minimum step`() {
        assertEquals(26, HeatmapGeometry.weeksThatFit(260f, 10f))
        assertEquals(26, HeatmapGeometry.weeksThatFit(269f, 10f))
        assertEquals(27, HeatmapGeometry.weeksThatFit(270f, 10f))
    }

    @Test
    fun `weeksThatFit never returns less than one`() {
        assertEquals(1, HeatmapGeometry.weeksThatFit(5f, 10f))
        assertEquals(1, HeatmapGeometry.weeksThatFit(0f, 10f))
        assertEquals(1, HeatmapGeometry.weeksThatFit(100f, 0f))
    }

    @Test
    fun `daysShown counts full weeks plus the end week so far`() {
        // Thursday end: 19 full weeks plus Sunday..Thursday of the last one.
        assertEquals(19 * 7 + 5, HeatmapGeometry.daysShown(endDay, weeks))
        assertEquals(5, HeatmapGeometry.daysShown(endDay, weeks = 1))
        val sundayEnd = LocalDate.of(2026, 8, 30).toEpochDay()
        assertEquals(1, HeatmapGeometry.daysShown(sundayEnd, weeks = 1))
        assertEquals(7 * 3 + 1, HeatmapGeometry.daysShown(sundayEnd, weeks = 4))
    }

    @Test
    fun `daysWritten only counts days inside the drawn range`() {
        val first = HeatmapGeometry.firstDay(endDay, weeks)
        val written = setOf(first - 1, first, endDay - 3, endDay, endDay + 1)
        assertEquals(3, HeatmapGeometry.daysWritten(written, endDay, weeks))
        assertEquals(0, HeatmapGeometry.daysWritten(emptySet(), endDay, weeks))
    }
}
