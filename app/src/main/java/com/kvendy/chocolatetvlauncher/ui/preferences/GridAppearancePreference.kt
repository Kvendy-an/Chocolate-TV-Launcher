package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import com.kvendy.chocolatetvlauncher.ui.components.ValueSelector
import kotlinx.coroutines.launch

@Composable
fun GridAppearancePreference(preferencesData: PreferencesData) {
    val coroutineScope = rememberCoroutineScope()
    val gridSize by preferencesData.gridSize.collectAsState(initial = 6)
    val folderGridSize by preferencesData.folderGridSize.collectAsState(initial = 4)
    val gridSquareIcons by preferencesData.gridSquareIcons.collectAsState(initial = false)

    ValueSelector(
        text = stringResource(R.string.preferences_apps_per_row),
        value = gridSize,
        onValueChange = { coroutineScope.launch { preferencesData.setGridSize(it) } },
        range = 2..12
    )

    ValueSelector(
        text = stringResource(R.string.preferences_apps_per_folder_row),
        value = folderGridSize,
        onValueChange = { coroutineScope.launch { preferencesData.setFolderGridSize(it) } },
        range = 2..8
    )

    PreferenceRow(stringResource(R.string.preferences_square_tiles), checked = gridSquareIcons) {
        coroutineScope.launch { preferencesData.setGridSquareIcons(it) }
    }
}
