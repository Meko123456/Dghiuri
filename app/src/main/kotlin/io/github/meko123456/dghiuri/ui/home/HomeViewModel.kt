package io.github.meko123456.dghiuri.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.meko123456.dghiuri.data.Entry
import io.github.meko123456.dghiuri.data.EntryRepository
import io.github.meko123456.dghiuri.domain.DayClock
import io.github.meko123456.dghiuri.domain.EntryStats
import io.github.meko123456.dghiuri.domain.MarkdownExport
import io.github.meko123456.dghiuri.domain.StreakEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Everything the home screen renders, derived from the entry table in one place.
 *
 * @param entries every entry, newest first (the repository's order)
 * @param stats aggregate numbers for the stats card
 * @param heatmapCounts intensity per epoch day for the contribution heatmap
 * @param today the epoch day the state was built for
 * @param query the raw search text as last typed
 * @param results entries matching the debounced query; empty when the query is blank
 * @param searching true while [results] still answer an older query than [query]
 * @param loading true until the first database emission arrives
 */
data class HomeUiState(
    val entries: List<Entry> = emptyList(),
    val stats: EntryStats = EntryStats.EMPTY,
    val heatmapCounts: Map<Long, Int> = emptyMap(),
    val today: Long = LocalDate.now().toEpochDay(),
    val query: String = "",
    val results: List<Entry> = emptyList(),
    val searching: Boolean = false,
    val loading: Boolean = true,
)

/** The rendered export plus how many entries went into it, read from the database in one go. */
data class ExportDocument(val markdown: String, val entryCount: Int)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: EntryRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    /** Bumped by [refreshToday]; restarts the day clock so a sleeping timer cannot leave it stale. */
    private val dayRefresh = MutableStateFlow(0)

    /**
     * Today's epoch day: emitted immediately, again at every local midnight, and re-read whenever
     * [refreshToday] is called. Timers do not run while the device sleeps, so the resume hook is
     * what catches an overnight background.
     */
    private val today: Flow<Long> = dayRefresh
        .flatMapLatest {
            flow {
                while (true) {
                    val now = ZonedDateTime.now()
                    emit(DayClock.epochDay(now))
                    delay(DayClock.millisUntilNextMidnight(now))
                }
            }
        }
        .distinctUntilChanged()

    /** Search hits tagged with the trimmed query they answer, so the UI can tell "stale" from "none". */
    private class SearchResults(val query: String, val entries: List<Entry>) {
        companion object {
            val NONE = SearchResults("", emptyList())
        }
    }

    private val results: Flow<SearchResults> = query
        .map { it.trim() }
        .distinctUntilChanged()
        // Clearing the box should empty the results at once; only real typing is debounced.
        .debounce { trimmed -> if (trimmed.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
        .flatMapLatest { trimmed ->
            if (trimmed.isEmpty()) {
                flowOf(SearchResults.NONE)
            } else {
                repository.search(trimmed).map { SearchResults(trimmed, it) }
            }
        }

    /** Whole-diary aggregates, recomputed only when the entries or the day change. */
    private class Derived(
        val entries: List<Entry>,
        val stats: EntryStats,
        val heatmapCounts: Map<Long, Int>,
        val today: Long,
    )

    // Word counting is a regex pass over every entry, so it runs off the main thread and is not
    // repeated for search keystrokes, which only touch the cheap combine below.
    private val derived: Flow<Derived> =
        combine(repository.observeAll(), today) { entries, today ->
            Derived(
                entries = entries,
                stats = EntryStats.from(entries, today),
                heatmapCounts = StreakEngine.heatmapCounts(
                    entries.associate { it.epochDay to StreakEngine.wordCount(it.markdown) },
                ),
                today = today,
            )
        }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<HomeUiState> =
        combine(derived, query, results) { d, rawQuery, found ->
            HomeUiState(
                entries = d.entries,
                stats = d.stats,
                heatmapCounts = d.heatmapCounts,
                today = d.today,
                query = rawQuery,
                results = found.entries,
                searching = rawQuery.trim() != found.query,
                loading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(q: String) {
        query.value = q
    }

    /** Re-reads the calendar day; the screen calls this whenever it comes back to the foreground. */
    fun refreshToday() {
        dayRefresh.update { it + 1 }
    }

    /**
     * The whole diary as one markdown document, read straight from the database rather than from
     * [uiState], which may still be the empty initial value if the picker result arrives before
     * the first emission (for example after process death behind the system file picker).
     */
    suspend fun exportMarkdown(): ExportDocument {
        val entries = repository.observeAll().first()
        return ExportDocument(markdown = MarkdownExport.render(entries), entryCount = entries.size)
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 250L

        fun factory(repository: EntryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
