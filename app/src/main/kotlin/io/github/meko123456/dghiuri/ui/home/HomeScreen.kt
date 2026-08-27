package io.github.meko123456.dghiuri.ui.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.dghiuri.data.Entry
import io.github.meko123456.dghiuri.dghiuriApp
import io.github.meko123456.dghiuri.domain.EntryStats
import io.github.meko123456.dghiuri.domain.StreakEngine
import io.github.meko123456.dghiuri.ui.theme.DghiuriTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val HEATMAP_WEEKS = 26
private const val EXPORT_FILE_NAME = "dghiuri-export.md"
private const val EXPORT_MIME_TYPE = "text/markdown"

private val cardModifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)

/**
 * Home: heatmap, stats, today's entry and the archive, plus search and markdown export.
 *
 * Owns the ViewModel and the Android plumbing (document picker, snackbar); the layout itself is
 * [HomeContent], which takes plain state so it can be previewed.
 */
@Composable
fun HomeScreen(onOpenDay: (Long) -> Unit) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(context.dghiuriApp.repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The day can change while the app sits in the background; re-read it on every return.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val appContext = context.applicationContext
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val document = viewModel.exportMarkdown()
                val written = withContext(Dispatchers.IO) { writeTextTo(appContext, uri, document.markdown) }
                snackbarHostState.showSnackbar(
                    if (written) {
                        "Exported ${plural(document.entryCount, "entry", "entries")}"
                    } else {
                        "Export failed"
                    },
                )
            }
        }
    }

    HomeContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onExport = { exportLauncher.launch(EXPORT_FILE_NAME) },
        onOpenDay = onOpenDay,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onExport: () -> Unit,
    onOpenDay: (Long) -> Unit,
) {
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    // Compose state is the source of truth for the text field (synchronous edits); the
    // ViewModel is told afterwards, which also re-syncs it after process death.
    LaunchedEffect(searchText) { onQueryChange(searchText) }

    val openSearch: () -> Unit = { searchVisible = true }
    val closeSearch: () -> Unit = {
        searchText = ""
        searchVisible = false
    }
    if (searchVisible) {
        BackHandler(onBack = closeSearch)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Dghiuri")
                            Text(
                                text = "დღიური",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = if (searchVisible) closeSearch else openSearch) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = if (searchVisible) "Close search" else "Search entries",
                            )
                        }
                        IconButton(onClick = onExport, enabled = state.entries.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export all entries as markdown",
                            )
                        }
                    },
                )
                AnimatedVisibility(visible = searchVisible) {
                    SearchField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        onClose = closeSearch,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Write today") },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = { onOpenDay(state.today) },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            state.loading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            searchVisible && searchText.isNotBlank() -> SearchResultsList(
                state = state,
                query = searchText.trim(),
                onOpenDay = onOpenDay,
                modifier = contentModifier,
            )
            else -> HomeList(state = state, onOpenDay = onOpenDay, modifier = contentModifier)
        }
    }
}

@Composable
private fun HomeList(
    state: HomeUiState,
    onOpenDay: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = state.today
    val todayEntry = remember(state.entries, today) { state.entries.firstOrNull { it.epochDay == today } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
    ) {
        item(key = "heatmap") {
            HeatmapCard(
                counts = state.heatmapCounts,
                today = today,
                maxWeeks = HEATMAP_WEEKS,
                onOpenDay = { day -> if (day <= today) onOpenDay(day) },
                modifier = cardModifier,
            )
        }
        if (state.entries.isEmpty()) {
            item(key = "empty") {
                EmptyState(onWriteToday = { onOpenDay(today) })
            }
        } else {
            item(key = "stats") {
                StatsCard(stats = state.stats, modifier = cardModifier)
            }
            item(key = "today") {
                TodayCard(entry = todayEntry, onOpenToday = { onOpenDay(today) }, modifier = cardModifier)
            }
            item(key = "recent") {
                SectionHeader("Recent")
            }
            entryRows(state.entries, today, onOpenDay)
        }
    }
}

@Composable
private fun SearchResultsList(
    state: HomeUiState,
    query: String,
    onOpenDay: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
    ) {
        when {
            state.results.isNotEmpty() -> {
                item(key = "results") {
                    SectionHeader(
                        if (state.searching) {
                            "Searching…"
                        } else {
                            "${plural(state.results.size, "match", "matches")} for “$query”"
                        },
                    )
                }
                entryRows(state.results, state.today, onOpenDay)
            }
            state.searching -> item(key = "searching") {
                CenteredMessage("Searching…")
            }
            else -> item(key = "none") {
                CenteredMessage("No entries match “$query”")
            }
        }
    }
}

/** Entry rows keyed by day, with a divider between neighbours. */
private fun LazyListScope.entryRows(
    entries: List<Entry>,
    today: Long,
    onOpenDay: (Long) -> Unit,
) {
    itemsIndexed(entries, key = { _, entry -> entry.epochDay }) { index, entry ->
        EntryRow(entry = entry, today = today, onClick = { onOpenDay(entry.epochDay) })
        if (index < entries.lastIndex) {
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        }
    }
}

/** Writes [text] to [uri] with truncation, so re-exporting over an older file never leaves a tail. */
private fun writeTextTo(context: Context, uri: Uri, text: String): Boolean = runCatching {
    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return@runCatching false
    stream.bufferedWriter().use { it.write(text) }
    true
}.getOrDefault(false)

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    val today = LocalDate.of(2026, 8, 27).toEpochDay()
    val entries = listOf(
        Entry(
            epochDay = today,
            markdown = "# Rainy morning\n\nSlow start, then a long walk once it cleared.",
            mood = 4,
            updatedAt = 0L,
        ),
        Entry(
            epochDay = today - 1,
            markdown = "Long day at work. **Shipped** the release anyway.",
            mood = 2,
            updatedAt = 0L,
        ),
        Entry(
            epochDay = today - 3,
            markdown = "- groceries\n- call home\n- fix the bike",
            mood = null,
            updatedAt = 0L,
        ),
    )
    val state = HomeUiState(
        entries = entries,
        stats = EntryStats.from(entries, today),
        heatmapCounts = StreakEngine.heatmapCounts(
            entries.associate { it.epochDay to StreakEngine.wordCount(it.markdown) },
        ),
        today = today,
        loading = false,
    )
    DghiuriTheme {
        HomeContent(
            state = state,
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {},
            onExport = {},
            onOpenDay = {},
        )
    }
}
