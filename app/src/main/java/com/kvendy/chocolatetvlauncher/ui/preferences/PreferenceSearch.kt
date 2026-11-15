package com.kvendy.chocolatetvlauncher.ui.preferences

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.ui.components.themedListItemColors
import java.util.Locale

private val settingLabels = mapOf(
    PreferencePage.Search to listOf(R.string.preferences_search_hidden_apps),
    PreferencePage.Backup to listOf(R.string.preferences_export, R.string.preferences_import),
    PreferencePage.RemoteButtons to listOf(R.string.remote_override_home, R.string.remote_adb_setup),
    PreferencePage.SectionHeaders to listOf(
        R.string.show_section_titles, R.string.show_section_icons, R.string.section_title_size
    ),
    PreferencePage.NavigationBars to listOf(
        R.string.preferences_clock_and_shortcuts, R.string.show_tab_labels,
        R.string.preferences_tv_inputs, R.string.preferences_clock, R.string.preferences_wifi,
        R.string.preferences_bluetooth, R.string.preferences_search_shortcut,
        R.string.preferences_settings_shortcut
    ),
    PreferencePage.RecentApps to listOf(
        R.string.preferences_show_recent_apps, R.string.preferences_square_tiles,
        R.string.preferences_corner_radius, R.string.preferences_history_limit, R.string.preferences_row_scale
    ),
    PreferencePage.Theme to listOf(
        R.string.preferences_theme_mode, R.string.preferences_accent_color, R.string.preferences_set_wallpaper,
        R.string.preferences_remove_wallpaper, R.string.preferences_gradient
    ),
    PreferencePage.TileStyle to listOf(
        R.string.preferences_tile_radius, R.string.preferences_app_labels, R.string.preferences_app_label_size
    ),
    PreferencePage.FocusGlow to listOf(
        R.string.preferences_glow, R.string.preferences_glow_intensity, R.string.preferences_glow_position
    ),
    PreferencePage.AppGrid to listOf(
        R.string.preferences_apps_per_row, R.string.preferences_apps_per_folder_row, R.string.preferences_square_tiles
    ),
    PreferencePage.Recommendations to listOf(
        R.string.preferences_enable_recommendations, R.string.preferences_show_on_home,
        R.string.preferences_card_size, R.string.preferences_tv_listings_permission, R.string.preferences_included_apps
    ),
    PreferencePage.Notifications to listOf(
        R.string.preferences_enable_notifications, R.string.preferences_show_on_home, R.string.preferences_notification_access
    ),
    PreferencePage.System to listOf(
        R.string.preferences_default_launcher, R.string.preferences_ui_scale, R.string.preferences_navigation_sound,
        R.string.preferences_dim_when_idle, R.string.preferences_idle_timeout, R.string.preferences_idle_dim_strength
    )
)

internal data class PreferenceSearchResult(
    val page: PreferencePage,
    @StringRes val label: Int,
    val title: String,
    val path: String
) {
    val key = "${page.name}:$label"
    val searchableTitle = title.searchText()
    val searchablePath = path.searchText()
}

private val separators = Regex("[^\\p{L}\\p{N}]+")
private fun String.searchText() = lowercase(Locale.ROOT).replace("-", "").replace(separators, " ").trim()

private fun Resources.searchEntries() = buildList {
    for (page in PreferencePage.entries) {
        val parent = page.parent ?: continue
        val pageTitle = getString(page.title)
        val parentTitle = getString(parent.title)
        add(PreferenceSearchResult(page, page.title, pageTitle, parentTitle))
        val path = if (parent == PreferencePage.Root) pageTitle
        else getString(R.string.preferences_search_path, parentTitle, pageTitle)
        for (label in settingLabels[page].orEmpty()) {
            add(PreferenceSearchResult(page, label, getString(label), path))
        }
    }
}

@Composable
internal fun rememberPreferenceSearchResults(query: String): List<PreferenceSearchResult> {
    val resources = LocalResources.current
    val entries = remember(resources, LocalConfiguration.current) { resources.searchEntries() }
    return remember(query, entries) {
        val phrase = query.searchText()
        if (phrase.isEmpty()) return@remember emptyList()
        val words = phrase.split(' ')
        entries.mapNotNull { result ->
            val title = result.searchableTitle
            val rank = when {
                title == phrase -> 0
                title.startsWith(phrase) -> 1
                words.all { it in title } -> 2
                words.any { it in title } && words.all { it in title || it in result.searchablePath } -> 3
                else -> return@mapNotNull null
            }
            result to rank
        }.sortedBy { it.second }.map { it.first }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun PreferenceSearchResults(
    results: List<PreferenceSearchResult>,
    returningTo: String?,
    focusFirst: Boolean,
    onFirstFocused: () -> Unit,
    onSelect: (PreferenceSearchResult) -> Unit
) {
    val listState = rememberLazyListState()
    val restoreFocus = remember { FocusRequester() }
    val focusIndex = if (focusFirst) 0 else results.indexOfFirst { it.key == returningTo }

    LaunchedEffect(focusIndex, focusFirst) {
        if (focusIndex in results.indices) {
            listState.scrollToItem(focusIndex)
            restoreFocus.requestFocus()
        }
        if (focusFirst) onFirstFocused()
    }

    PreferenceList(state = listState) {
        if (results.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.preferences_search_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        itemsIndexed(results, key = { _, result -> result.key }) { index, result ->
            ListItem(
                modifier = if (index == focusIndex) Modifier.focusRequester(restoreFocus) else Modifier,
                selected = false,
                onClick = { onSelect(result) },
                headlineContent = { Text(result.title) },
                supportingContent = { Text(result.path) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                scale = ListItemDefaults.scale(focusedScale = 1f),
                colors = themedListItemColors()
            )
        }
    }
}
