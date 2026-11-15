package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.LauncherDefaults
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import com.kvendy.chocolatetvlauncher.ui.components.SectionDivider
import kotlinx.coroutines.launch

@Composable
fun StatusBarPreference(preferencesData: PreferencesData) {
    val coroutineScope = rememberCoroutineScope()
    val showStatusBar by preferencesData.showTopBar.collectAsState(initial = true)
    val showPageTabLabels by preferencesData.showPageTabLabels.collectAsState(initial = LauncherDefaults.SHOW_PAGE_TAB_LABELS)
    val showClock by preferencesData.showClock.collectAsState(initial = true)
    val showWifiShortcut by preferencesData.showWifiShortcut.collectAsState(initial = true)
    val showBluetoothShortcut by preferencesData.showBluetoothShortcut.collectAsState(initial = true)
    val showInputShortcut by preferencesData.showInputShortcut.collectAsState(initial = true)
    val showSearchShortcut by preferencesData.showSearchShortcut.collectAsState(initial = true)
    val showSettingsShortcut by preferencesData.showSettingsShortcut.collectAsState(initial = true)

    PreferenceRow(stringResource(R.string.preferences_clock_and_shortcuts), checked = showStatusBar) {
        coroutineScope.launch { preferencesData.setShowTopBar(it) }
    }

    PreferenceRow(stringResource(R.string.show_tab_labels), checked = showPageTabLabels) {
        coroutineScope.launch { preferencesData.setShowPageTabLabels(it) }
    }

    if (showStatusBar) {
        SectionDivider()
        Text("Clock and Shortcuts", style = MaterialTheme.typography.titleMedium)

        PreferenceRow(stringResource(R.string.preferences_tv_inputs), checked = showInputShortcut) {
            coroutineScope.launch { preferencesData.setShowInputShortcut(it) }
        }
        PreferenceRow(stringResource(R.string.preferences_clock), checked = showClock) {
            coroutineScope.launch { preferencesData.setShowClock(it) }
        }
        PreferenceRow(stringResource(R.string.preferences_wifi), checked = showWifiShortcut) {
            coroutineScope.launch { preferencesData.setShowWifiShortcut(it) }
        }
        PreferenceRow(stringResource(R.string.preferences_bluetooth), checked = showBluetoothShortcut) {
            coroutineScope.launch { preferencesData.setShowBluetoothShortcut(it) }
        }
        PreferenceRow(stringResource(R.string.preferences_search_shortcut), checked = showSearchShortcut) {
            coroutineScope.launch { preferencesData.setShowSearchShortcut(it) }
        }
        PreferenceRow(stringResource(R.string.preferences_settings_shortcut), checked = showSettingsShortcut) {
            coroutineScope.launch { preferencesData.setShowSettingsShortcut(it) }
        }
    }
}