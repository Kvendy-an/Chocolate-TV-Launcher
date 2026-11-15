package com.kvendy.chocolatetvlauncher.ui.preferences

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.LauncherDefaults
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.FloatValueSelector
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import com.kvendy.chocolatetvlauncher.ui.components.SteppedValueSelector
import com.kvendy.chocolatetvlauncher.ui.components.themedListItemColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SystemPreference(preferencesData: PreferencesData) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var isDefaultLauncher by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                isDefaultLauncher = resolveInfo?.activityInfo?.packageName == context.packageName
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val uiScale by preferencesData.uiScale.collectAsState(initial = 1f)
    val navigationSoundEnabled by preferencesData.navigationSoundEnabled.collectAsState(initial = true)
    val idleDimEnabled by preferencesData.idleDimEnabled.collectAsState(initial = true)
    val idleDimStrength by preferencesData.idleDimStrength.collectAsState(initial = LauncherDefaults.IDLE_DIM_STRENGTH)
    val idleDimTimeoutSeconds by preferencesData.idleDimTimeoutSeconds.collectAsState(initial = 30)

    ListItem(
        selected = false,
        onClick = {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            try { context.startActivity(intent) }
            catch (e: Exception) {
                try { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (e2: Exception) {}
            }
        },
        scale = ListItemDefaults.scale(focusedScale = 1.01f),
        colors = themedListItemColors(),
        headlineContent = { Text(stringResource(R.string.preferences_default_launcher)) },
        supportingContent = { Text(if (isDefaultLauncher) "Currently active" else "Open system settings") },
        trailingContent = { if (isDefaultLauncher) Text("✔") }
    )

    // Manual escape hatch for TV boxes that report display density inconsistently — the same dp
    // values in this app can render at visibly different physical sizes on different hardware,
    // which isn't something the app can detect or correct for automatically.
    FloatValueSelector(
        text = stringResource(R.string.preferences_ui_scale),
        value = uiScale,
        onValueChange = { coroutineScope.launch { preferencesData.setUiScale(it) } },
        range = 0.7f..1.3f
    )

    PreferenceRow(stringResource(R.string.preferences_navigation_sound), checked = navigationSoundEnabled) {
        coroutineScope.launch { preferencesData.setNavigationSoundEnabled(it) }
    }

    PreferenceRow(stringResource(R.string.preferences_dim_when_idle), checked = idleDimEnabled) {
        coroutineScope.launch { preferencesData.setIdleDimEnabled(it) }
    }

    if (idleDimEnabled) {
        SteppedValueSelector(
            text = stringResource(R.string.preferences_idle_timeout),
            value = idleDimTimeoutSeconds,
            options = PreferencesData.IDLE_DIM_TIMEOUT_OPTIONS_SECONDS,
            label = { seconds -> if (seconds < 60) "$seconds sec" else "${seconds / 60} min" },
            onValueChange = { coroutineScope.launch { preferencesData.setIdleDimTimeoutSeconds(it) } }
        )
        val strengthLabel = stringResource(R.string.preferences_idle_dim_strength_percent, idleDimStrength)
        SteppedValueSelector(
            text = stringResource(R.string.preferences_idle_dim_strength),
            value = idleDimStrength,
            options = PreferencesData.IDLE_DIM_STRENGTH_OPTIONS,
            label = { strengthLabel },
            onValueChange = { coroutineScope.launch { preferencesData.setIdleDimStrength(it) } }
        )
    }

}
