package io.github.meko123456.dghiuri.ui.markdown

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.meko123456.dghiuri.ui.theme.DghiuriTheme
import io.github.meko123456.markdown.Markdown
import io.github.meko123456.markdown.MdBlock
import io.github.meko123456.markdown.MdSpan

/**
 * Renders parsed markdown natively with Material 3 typography — no WebView, no HTML.
 *
 * Callers own the parse (`Markdown.parse(text)`) so a preview pane can re-render on every
 * keystroke without this composable having to remember anything. Links open through the
 * ambient `LocalUriHandler`, which is what [Text] does with a [LinkAnnotation.Url].
 */
@Composable
fun MarkdownText(blocks: List<MdBlock>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val linkColor = colors.primary
    val codeBackground = colors.surfaceVariant

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = inline(block.spans, linkColor, codeBackground),
                    style = headingStyle(block.level),
                    modifier = Modifier.padding(top = 4.dp),
                )

                is MdBlock.Paragraph -> Text(
                    text = inline(block.spans, linkColor, codeBackground),
                    style = typography.bodyLarge,
                )

                is MdBlock.BulletItem -> ListRow(
                    marker = "•  ",
                    content = inline(block.spans, linkColor, codeBackground),
                )

                is MdBlock.OrderedItem -> ListRow(
                    marker = "${block.number}.  ",
                    content = inline(block.spans, linkColor, codeBackground),
                )

                is MdBlock.Quote -> Surface(
                    color = colors.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = inline(block.spans, linkColor, codeBackground),
                        style = typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                is MdBlock.CodeBlock -> CodeBlock(code = block.code, background = colors.surfaceVariant)

                MdBlock.Divider -> HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

/** Marker column plus wrapping content; shared by bullet and ordered items. */
@Composable
private fun ListRow(marker: String, content: AnnotatedString) {
    val style = MaterialTheme.typography.bodyLarge
    Row(verticalAlignment = Alignment.Top) {
        Text(text = marker, style = style)
        Text(text = content, style = style, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CodeBlock(code: String, background: Color) {
    Surface(
        color = background,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun headingStyle(level: Int): TextStyle = when (level) {
    1 -> MaterialTheme.typography.headlineMedium
    2 -> MaterialTheme.typography.titleLarge
    else -> MaterialTheme.typography.titleMedium
}

/**
 * Maps inline spans to a styled [AnnotatedString].
 *
 * Deliberately not `@Composable`: every colour it needs is a parameter, so it never reads
 * [MaterialTheme] itself and can be called from anywhere (or unit-tested on the JVM).
 */
internal fun inline(spans: List<MdSpan>, linkColor: Color, codeBackground: Color): AnnotatedString =
    buildAnnotatedString {
        for (span in spans) {
            when (span) {
                is MdSpan.Text -> append(span.text)

                is MdSpan.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) }

                is MdSpan.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(span.text) }

                is MdSpan.Code -> withStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground),
                ) { append(span.text) }

                is MdSpan.Strikethrough -> withStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                ) { append(span.text) }

                is MdSpan.Link -> withLink(
                    LinkAnnotation.Url(
                        url = span.destination,
                        styles = TextLinkStyles(
                            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        ),
                    ),
                ) { append(span.text) }
            }
        }
    }

@Preview(showBackground = true)
@Composable
private fun MarkdownTextPreview() {
    val sample = """
        # Tuesday

        Slept **well**, ran 5k, then read [this](https://example.com) with `coffee`.

        ## Later
        - groceries
        - ~~call the bank~~ done
        1. draft the letter
        2. post it

        > Tomorrow is another page.

        ```
        val mood = 4
        ```

        ---
    """.trimIndent()
    DghiuriTheme {
        MarkdownText(blocks = Markdown.parse(sample), modifier = Modifier.padding(16.dp))
    }
}
