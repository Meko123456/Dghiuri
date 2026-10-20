package io.github.meko123456.dghiuri

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.meko123456.dghiuri.domain.SharedText
import io.github.meko123456.dghiuri.ui.editor.EditorScreen
import io.github.meko123456.dghiuri.ui.home.HomeScreen
import io.github.meko123456.dghiuri.ui.theme.DghiuriTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    /** Text shared from another app, until the day's editor has added it to the entry. */
    private var shared by mutableStateOf<Shared?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A fresh start only: recreated after a turn, the activity is handed the same intent, and
        // the text is in the entry already.
        if (savedInstanceState == null) take(intent)
        setContent {
            DghiuriTheme {
                DghiuriNav(shared = shared, onSharedAdded = { shared = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() } ?: return
        shared = Shared(
            markdown = SharedText.toMarkdown(text, intent.getStringExtra(Intent.EXTRA_SUBJECT)),
            epochDay = LocalDate.now().toEpochDay(),
        )
    }
}

/** Shared text as markdown, and the day it was shared on: the entry it goes into. */
private data class Shared(val markdown: String, val epochDay: Long)

/**
 * Two screens, one piece of state: `null` = Home, otherwise the epoch day open in the editor.
 * Kept in [rememberSaveable] so rotation/process death returns to the same day.
 */
@Composable
private fun DghiuriNav(shared: Shared?, onSharedAdded: () -> Unit) {
    var openDay: Long? by rememberSaveable { mutableStateOf(null) }
    // Shared text goes into the day it was shared on, whatever was open.
    LaunchedEffect(shared) {
        if (shared != null) openDay = shared.epochDay
    }
    val day = openDay
    if (day == null) {
        HomeScreen(onOpenDay = { openDay = it })
    } else {
        BackHandler { openDay = null }
        EditorScreen(
            epochDay = day,
            onBack = { openDay = null },
            append = shared?.takeIf { it.epochDay == day }?.markdown,
            onAppended = onSharedAdded,
        )
    }
}
