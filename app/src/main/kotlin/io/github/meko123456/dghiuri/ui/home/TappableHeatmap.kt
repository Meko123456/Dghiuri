package io.github.meko123456.dghiuri.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import io.github.meko123456.dghiuri.domain.HeatmapGeometry
import io.github.meko123456.dghiuri.domain.StreakEngine
import androidx.compose.ui.graphics.Color
import io.github.meko123456.heatmap.ContributionHeatmap
import io.github.meko123456.heatmap.GithubGreens

/**
 * GitHub's light-theme greens, level 1 (lightest) to 4. The library's default [GithubGreens] is
 * the dark-theme palette, whose level 1 is the darkest shade — inverted on a light surface.
 */
private val LightGreens: List<Color> = listOf(
    Color(0xFF9BE9A8),
    Color(0xFF40C463),
    Color(0xFF30A14E),
    Color(0xFF216E39),
)

/**
 * [ContributionHeatmap] that reports which day was tapped.
 *
 * The library draws on a plain Canvas with no per-cell nodes, so this wrapper measures its own
 * width and maps tap offsets back to epoch days with [HeatmapGeometry], which mirrors the
 * library's layout. Taps on undrawn cells (after [endDay]) are ignored. The single semantics
 * node summarises the picture (how many of the shown days were written); the surrounding card
 * offers a date picker as the accessible way to open a day.
 *
 * @param counts intensity per day (1..[StreakEngine.MAX_INTENSITY]), keyed by
 *   [java.time.LocalDate.toEpochDay]; the scale is pinned so the buckets render absolutely
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
    var size by remember { mutableStateOf(IntSize.Zero) }
    val currentOnDayTap by rememberUpdatedState(onDayTap)
    val pinnedCounts = remember(counts, endDay) { StreakEngine.pinIntensityScale(counts, endDay) }
    val description = remember(counts, endDay, weeks) {
        val shown = HeatmapGeometry.daysShown(endDay, weeks)
        val written = HeatmapGeometry.daysWritten(counts.keys, endDay, weeks)
        "Journaling heatmap: $written of the last ${plural(shown, "day", "days")} written"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .onSizeChanged { size = it }
            .pointerInput(weeks, endDay) {
                detectTapGestures { offset ->
                    HeatmapGeometry.dayAt(
                        x = offset.x,
                        y = offset.y,
                        width = size.width.toFloat(),
                        weeks = weeks,
                        endDay = endDay,
                    )?.let(currentOnDayTap)
                }
            },
    ) {
        ContributionHeatmap(
            counts = pinnedCounts,
            endDay = endDay,
            weeks = weeks,
            levelColors = if (isSystemInDarkTheme()) GithubGreens else LightGreens,
        )
    }
}
