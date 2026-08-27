package io.github.meko123456.dghiuri.domain

import io.github.meko123456.markdown.Markdown
import io.github.meko123456.markdown.MdBlock
import io.github.meko123456.markdown.MdSpan

/**
 * Turns an entry's raw markdown into the one-line title and snippet shown in lists.
 *
 * Pure and JVM-only: it leans on [Markdown.parse] for structure and just flattens the
 * result to text, so a `**bold**` marker or a link destination never leaks into the UI.
 */
object EntryPreview {

    const val UNTITLED = "Untitled"

    private const val TITLE_MAX_CHARS = 60
    private const val ELLIPSIS = "…"
    private const val SEPARATOR = " · "
    private val whitespace = Regex("\\s+")

    /**
     * The entry's title: the first heading if the document opens with one, otherwise the text
     * of the first block that has any. Trimmed, whitespace-collapsed and capped at 60 chars
     * (ellipsis included). [UNTITLED] when the entry has no visible text at all.
     */
    fun title(markdown: String): String {
        val blocks = Markdown.parse(markdown)
        val index = titleIndex(blocks) ?: return UNTITLED
        return cap(collapse(plainText(blocks[index])), TITLE_MAX_CHARS)
    }

    /**
     * Everything else, flattened: the plain text of each block joined with " · ", capped at
     * [maxChars] (ellipsis included). When the title came from a heading that block is left
     * out — it is already on screen as the title — but a paragraph that doubled as the title
     * stays, since the snippet is where the rest of that sentence lives. Empty string when
     * there is nothing to show.
     */
    fun snippet(markdown: String, maxChars: Int = 120): String {
        val blocks = Markdown.parse(markdown)
        val skip = titleIndex(blocks)?.takeIf { blocks[it] is MdBlock.Heading }
        val text = blocks
            .asSequence()
            .filterIndexed { index, _ -> index != skip }
            .map { collapse(plainText(it)) }
            .filter { it.isNotEmpty() }
            .joinToString(SEPARATOR)
        return cap(text, maxChars)
    }

    /** Visible text of a run of inline spans; a link contributes its label, not its URL. */
    fun plainText(spans: List<MdSpan>): String = buildString {
        for (span in spans) {
            append(
                when (span) {
                    is MdSpan.Text -> span.text
                    is MdSpan.Bold -> span.text
                    is MdSpan.Italic -> span.text
                    is MdSpan.Code -> span.text
                    is MdSpan.Strikethrough -> span.text
                    is MdSpan.Link -> span.text
                },
            )
        }
    }

    /** Visible text of one block; dividers have none. */
    fun plainText(block: MdBlock): String = when (block) {
        is MdBlock.Heading -> plainText(block.spans)
        is MdBlock.Paragraph -> plainText(block.spans)
        is MdBlock.BulletItem -> plainText(block.spans)
        is MdBlock.OrderedItem -> plainText(block.spans)
        is MdBlock.Quote -> plainText(block.spans)
        is MdBlock.CodeBlock -> block.code
        MdBlock.Divider -> ""
    }

    /**
     * Index of the block the title is taken from: the first one with any visible text.
     * When the document opens with a heading, that is the heading; a leading divider or
     * empty code fence is skipped over rather than producing an empty title.
     */
    private fun titleIndex(blocks: List<MdBlock>): Int? =
        blocks.indexOfFirst { collapse(plainText(it)).isNotEmpty() }.takeIf { it >= 0 }

    private fun collapse(text: String): String = text.replace(whitespace, " ").trim()

    private fun cap(text: String, maxChars: Int): String =
        if (text.length <= maxChars) {
            text
        } else {
            text.take((maxChars - 1).coerceAtLeast(0)).trimEnd() + ELLIPSIS
        }
}
