package io.github.meko123456.dghiuri.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.meko123456.dghiuri.data.Entry
import io.github.meko123456.dghiuri.domain.EntryPreview
import io.github.meko123456.dghiuri.domain.EntryStats
import io.github.meko123456.dghiuri.domain.Mood
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dayMonthFormat = DateTimeFormatter.ofPattern("d MMM")
private val dayMonthYearFormat = DateTimeFormatter.ofPattern("d MMM yyyy")
private val weekdayFormat = DateTimeFormatter.ofPattern("EEE")

/** "27 Aug" for days in the current year, "27 Aug 2025" otherwise. */
internal fun formatDayMonth(epochDay: Long, today: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    val sameYear = date.year == LocalDate.ofEpochDay(today).year
    return date.format(if (sameYear) dayMonthFormat else dayMonthYearFormat)
}

/** "Today", "Yesterday", or the short weekday name ("Thu"). */
internal fun formatWeekday(epochDay: Long, today: Long): String = when (epochDay) {
    today -> "Today"
    today - 1 -> "Yesterday"
    else -> LocalDate.ofEpochDay(epochDay).format(weekdayFormat)
}

/** "1 entry" / "3 entries" — English-only, like the rest of the UI. */
internal fun plural(count: Int, one: String, many: String): String =
    "$count ${if (count == 1) one else many}"

/** The contribution heatmap in a card, with a caption row underneath. */
@Composable
internal fun HeatmapCard(
    counts: Map<Long, Int>,
    today: Long,
    weeks: Int,
    onDayTap: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TappableHeatmap(counts = counts, endDay = today, onDayTap = onDayTap, weeks = weeks)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Last $weeks weeks",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Tap a day to open it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Four stat tiles plus a one-line footer with word count and this month's entries. */
@Composable
internal fun StatsCard(stats: EntryStats, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatTile(
                    value = "🔥 ${stats.currentStreak}",
                    label = "streak",
                    description = "Current streak ${plural(stats.currentStreak, "day", "days")}",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = "🏆 ${stats.longestStreak}",
                    label = "best",
                    description = "Longest streak ${plural(stats.longestStreak, "day", "days")}",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = stats.total.toString(),
                    label = "entries",
                    description = "${plural(stats.total, "entry", "entries")} in total",
                    modifier = Modifier.weight(1f),
                )
                val averageMood = stats.averageMood?.roundToInt()?.coerceIn(Mood.range)
                StatTile(
                    value = if (averageMood == null) "—" else Mood.emoji(averageMood),
                    label = "avg mood",
                    description = if (averageMood == null) {
                        "Average mood not set"
                    } else {
                        "Average mood ${Mood.label(averageMood)}"
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "${plural(stats.totalWords, "word", "words")} · " +
                    "${plural(stats.writtenThisMonth, "entry", "entries")} this month",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
            )
        }
    }
}

/**
 * One stat. The whole tile is announced as [description] so TalkBack says "Current streak 3 days"
 * instead of reading a fire emoji and a number.
 */
@Composable
private fun StatTile(
    value: String,
    label: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * Today's entry as a tappable card, or a call to action when nothing has been written yet.
 * Tinted with the secondary container so it stands out from the archive below.
 */
@Composable
internal fun TodayCard(
    entry: Entry?,
    onOpenToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    if (entry == null) {
        Card(modifier = modifier.fillMaxWidth(), colors = colors) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = "Today", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "Nothing written today yet",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "A few lines are enough to keep the streak alive.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = onOpenToday,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Write today")
                }
            }
        }
    } else {
        val title = remember(entry.markdown) { EntryPreview.title(entry.markdown) }
        val snippet = remember(entry.markdown) { EntryPreview.snippet(entry.markdown) }
        Card(onClick = onOpenToday, modifier = modifier.fillMaxWidth(), colors = colors) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    MoodEmoji(entry.mood)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (snippet.isNotEmpty()) {
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** One archive row: date column, title + one-line snippet, mood emoji. */
@Composable
internal fun EntryRow(
    entry: Entry,
    today: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayMonth = remember(entry.epochDay, today) { formatDayMonth(entry.epochDay, today) }
    val weekday = remember(entry.epochDay, today) { formatWeekday(entry.epochDay, today) }
    val title = remember(entry.markdown) { EntryPreview.title(entry.markdown) }
    val snippet = remember(entry.markdown) { EntryPreview.snippet(entry.markdown) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.widthIn(min = 64.dp)) {
            Text(
                text = dayMonth,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = weekday,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (snippet.isNotEmpty()) {
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (entry.mood != null) {
            Spacer(modifier = Modifier.width(12.dp))
            MoodEmoji(entry.mood)
        }
    }
}

/** Mood glyph that reads as its label ("Mood Good") to screen readers; draws nothing for no mood. */
@Composable
internal fun MoodEmoji(mood: Int?, modifier: Modifier = Modifier) {
    val emoji = Mood.emoji(mood)
    if (emoji.isEmpty()) return
    val label = Mood.label(mood)
    Text(
        text = emoji,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.clearAndSetSemantics { contentDescription = "Mood $label" },
    )
}

/** Shown under the (empty) heatmap when the diary has no entries at all. */
@Composable
internal fun EmptyState(onWriteToday: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "📔",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(
            text = "Your diary is empty",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Write your first entry and the heatmap above starts filling in, " +
                "one square per day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onWriteToday) {
            Text("Write today")
        }
    }
}

@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
internal fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The search box revealed under the top bar; grabs focus when it appears. */
@Composable
internal fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .focusRequester(focusRequester),
        placeholder = { Text("Search entries") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close search")
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = MaterialTheme.shapes.extraLarge,
    )
}
