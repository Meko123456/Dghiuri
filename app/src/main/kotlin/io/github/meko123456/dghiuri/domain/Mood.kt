package io.github.meko123456.dghiuri.domain

/** The 1..5 mood scale and its presentation. Anything outside [range] (or null) is "no mood". */
object Mood {

    val range: IntRange = 1..5

    private val emojis = listOf("😞", "😕", "😐", "🙂", "😄")
    private val labels = listOf("Awful", "Meh", "Okay", "Good", "Great")

    fun emoji(mood: Int?): String =
        if (mood != null && mood in range) emojis[mood - range.first] else ""

    fun label(mood: Int?): String =
        if (mood != null && mood in range) labels[mood - range.first] else "No mood"
}

fun moodEmoji(mood: Int?): String = Mood.emoji(mood)

fun moodLabel(mood: Int?): String = Mood.label(mood)
