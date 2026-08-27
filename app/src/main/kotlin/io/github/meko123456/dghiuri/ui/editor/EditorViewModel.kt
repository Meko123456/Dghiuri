package io.github.meko123456.dghiuri.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.dghiuri.data.EntryRepository
import io.github.meko123456.dghiuri.domain.Mood
import io.github.meko123456.dghiuri.domain.StreakEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Everything the editor shows for one day.
 *
 * @param text the raw markdown being edited
 * @param mood 1..5, or null when none is picked
 * @param previewMode true while rendered markdown is shown instead of the text field
 * @param loaded false until the stored entry (if any) has been read; the screen waits for it
 * @param wordCount words in [text]
 * @param exists true when the day currently has a row in the database
 * @param dirty true while an edit has not reached the database yet ("saving…" vs "saved")
 * @param deleted set once [EditorViewModel.delete] has finished; the screen navigates back and
 *   acknowledges it with [EditorViewModel.onDeleteHandled]
 */
data class EditorUiState(
    val text: String = "",
    val mood: Int? = null,
    val previewMode: Boolean = false,
    val loaded: Boolean = false,
    val wordCount: Int = 0,
    val exists: Boolean = false,
    val dirty: Boolean = false,
    val deleted: Boolean = false,
)

/**
 * Editor state for a single [epochDay].
 *
 * The stored entry is read exactly once, on creation, so the text field never fights an
 * incoming database emission while the user types. Text edits are debounced by
 * [saveDebounceMs]; mood changes save immediately. [flush] forces any pending edit to disk and
 * is what the screen calls when it leaves composition.
 *
 * Saving goes through [EntryRepository.save], which deletes the row when the text is blank and
 * there is no mood — an accidentally opened day never counts toward the streak.
 */
class EditorViewModel(
    private val repository: EntryRepository,
    val epochDay: Long,
    private val saveDebounceMs: Long = DEFAULT_SAVE_DEBOUNCE_MS,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var saveJob: Job? = null
    private var deleting = false

    init {
        viewModelScope.launch {
            val entry = repository.observe(epochDay).first()
            _state.update { current ->
                if (current.dirty) {
                    // The user got ahead of the load; keep what they typed.
                    current.copy(loaded = true, exists = entry != null)
                } else {
                    val text = entry?.markdown.orEmpty()
                    current.copy(
                        text = text,
                        mood = entry?.mood,
                        wordCount = StreakEngine.wordCount(text),
                        exists = entry != null,
                        loaded = true,
                    )
                }
            }
        }
    }

    /** Updates the text right away and schedules a debounced save. */
    fun onTextChange(text: String) {
        if (text == _state.value.text) return
        _state.update { it.copy(text = text, wordCount = StreakEngine.wordCount(text), dirty = true) }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(saveDebounceMs)
            persist()
        }
    }

    /** Sets the mood (null clears it) and saves immediately, together with any pending text. */
    fun onMoodChange(mood: Int?) {
        val valid = mood?.takeIf { it in Mood.range }
        if (valid == _state.value.mood) return
        _state.update { it.copy(mood = valid, dirty = true) }
        flush()
    }

    fun togglePreview() {
        _state.update { it.copy(previewMode = !it.previewMode) }
    }

    /** Cancels the debounce and writes the current state now, if anything is unsaved. */
    fun flush() {
        saveJob?.cancel()
        if (deleting || !_state.value.dirty) return
        saveJob = viewModelScope.launch { persist() }
    }

    /** Removes the day's entry and resets to an empty state with [EditorUiState.deleted] set. */
    fun delete() {
        if (deleting) return
        deleting = true
        saveJob?.cancel()
        viewModelScope.launch {
            withContext(NonCancellable) { repository.delete(epochDay) }
            deleting = false
            _state.value = EditorUiState(loaded = true, deleted = true)
        }
    }

    /** Clears [EditorUiState.deleted] once the screen has navigated away. */
    fun onDeleteHandled() {
        _state.update { it.copy(deleted = false) }
    }

    private suspend fun persist() {
        if (deleting) return
        val snapshot = _state.value
        // A write that has started always completes, so a flush arriving mid-save cannot lose
        // data or reorder against the write it triggers.
        withContext(NonCancellable) { repository.save(epochDay, snapshot.text, snapshot.mood) }
        _state.update { current ->
            current.copy(
                exists = snapshot.text.isNotBlank() || snapshot.mood != null,
                dirty = current.text != snapshot.text || current.mood != snapshot.mood,
            )
        }
    }

    companion object {
        const val DEFAULT_SAVE_DEBOUNCE_MS: Long = 600L
    }
}
