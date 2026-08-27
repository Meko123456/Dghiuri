package io.github.meko123456.dghiuri.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.meko123456.dghiuri.dghiuriApp
import io.github.meko123456.dghiuri.ui.markdown.MarkdownText
import io.github.meko123456.markdown.Markdown
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PLACEHOLDER = "How was your day? Markdown works: # heading, - list, **bold**…"

/**
 * Edit one day's entry: mood row on top, then either the markdown text field or its rendered
 * preview, with a "N words · saved" footer.
 *
 * The [EditorViewModel] is keyed by [epochDay] and lives in the Activity's store, so a pending
 * debounced save survives rotation and the system back gesture (which bypasses [onBack] but
 * still disposes this screen, and disposal flushes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(epochDay: Long, onBack: () -> Unit) {
    val viewModel = editorViewModel(epochDay)
    val state by viewModel.state.collectAsState()

    val dateLabel = remember(epochDay) {
        LocalDate.ofEpochDay(epochDay)
            .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
    }
    val isToday = remember(epochDay) { epochDay == LocalDate.now().toEpochDay() }

    // Owned by the screen (not the ViewModel) so cursor and IME composition update
    // synchronously; the ViewModel only sees the plain text. Re-seeded when the entry loads.
    var fieldValue by remember(state.loaded) {
        mutableStateOf(TextFieldValue(state.text, TextRange(state.text.length)))
    }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(viewModel) {
        onDispose { viewModel.flush() }
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.onDeleteHandled()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = dateLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isToday) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.flush()
                            onBack()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::togglePreview) {
                        if (state.previewMode) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        } else {
                            // No "preview" glyph in icons-core; the emoji is decorative and the
                            // action is what gets announced.
                            Text(
                                text = "👁",
                                fontSize = 20.sp,
                                modifier = Modifier.clearAndSetSemantics {
                                    contentDescription = "Show preview"
                                },
                            )
                        }
                    }
                    if (state.exists) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                        }
                    }
                },
            )
        },
        // safeDrawing includes the IME, so the padding grows with the keyboard and the field
        // is never covered — no separate imePadding (which would double-count the nav bar).
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.loaded) {
                MoodPicker(
                    selected = state.mood,
                    onSelect = viewModel::onMoodChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                HorizontalDivider()
                if (state.previewMode) {
                    PreviewPane(
                        text = state.text,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                } else {
                    EditorPane(
                        value = fieldValue,
                        onValueChange = { value ->
                            fieldValue = value
                            viewModel.onTextChange(value.text)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
                EditorFooter(wordCount = state.wordCount, dirty = state.dirty)
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    if (showDeleteDialog) {
        DeleteDialog(
            dateLabel = dateLabel,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun editorViewModel(epochDay: Long): EditorViewModel {
    val repository = LocalContext.current.dghiuriApp.repository
    return viewModel(
        key = "editor/$epochDay",
        factory = viewModelFactory {
            initializer { EditorViewModel(repository = repository, epochDay = epochDay) }
        },
    )
}

/** Full-height monospace field. Requests focus (and so the keyboard) when opened empty. */
@Composable
private fun EditorPane(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val openedEmpty = remember { value.text.isEmpty() }
    LaunchedEffect(Unit) {
        if (openedEmpty) focusRequester.requestFocus()
    }

    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { innerField ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (value.text.isEmpty()) {
                    Text(
                        text = PLACEHOLDER,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                innerField()
            }
        },
    )
}

/** Rendered markdown, parsed once per distinct [text]. */
@Composable
private fun PreviewPane(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Nothing to preview yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        val blocks = remember(text) { Markdown.parse(text) }
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            MarkdownText(blocks = blocks)
        }
    }
}

@Composable
private fun EditorFooter(wordCount: Int, dirty: Boolean) {
    val words = if (wordCount == 1) "1 word" else "$wordCount words"
    val status = if (dirty) "saving…" else "saved"
    Text(
        text = "$words · $status",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun DeleteDialog(
    dateLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this entry?") },
        text = { Text("The entry for $dateLabel and its mood will be removed. This can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
