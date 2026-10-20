package io.github.meko123456.dghiuri.domain

/** Text shared to Dghiuri from another app, as markdown for the day's entry. */
object SharedText {

    /**
     * A lone link becomes a markdown link titled with the page's title when the sharing app sends
     * one (a browser does), or stays the bare address; anything else is quoted, line by line.
     */
    fun toMarkdown(text: String, subject: String?): String {
        val body = text.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (link.matches(body)) {
            // The renderer has no escapes, so a bracket in the title would end the label early.
            val title = subject?.trim()?.takeIf { it.isNotEmpty() && it != body }
                ?.replace('[', '(')?.replace(']', ')')
            return if (title != null) "[$title]($body)" else body
        }
        return body.lines().joinToString("\n") { line ->
            if (line.isBlank()) ">" else "> ${line.trimEnd()}"
        }
    }

    /** [entry] with [markdown] after it, a blank line apart, and a blank line to write on below. */
    fun appendTo(entry: String, markdown: String): String =
        if (entry.isBlank()) "$markdown\n\n" else "${entry.trimEnd()}\n\n$markdown\n\n"

    private val link = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
}
