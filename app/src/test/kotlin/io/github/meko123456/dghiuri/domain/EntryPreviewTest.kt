package io.github.meko123456.dghiuri.domain

import io.github.meko123456.markdown.MdSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryPreviewTest {

    @Test
    fun `title is the heading when the entry starts with one`() {
        assertEquals("Morning run", EntryPreview.title("# Morning run\n\nFelt great afterwards."))
    }

    @Test
    fun `title strips inline markup from the heading`() {
        assertEquals("Big day with code", EntryPreview.title("# **Big** day with `code`"))
    }

    @Test
    fun `title falls back to the first line when there is no heading`() {
        assertEquals("Felt great today", EntryPreview.title("Felt great today\n\nRan, read, slept."))
    }

    @Test
    fun `title skips a leading divider and takes the first block with text`() {
        assertEquals("hello", EntryPreview.title("---\n\nhello"))
    }

    @Test
    fun `title is capped at sixty chars including the ellipsis`() {
        val title = EntryPreview.title("a".repeat(80))
        assertEquals(60, title.length)
        assertTrue(title.endsWith("…"))
        assertEquals("a".repeat(59) + "…", title)
    }

    @Test
    fun `title exactly sixty chars is not truncated`() {
        val sixty = "b".repeat(60)
        assertEquals(sixty, EntryPreview.title(sixty))
    }

    @Test
    fun `empty or blank entries are Untitled with an empty snippet`() {
        for (markdown in listOf("", "   ", "\n\n  \n", "---", "```\n```")) {
            assertEquals(markdown, "Untitled", EntryPreview.title(markdown))
            assertEquals(markdown, "", EntryPreview.snippet(markdown))
        }
    }

    @Test
    fun `snippet strips bold, link and code markers`() {
        val md = "# Note\n\nSome **bold** and [a link](https://example.com) and `code` here."
        assertEquals("Some bold and a link and code here.", EntryPreview.snippet(md))
    }

    @Test
    fun `snippet skips the heading that became the title`() {
        val md = "# Title\n\nBody one\n\nBody two"
        assertEquals("Title", EntryPreview.title(md))
        assertEquals("Body one · Body two", EntryPreview.snippet(md))
    }

    @Test
    fun `snippet keeps a first paragraph even when it was used as the title`() {
        val md = "Body one\n\nBody two"
        assertEquals("Body one", EntryPreview.title(md))
        assertEquals("Body one · Body two", EntryPreview.snippet(md))
    }

    @Test
    fun `snippet includes code block text`() {
        val md = "# T\n\n```kotlin\nval x = 1\nval y = 2\n```"
        assertEquals("val x = 1 val y = 2", EntryPreview.snippet(md))
    }

    @Test
    fun `snippet joins list items and quotes with the separator`() {
        val md = "# T\n- one\n1. two\n> three"
        assertEquals("one · two · three", EntryPreview.snippet(md))
    }

    @Test
    fun `snippet drops dividers and empty blocks instead of leaving empty segments`() {
        val md = "# T\n\nA\n\n---\n\nB\n\n```\n```"
        assertEquals("A · B", EntryPreview.snippet(md))
    }

    @Test
    fun `snippet collapses runs of whitespace`() {
        val md = "# T\n\nline   with\ttabs\nand a newline"
        assertEquals("line with tabs and a newline", EntryPreview.snippet(md))
    }

    @Test
    fun `snippet honours maxChars with an ellipsis`() {
        val md = "# T\n\n" + "word ".repeat(30)
        val snippet = EntryPreview.snippet(md, maxChars = 20)
        assertEquals(20, snippet.length)
        assertEquals("word word word word…", snippet)
    }

    @Test
    fun `plainText uses a link's label and not its destination`() {
        val spans = listOf(
            MdSpan.Text("see "),
            MdSpan.Link("Kotlin", "https://kotlinlang.org"),
            MdSpan.Text(" and "),
            MdSpan.Strikethrough("Java"),
        )
        assertEquals("see Kotlin and Java", EntryPreview.plainText(spans))
    }
}
