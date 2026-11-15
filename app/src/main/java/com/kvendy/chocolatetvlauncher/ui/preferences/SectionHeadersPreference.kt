package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.LauncherDefaults
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.FloatValueSelector
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import kotlinx.coroutines.launch

@Composable
internal fun SectionHeadersPreference(preferencesData: PreferencesData) {
    val coroutineScope = rememberCoroutineScope()
    val sectionTitleScale by preferencesData.sectionTitleScale.collectAsState(initial = LauncherDefaults.SECTION_TITLE_SCALE)
    val showSectionTitles by preferencesData.showSectionTitles.collectAsState(initial = LauncherDefaults.SHOW_SECTION_TITLES)
    val showSectionIcons by preferencesData.showSectionIcons.collectAsState(initial = LauncherDefaults.SHOW_SECTION_ICONS)

    PreferenceRow(stringResource(R.string.show_section_titles), checked = showSectionTitles) {
        coroutineScope.launch { preferencesData.setShowSectionTitles(it) }
    }

    PreferenceRow(stringResource(R.string.show_section_icons), checked = showSectionIcons) {
        coroutineScope.launch { preferencesData.setShowSectionIcons(it) }
    }

    if (showSectionTitles || showSectionIcons) {
        FloatValueSelector(
            text = stringResource(R.string.section_title_size),
            value = sectionTitleScale,
            onValueChange = { coroutineScope.launch { preferencesData.setSectionTitleScale(it) } },
            range = 0.5f..1.5f
        )
    }
}
