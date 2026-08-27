package io.github.meko123456.dghiuri.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.meko123456.dghiuri.data.Entry
import io.github.meko123456.dghiuri.data.EntryRepository
import io.github.meko123456.dghiuri.domain.EntryStats
import io.github.meko123456.dghiuri.domain.MarkdownExport
import io.github.meko123456.dghiuri.domain.StreakEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: EntryRepository) : ViewModel() {

    private val query = MutableStateFlow("")

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

    val uiState: StateFlow<HomeUiState> =
        combine(repository.observeAll(), query, results) { entries, rawQuery, found ->
            val today = LocalDate.now().toEpochDay()
            HomeUiState(
                entries = entries,
                stats = EntryStats.from(entries, today),
                heatmapCounts = StreakEngine.heatmapCounts(
                    entries.associate { it.epochDay to StreakEngine.wordCount(it.markdown) },
                ),
                today = today,
                query = rawQuery,
                results = found.entries,
                searching = rawQuery.trim() != found.query,
                loading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(q: String) {
        query.value = q
    }

    /** The whole diary as one markdown document, rendered from the current state. */
    fun exportMarkdown(): String = MarkdownExport.render(uiState.value.entries)

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 250L

        fun factory(repository: EntryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
