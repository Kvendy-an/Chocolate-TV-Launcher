package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.kvendy.chocolatetvlauncher.data.LauncherDefaults
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.FloatValueSelector
import com.kvendy.chocolatetvlauncher.ui.components.GlowPosition
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import com.kvendy.chocolatetvlauncher.ui.components.SteppedValueSelector
import com.kvendy.chocolatetvlauncher.ui.components.ValueSelector
import com.kvendy.chocolatetvlauncher.ui.components.id
import kotlinx.coroutines.launch

@Composable
internal fun TileStylePreference(preferencesData: PreferencesData) {
    val coroutineScope = rememberCoroutineScope()
    val iconCornerRadius by preferencesData.iconCornerRadius.collectAsState(initial = LauncherDefaults.ICON_CORNER_RADIUS)
    val appLabelScale by preferencesData.appLabelScale.collectAsState(initial = 0.85f)
    val showAppLabels by preferencesData.showAppLabels.collectAsState(initial = true)

    ValueSelector(
        text = "Tile Radius",
        value = iconCornerRadius.toInt(),
        onValueChange = { coroutineScope.launch { preferencesData.setIconCornerRadius(it.toFloat()) } },
        range = 0..100
    )

    PreferenceRow("App Labels", checked = showAppLabels) {
        coroutineScope.launch { preferencesData.setShowAppLabels(it) }
    }

    FloatValueSelector(
        text = "App Label Size",
        value = appLabelScale,
        onValueChange = { coroutineScope.launch { preferencesData.setAppLabelScale(it) } },
        range = 0.5f..1.2f
    )
}

@Composable
internal fun FocusGlowPreference(preferencesData: PreferencesData) {
    val coroutineScope = rememberCoroutineScope()
    val showGlowEffect by preferencesData.showGlowEffect.collectAsState(initial = true)
    val glowRadius by preferencesData.glowRadius.collectAsState(initial = LauncherDefaults.GLOW_RADIUS)
    val glowPosition by preferencesData.glowPosition.collectAsState(initial = "topright")

    PreferenceRow("Glow", checked = showGlowEffect) {
        coroutineScope.launch { preferencesData.setShowGlowEffect(it) }
    }

    if (showGlowEffect) {
        ValueSelector(
            text = "Glow Intensity",
            value = glowRadius.toInt(),
            onValueChange = { coroutineScope.launch { preferencesData.setGlowRadius(it.toFloat()) } },
            range = 10..100
        )

        SteppedValueSelector(
            text = "Glow Position",
            value = GlowPosition.fromId(glowPosition).ordinal,
            options = GlowPosition.entries.indices.toList(),
            label = { GlowPosition.entries[it].label },
            onValueChange = { coroutineScope.launch { preferencesData.setGlowPosition(GlowPosition.entries[it].id) } }
        )
    }
}
