package io.github.meko123456.dghiuri.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.meko123456.dghiuri.domain.Mood
import io.github.meko123456.dghiuri.ui.theme.DghiuriTheme

/**
 * The 1..5 mood row: five emoji buttons with the chosen one on a `primaryContainer` circle,
 * plus a small caption naming it ("No mood" when nothing is picked).
 *
 * Tapping the already selected mood clears it, so [onSelect] receives `null` for "no mood".
 * TalkBack hears "Mood: Good, selected" rather than the emoji itself.
 */
@Composable
fun MoodPicker(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (mood in Mood.range) {
                val isSelected = mood == selected
                MoodButton(
                    mood = mood,
                    isSelected = isSelected,
                    onClick = { onSelect(if (isSelected) null else mood) },
                )
            }
        }
        Text(
            text = Mood.label(selected),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun MoodButton(
    mood: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val label = Mood.label(mood)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background)
            // selectable must come before clearAndSetSemantics so its click action survives;
            // clearAndSetSemantics only replaces what follows it and the emoji text below.
            .selectable(selected = isSelected, onClick = onClick, role = Role.Button)
            .clearAndSetSemantics {
                contentDescription = "Mood: $label"
                selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = Mood.emoji(mood), fontSize = 24.sp)
    }
}

@Preview(showBackground = true)
@Composable
private fun MoodPickerPreview() {
    DghiuriTheme {
        MoodPicker(selected = 4, onSelect = {}, modifier = Modifier.padding(16.dp))
    }
}
