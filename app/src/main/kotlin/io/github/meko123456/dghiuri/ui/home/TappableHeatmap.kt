package io.github.meko123456.dghiuri.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
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
import io.github.meko123456.heatmap.ContributionHeatmap

/**
 * [ContributionHeatmap] that reports which day was tapped.
 *
 * The library draws on a plain Canvas with no per-cell nodes, so this wrapper measures its own
 * width and maps tap offsets back to epoch days with [HeatmapGeometry], which mirrors the
 * library's layout. Taps on undrawn cells (after [endDay]) are ignored.
 *
 * @param counts intensity per day, keyed by [java.time.LocalDate.toEpochDay]
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Journaling heatmap, last $weeks weeks" }
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
        ContributionHeatmap(counts = counts, endDay = endDay, weeks = weeks)
    }
}
