package io.github.meko123456.dghiuri.domain

/**
 * Pure streak arithmetic over the set of days that have an entry. No Android, no time zones —
 * callers pass epoch days ([java.time.LocalDate.toEpochDay]) so this is trivially unit-testable.
 */
object StreakEngine {

    /** Highest heatmap intensity; see [intensity]. */
    const val MAX_INTENSITY: Int = 4

    private val whitespace = Regex("\\s+")

    /**
     * Consecutive written days ending today or yesterday. A streak is still "alive" if today has
     * no entry yet but yesterday does — the user hasn't missed a day until midnight passes.
     */
    fun currentStreak(writtenDays: Set<Long>, today: Long): Int {
        var day = if (today in writtenDays) today else today - 1
        var streak = 0
        while (day in writtenDays) {
            streak++
            day--
        }
        return streak
    }

    /** Longest run of consecutive written days anywhere in history. */
    fun longestStreak(writtenDays: Set<Long>): Int {
        if (writtenDays.isEmpty()) return 0
        val sorted = writtenDays.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /**
     * Heatmap intensity per day. A written day is never zero; longer entries get a higher
     * count so the heatmap shows effort, not just presence. Buckets by word count:
     * `<50` → 1, `<150` → 2, `<400` → 3, else 4.
     */
    fun heatmapCounts(wordCountsByDay: Map<Long, Int>): Map<Long, Int> =
        wordCountsByDay.mapValues { (_, words) -> intensity(words) }

    fun intensity(words: Int): Int = when {
        words < 50 -> 1
        words < 150 -> 2
        words < 400 -> 3
        else -> MAX_INTENSITY
    }

    /**
     * Pins the colour scale of [heatmapCounts] output so the buckets render absolutely.
     *
     * The heatmap library shades each day relative to the largest value in the map (the busiest
     * day is always the darkest), which would turn a diary of short entries entirely dark. It
     * never draws cells after its end day, so adding [MAX_INTENSITY] at `endDay + 1` fixes the
     * scale at 1..4 without painting anything. Already-present real days are left untouched.
     */
    fun pinIntensityScale(counts: Map<Long, Int>, endDay: Long): Map<Long, Int> {
        val pin = endDay + 1
        if (counts[pin] == MAX_INTENSITY) return counts
        return counts.filterKeys { it <= endDay } + (pin to MAX_INTENSITY)
    }

    /** Word count of a markdown body, ignoring blank runs. */
    fun wordCount(markdown: String): Int =
        markdown.split(whitespace).count { it.isNotBlank() }
}
