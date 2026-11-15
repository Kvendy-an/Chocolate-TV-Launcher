package com.kvendy.chocolatetvlauncher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PreferenceRow(
    text: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = modifier,
        selected = false,
        onClick = { onCheckedChange(!checked) },
        scale = ListItemDefaults.scale(focusedScale = 1.01f),
        colors = themedListItemColors(),
        headlineContent = { Text(text) },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                // Explicit instead of SwitchDefaults.colors()'s own implicit primary/onPrimary
                // token lookup, which wasn't picking up accent changes reliably.
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedIconColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    )
}
