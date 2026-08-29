package io.github.meko123456.dghiuri.ui.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.meko123456.dghiuri.domain.StreakEngine
import io.github.meko123456.heatmap.ContributionHeatmap
import io.github.meko123456.heatmap.GithubGreens
import io.github.meko123456.heatmap.GithubLightGreens
import io.github.meko123456.heatmap.HeatmapLayout

/**
 * The journaling [ContributionHeatmap]: tap-to-select via the library's `onDayClick`, with a
 * single semantics node summarising the picture (how many of the shown days were written).
 * The surrounding card offers a date picker as the accessible way to open a day.
 *
 * @param counts intensity per day (1..[StreakEngine.MAX_INTENSITY]), keyed by
 *   [java.time.LocalDate.toEpochDay]; `maxCount` fixes the scale so the buckets render absolutely
 * @param endDay last day drawn, normally today
 * @param onDayTap called with the epoch day under the finger
 * @param weeks number of week columns to show
 */
@Composable
fun TappableHeatmap(
    counts: Map<Long, Int>,
    endDay: Long,
    onDayTap: (Long) -> Unit,
    modifier: Modifier = Modifier,
    weeks: Int = 26,
) {
    val description = remember(counts, endDay, weeks) {
        val shown = HeatmapLayout.daysShown(endDay, weeks)
        val written = HeatmapLayout.daysWithin(counts.keys, endDay, weeks)
        "Journaling heatmap: $written of the last ${plural(shown, "day", "days")} written"
    }
    ContributionHeatmap(
        counts = counts,
        endDay = endDay,
        modifier = modifier,
        weeks = weeks,
        maxCount = StreakEngine.MAX_INTENSITY,
        levelColors = if (isSystemInDarkTheme()) GithubGreens else GithubLightGreens,
        onDayClick = onDayTap,
        contentDescription = description,
    )
}
