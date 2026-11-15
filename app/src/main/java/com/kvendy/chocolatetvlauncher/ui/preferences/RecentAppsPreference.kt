package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.compose.ui.res.stringResource
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.LauncherDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.FloatValueSelector
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import com.kvendy.chocolatetvlauncher.ui.components.ValueSelector
import kotlinx.coroutines.launch

@Composable
fun RecentAppsPreference(preferencesData: PreferencesData) {
    val coroutineScope = rememberCoroutineScope()
    val showRecentApps by preferencesData.showRecentApps.collectAsState(initial = true)
    val recentAppsScale by preferencesData.recentAppsScale.collectAsState(initial = LauncherDefaults.RECENT_APPS_SCALE)
    val recentAppsSquare by preferencesData.recentAppsSquareIcons.collectAsState(initial = LauncherDefaults.RECENT_APPS_SQUARE_ICONS)
    val recentAppsCornerRadius by preferencesData.recentAppsCornerRadius.collectAsState(initial = LauncherDefaults.RECENT_APPS_CORNER_RADIUS)
    val maxApps by preferencesData.maxRecentApps.collectAsState(initial = LauncherDefaults.MAX_RECENT_APPS)

    PreferenceRow(stringResource(R.string.preferences_show_recent_apps), checked = showRecentApps) {
        coroutineScope.launch { preferencesData.setShowRecentApps(it) }
    }

    if (showRecentApps) {
        PreferenceRow(stringResource(R.string.preferences_square_tiles), checked = recentAppsSquare) {
            coroutineScope.launch { preferencesData.setRecentAppsSquareIcons(it) }
        }

        ValueSelector(
            text = stringResource(R.string.preferences_corner_radius),
            value = recentAppsCornerRadius.toInt(),
            onValueChange = { coroutineScope.launch { preferencesData.setRecentAppsCornerRadius(it.toFloat()) } },
            range = 0..100
        )

        ValueSelector(
            text = stringResource(R.string.preferences_history_limit),
            value = maxApps,
            onValueChange = { coroutineScope.launch { preferencesData.setMaxRecentApps(it) } },
            range = 5..20
        )

        FloatValueSelector(
            text = stringResource(R.string.preferences_row_scale),
            value = recentAppsScale,
            onValueChange = { coroutineScope.launch { preferencesData.setRecentAppsScale(it) } },
            range = 0.5f..2.0f
        )
    }
}