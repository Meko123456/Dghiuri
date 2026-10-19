package io.github.meko123456.dghiuri.domain

import java.text.DecimalFormatSymbols
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class PluralTest {

    @Test
    fun `one is singular and the rest are plural`() {
        assertEquals("1 entry", plural(1, "entry", "entries", Locale.US))
        assertEquals("0 entries", plural(0, "entry", "entries", Locale.US))
        assertEquals("2 entries", plural(2, "entry", "entries", Locale.US))
    }

    @Test
    fun `thousands are grouped as the language groups them`() {
        // The stats line read "12345 words · 3 entries this month".
        assertEquals("12,345 words", plural(12_345, "word", "words", Locale.US))
        val space = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ka")).groupingSeparator
        assertEquals("12${space}345 words", plural(12_345, "word", "words", Locale.forLanguageTag("ka")))
        assertEquals("1,204", grouped(1_204, Locale.ROOT))
    }
}
