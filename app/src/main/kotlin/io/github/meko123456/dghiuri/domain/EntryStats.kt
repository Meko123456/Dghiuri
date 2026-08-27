package io.github.meko123456.dghiuri.domain

import io.github.meko123456.dghiuri.data.Entry
import java.time.LocalDate

/**
 * Aggregate numbers shown on the home screen. Pure: derived from the full entry list and a
 * caller-supplied "today" so it can be unit-tested without a clock.
 *
 * @param averageMood mean of the moods that were set, or null when no entry has one
 * @param writtenThisMonth entries whose day falls in the same year-month as `today`
 */
data class EntryStats(
    val total: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val averageMood: Double?,
    val totalWords: Int,
    val writtenThisMonth: Int,
) {
    companion object {

        val EMPTY = EntryStats(
            total = 0,
            currentStreak = 0,
            longestStreak = 0,
            averageMood = null,
            totalWords = 0,
            writtenThisMonth = 0,
        )

        fun from(entries: List<Entry>, today: Long): EntryStats {
            if (entries.isEmpty()) return EMPTY

            val writtenDays = entries.map { it.epochDay }.toSet()
            val moods = entries.mapNotNull { it.mood }
            val todayDate = LocalDate.ofEpochDay(today)

            return EntryStats(
                total = entries.size,
                currentStreak = StreakEngine.currentStreak(writtenDays, today),
                longestStreak = StreakEngine.longestStreak(writtenDays),
                averageMood = if (moods.isEmpty()) null else moods.average(),
                totalWords = entries.sumOf { StreakEngine.wordCount(it.markdown) },
                writtenThisMonth = entries.count { entry ->
                    val date = LocalDate.ofEpochDay(entry.epochDay)
                    date.year == todayDate.year && date.month == todayDate.month
                },
            )
        }
    }
}
