package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import kotlinx.coroutines.launch

@Composable
internal fun SearchPreference(preferencesData: PreferencesData) {
    val scope = rememberCoroutineScope()
    val showHiddenApps by preferencesData.showHiddenAppsInSearch.collectAsState(initial = false)
    PreferenceRow(
        text = stringResource(R.string.preferences_search_hidden_apps),
        checked = showHiddenApps,
        summary = stringResource(R.string.preferences_search_hidden_apps_summary)
    ) { show -> scope.launch { preferencesData.setShowHiddenAppsInSearch(show) } }
}
