package com.kvendy.chocolatetvlauncher.ui.preferences

import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.LauncherNotificationListenerService
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import com.kvendy.chocolatetvlauncher.ui.components.themedListItemColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NotificationsPreference(preferencesData: PreferencesData) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val showNotifications by preferencesData.showNotifications.collectAsState(initial = false)
    val showNotificationsOnHome by preferencesData.showNotificationsOnHome.collectAsState(initial = true)

    // Android has no runtime-permission dialog for this — the user has to grant it manually via
    // Settings, so this is re-checked on resume the same way "Set as Default Launcher" is in
    // SystemPreference.kt, since the only way back here is backing out of that settings screen.
    var hasPermission by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = LauncherNotificationListenerService.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Master switch: same shape as Recommendations' — turns off the page/tab and the Home row
    // entirely, not just the row.
    PreferenceRow(stringResource(R.string.preferences_enable_notifications), checked = showNotifications) {
        coroutineScope.launch { preferencesData.setShowNotifications(it) }
    }

    if (showNotifications) {
        if (!hasPermission) {
            ListItem(
                selected = false,
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                scale = ListItemDefaults.scale(focusedScale = 1.01f),
                colors = themedListItemColors(),
                headlineContent = { Text(stringResource(R.string.preferences_notification_access)) },
                supportingContent = { Text("Required so this launcher can read notifications from other apps") },
                trailingContent = { Icon(Icons.Default.Notifications, "Notifications") }
            )
        } else {
            // Narrower than the toggle above — lets someone keep the dedicated Notifications page
            // while decluttering the Home page specifically.
            PreferenceRow(stringResource(R.string.preferences_show_on_home), checked = showNotificationsOnHome) {
                coroutineScope.launch { preferencesData.setShowNotificationsOnHome(it) }
            }
        }
    }
}
