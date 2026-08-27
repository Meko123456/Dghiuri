package io.github.meko123456.dghiuri.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One journal entry per calendar day.
 *
 * @param epochDay the day, as [java.time.LocalDate.toEpochDay] — the natural key, so a day can
 *   only ever have one entry
 * @param markdown the raw markdown the user typed
 * @param mood 1..5, or null if the user hasn't picked one
 * @param updatedAt epoch millis of the last edit
 */
@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey val epochDay: Long,
    val markdown: String,
    val mood: Int? = null,
    val updatedAt: Long,
)
