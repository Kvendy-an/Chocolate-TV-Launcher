package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.ui.components.SmallScaleButton
import com.kvendy.chocolatetvlauncher.ui.components.TvSearchField
import com.kvendy.chocolatetvlauncher.ui.components.themedListItemColors
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

/**
 * A searchable, vertically-scrolling checklist for showing/hiding apps on the home screen.
 * Tapping a row toggles it; long-pressing opens [com.kvendy.chocolatetvlauncher.ui.home.menus.AppContextMenu]
 * for the rest of the per-app actions (open, app info, uninstall).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
internal fun LazyListScope.manageAppsList(
    filteredApps: List<AppInfo>,
    query: String,
    onQueryChange: (String) -> Unit,
    hiddenApps: Set<String>,
    onToggleApp: (String) -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onLongPressApp: (AppInfo) -> Unit,
) {
    item {
        Column {
            TvSearchField(query = query, onQueryChange = onQueryChange)

            Spacer(modifier = Modifier.height(Spacing.Small))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                SmallScaleButton(onClick = onShowAll) { Text("Show All") }
                SmallScaleButton(onClick = onHideAll) { Text("Hide All") }
            }

            Spacer(modifier = Modifier.height(Spacing.Small))
        }
    }

    items(filteredApps, key = { it.packageName }) { app ->
        val isHidden = app.packageName in hiddenApps
        ListItem(
            selected = false,
            onClick = { onToggleApp(app.packageName) },
            onLongClick = { onLongPressApp(app) },
            scale = ListItemDefaults.scale(focusedScale = 1.01f),
            colors = themedListItemColors(),
            headlineContent = { Text(app.label) },
            supportingContent = { Text(if (isHidden) "Hidden" else "Visible on Home") },
            trailingContent = {
                Switch(
                    checked = !isHidden,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        )
    }
}
