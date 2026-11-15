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
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import com.kvendy.chocolatetvlauncher.ui.components.SmallScaleButton
import com.kvendy.chocolatetvlauncher.ui.components.TvSearchField
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

/** A searchable, vertically-scrolling checklist for choosing which apps' content is included. */
@OptIn(ExperimentalTvMaterial3Api::class)
internal fun LazyListScope.includedAppsList(
    filteredApps: List<AppInfo>,
    query: String,
    onQueryChange: (String) -> Unit,
    excludedApps: Set<String>,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
) {
    item {
        Column {
            TvSearchField(query = query, onQueryChange = onQueryChange)

            Spacer(modifier = Modifier.height(Spacing.Small))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                SmallScaleButton(onClick = onSelectAll) { Text("Select All") }
                SmallScaleButton(onClick = onDeselectAll) { Text("Deselect All") }
            }

            Spacer(modifier = Modifier.height(Spacing.Small))
        }
    }

    items(filteredApps, key = { it.packageName }) { app ->
        PreferenceRow(app.label, checked = app.packageName !in excludedApps) {
            onToggleApp(app.packageName)
        }
    }
}
