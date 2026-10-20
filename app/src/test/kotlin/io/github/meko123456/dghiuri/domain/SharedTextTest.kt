package io.github.meko123456.dghiuri.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedTextTest {

    @Test
    fun `a page shared from a browser becomes a link titled with the page`() {
        assertEquals(
            "[Tbilisi - Wikipedia](https://en.wikipedia.org/wiki/Tbilisi)",
            SharedText.toMarkdown("https://en.wikipedia.org/wiki/Tbilisi", "Tbilisi - Wikipedia"),
        )
    }

    @Test
    fun `a link with no title, or the link as its title, stays the bare address`() {
        assertEquals("https://example.com/a", SharedText.toMarkdown(" https://example.com/a\n", null))
        assertEquals("https://example.com/a", SharedText.toMarkdown("https://example.com/a", "https://example.com/a"))
    }

    @Test
    fun `brackets in a title don't end the label early`() {
        assertEquals(
            "[Notes (draft)](https://example.com/n)",
            SharedText.toMarkdown("https://example.com/n", "Notes [draft]"),
        )
    }

    @Test
    fun `anything else is quoted line by line`() {
        assertEquals(
            "> The best time to plant a tree\n> was twenty years ago.\n>\n> — proverb",
            SharedText.toMarkdown("The best time to plant a tree\r\nwas twenty years ago.  \r\n\r\n— proverb", "ignored"),
        )
    }

    @Test
    fun `shared text goes after the entry with room to write below`() {
        assertEquals("> quote\n\n", SharedText.appendTo("", "> quote"))
        assertEquals("> quote\n\n", SharedText.appendTo(" \n", "> quote"))
        assertEquals("Morning run.\n\n> quote\n\n", SharedText.appendTo("Morning run.\n", "> quote"))
    }
}
