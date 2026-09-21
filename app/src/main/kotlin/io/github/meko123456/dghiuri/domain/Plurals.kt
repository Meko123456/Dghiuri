package io.github.meko123456.dghiuri.domain

/**
 * "1 entry" / "3 entries" — English-only, like the rest of the UI.
 *
 * In `domain` rather than in the home screen because the markdown export needs the same rule and
 * was quietly not using it: a one-entry export read "_1 entries_". A counted noun should not be
 * spelled two different ways depending on which file wrote it.
 */
internal fun plural(count: Int, one: String, many: String): String =
    "$count ${if (count == 1) one else many}"
