package io.github.meko123456.dghiuri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.meko123456.dghiuri.ui.editor.EditorScreen
import io.github.meko123456.dghiuri.ui.home.HomeScreen
import io.github.meko123456.dghiuri.ui.theme.DghiuriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DghiuriTheme {
                DghiuriNav()
            }
        }
    }
}

/**
 * Two screens, one piece of state: `null` = Home, otherwise the epoch day open in the editor.
 * Kept in [rememberSaveable] so rotation/process death returns to the same day.
 */
@Composable
private fun DghiuriNav() {
    var openDay: Long? by rememberSaveable { mutableStateOf(null) }
    val day = openDay
    if (day == null) {
        HomeScreen(onOpenDay = { openDay = it })
    } else {
        BackHandler { openDay = null }
        EditorScreen(epochDay = day, onBack = { openDay = null })
    }
}
