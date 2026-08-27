package io.github.meko123456.dghiuri.domain

import io.github.meko123456.dghiuri.data.Entry
import java.time.LocalDate

/**
 * Renders every entry into one markdown document for sharing or backup.
 *
 * Layout:
 * ```
 * # Dghiuri export
 *
 * _N entries, YYYY-MM-DD → YYYY-MM-DD_
 *
 * ## YYYY-MM-DD · 🙂 Good
 *
 * body
 *
 * ```
 * Entries are emitted in ascending date order regardless of input order, the body is trimmed, and
 * the document always ends with exactly one newline.
 */
object MarkdownExport {

    fun render(entries: List<Entry>): String {
        val sorted = entries.sortedBy { it.epochDay }
        val sb = StringBuilder()
        sb.append("# Dghiuri export\n\n")

        if (sorted.isNotEmpty()) {
            val first = dateOf(sorted.first())
            val last = dateOf(sorted.last())
            sb.append("_${sorted.size} entries, ${first} → ${last}_\n\n")
        }

        for (entry in sorted) {
            sb.append("## ").append(dateOf(entry))
            entry.mood?.let { mood ->
                if (mood in Mood.range) {
                    sb.append(" · ").append(Mood.emoji(mood)).append(' ').append(Mood.label(mood))
                }
            }
            sb.append("\n\n")
            sb.append(entry.markdown.trim())
            sb.append("\n\n")
        }

        return sb.toString().trimEnd('\n') + "\n"
    }

    private fun dateOf(entry: Entry): String = LocalDate.ofEpochDay(entry.epochDay).toString()
}
