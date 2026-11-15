package com.kvendy.chocolatetvlauncher.ui.home.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.TileBackground
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

private val backgroundModes = listOf(TileBackground.None, TileBackground.Auto)
private val swatchRows = TileBackground.entries.filter { it.color != null }.chunked(6)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun TileBackgroundPicker(
    selected: TileBackground,
    onSelect: (TileBackground) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(400.dp)) {
            Column(
                modifier = Modifier.padding(Spacing.Large),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                Text(stringResource(R.string.tile_background), style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    backgroundModes.forEach { choice ->
                        BackgroundChoice(choice, selected, focusRequester, onSelect, Modifier.weight(1f))
                    }
                }
                swatchRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        row.forEach { choice ->
                            BackgroundChoice(choice, selected, focusRequester, onSelect, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BackgroundChoice(
    choice: TileBackground,
    current: TileBackground,
    focusRequester: FocusRequester,
    onSelect: (TileBackground) -> Unit,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isSelected = choice == current
    val label = stringResource(choice.label)
    val swatch = choice.color

    Surface(
        onClick = { onSelect(choice) },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) colors.primaryContainer else colors.surfaceVariant,
            contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            focusedContainerColor = colors.primary,
            focusedContentColor = colors.onPrimary
        ),
        modifier = modifier
            .semantics {
                selected = isSelected
                if (swatch != null) contentDescription = label
            }
            .then(if (isSelected) Modifier.focusRequester(focusRequester) else Modifier)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            if (swatch == null) {
                Text(label, maxLines = 1)
            } else {
                Box(
                    modifier = Modifier.size(28.dp)
                        .background(swatch, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (swatch.luminance() > 0.18f) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}
