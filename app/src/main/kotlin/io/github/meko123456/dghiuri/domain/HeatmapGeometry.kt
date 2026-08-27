package io.github.meko123456.dghiuri.domain

import java.time.LocalDate

/**
 * Pure layout math for the contribution heatmap, mirroring the drawing code in
 * `io.github.meko123456.heatmap.ContributionHeatmap` so taps can be mapped back to days.
 *
 * The grid has one column per week (oldest on the left) and seven rows, Sunday (row 0) to
 * Saturday (row 6). Each cell occupies a square "step" box of `width / weeks` px; the painted
 * square is smaller (82 % of the step), but hit-testing uses the whole step box so a finger
 * landing in the gutter still selects the nearest day.
 *
 * All days are [LocalDate.toEpochDay] values.
 */
object HeatmapGeometry {

    /** Number of rows in the grid: one per day of the week. */
    const val ROWS: Int = 7

    /** Row of [day] in the grid: Sunday = 0, Monday = 1 … Saturday = 6. */
    fun rowOf(day: Long): Int = LocalDate.ofEpochDay(day).dayOfWeek.value % ROWS

    /** Epoch day drawn in the top-left cell (column 0, row 0) for a grid ending at [endDay]. */
    fun firstDay(endDay: Long, weeks: Int): Long =
        endDay - rowOf(endDay) - (weeks - 1) * 7L

    /**
     * Epoch day whose step box contains the point ([x], [y]), or null when the point is
     * outside `[0, width) × [0, height)` or lands on a cell after [endDay] (which the heatmap
     * leaves undrawn). [width] is the composable's width in px; its height is
     * `width * 7 / weeks`.
     */
    fun dayAt(x: Float, y: Float, width: Float, weeks: Int, endDay: Long): Long? {
        if (weeks < 1 || width <= 0f) return null
        val height = width * ROWS / weeks
        if (x < 0f || y < 0f || x >= width || y >= height) return null
        val step = width / weeks
        // coerceIn guards against float rounding pushing a point on the far edge one past the grid.
        val col = (x / step).toInt().coerceIn(0, weeks - 1)
        val row = (y / step).toInt().coerceIn(0, ROWS - 1)
        val day = firstDay(endDay, weeks) + col * 7L + row
        return if (day > endDay) null else day
    }

    /** Number of days actually drawn in a grid ending at [endDay]: full weeks plus the end week so far. */
    fun daysShown(endDay: Long, weeks: Int): Int =
        (endDay - firstDay(endDay, weeks) + 1).toInt()

    /** How many of [writtenDays] fall inside the drawn range of a grid ending at [endDay]. */
    fun daysWritten(writtenDays: Collection<Long>, endDay: Long, weeks: Int): Int {
        val first = firstDay(endDay, weeks)
        return writtenDays.count { it in first..endDay }
    }

    /**
     * How many week columns fit in [widthPx] when every step must be at least [minStepPx] wide.
     * Never less than 1 so the heatmap always has something to draw.
     */
    fun weeksThatFit(widthPx: Float, minStepPx: Float): Int {
        if (minStepPx <= 0f || widthPx.isNaN() || minStepPx.isNaN()) return 1
        return (widthPx / minStepPx).toInt().coerceAtLeast(1)
    }
}
