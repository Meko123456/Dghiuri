package io.github.meko123456.dghiuri.domain

import java.text.NumberFormat
import java.util.Locale

/**
 * "1 entry" / "3 entries" / "1,204 words" — English words, like the rest of the UI, with the digits
 * grouped as [locale]'s language groups them. A journal's word count is in the thousands within
 * weeks, and "12345 words" had to be counted out.
 *
 * In `domain` rather than in the home screen because the markdown export needs the same rule and
 * was quietly not using it: a one-entry export read "_1 entries_". A counted noun should not be
 * spelled two different ways depending on which file wrote it.
 */
internal fun plural(count: Int, one: String, many: String, locale: Locale): String =
    "${grouped(count, locale)} ${if (count == 1) one else many}"

/** [count] with its digits grouped as [locale]'s language groups them: "1,204", "1 204". */
internal fun grouped(count: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(count)
