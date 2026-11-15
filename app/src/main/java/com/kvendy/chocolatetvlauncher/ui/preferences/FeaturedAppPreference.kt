package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.FloatValueSelector
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import com.kvendy.chocolatetvlauncher.ui.components.themedListItemColors
import com.kvendy.chocolatetvlauncher.ui.util.openAppInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FeaturedAppPreference(preferencesData: PreferencesData, apps: List<AppInfo>) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val showFeaturedTile by preferencesData.showFeaturedTile.collectAsState(initial = false)
    val showRecommendationsOnHome by preferencesData.showRecommendationsOnHome.collectAsState(initial = true)
    val contentCardScale by preferencesData.contentCardScale.collectAsState(initial = 0.7f)
    val excludedContentApps by preferencesData.excludedContentApps.collectAsState(initial = emptySet())

    var query by remember { mutableStateOf("") }
    val filteredApps = remember(query, apps) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    PreferenceList {
        item {
            PreferenceRow(stringResource(R.string.preferences_enable_recommendations), checked = showFeaturedTile) {
                coroutineScope.launch { preferencesData.setShowFeaturedTile(it) }
            }
        }

        if (showFeaturedTile) {
            item {
                PreferenceRow(stringResource(R.string.preferences_show_on_home), checked = showRecommendationsOnHome) {
                    coroutineScope.launch { preferencesData.setShowRecommendationsOnHome(it) }
                }
            }
            item {
                FloatValueSelector(
                    text = stringResource(R.string.preferences_card_size),
                    value = contentCardScale,
                    onValueChange = { coroutineScope.launch { preferencesData.setContentCardScale(it) } },
                    range = 0.2f..2.0f
                )
            }
            item {
                ListItem(
                    selected = false,
                    onClick = { context.openAppInfo(context.packageName) },
                    scale = ListItemDefaults.scale(focusedScale = 1.01f),
                    colors = themedListItemColors(),
                    headlineContent = { Text(stringResource(R.string.preferences_tv_listings_permission)) },
                    supportingContent = { Text("Some devices require enabling this manually in App Info → Permissions for content to appear") }
                )
            }
            item { Text(stringResource(R.string.preferences_included_apps), style = MaterialTheme.typography.titleMedium) }
            includedAppsList(
                filteredApps = filteredApps,
                query = query,
                onQueryChange = { query = it },
                excludedApps = excludedContentApps,
                onToggleApp = { coroutineScope.launch { preferencesData.toggleExcludedContentApp(it) } },
                onSelectAll = { coroutineScope.launch { preferencesData.setExcludedContentApps(emptySet()) } },
                onDeselectAll = { coroutineScope.launch { preferencesData.setExcludedContentApps(apps.map { it.packageName }.toSet()) } }
            )
        }
    }
}
