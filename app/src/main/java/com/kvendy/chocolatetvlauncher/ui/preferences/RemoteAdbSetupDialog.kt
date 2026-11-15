package com.kvendy.chocolatetvlauncher.ui.preferences

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.remote.LocalAdbSetupResult
import com.kvendy.chocolatetvlauncher.remote.allowRestrictedSettings
import com.kvendy.chocolatetvlauncher.ui.components.SmallScaleButton
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun RemoteAdbSetupDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val actionFocus = remember { FocusRequester() }
        var working by remember { mutableStateOf(false) }
        var result by remember { mutableStateOf<LocalAdbSetupResult?>(null) }
        var settingsUnavailable by remember { mutableStateOf(false) }

        fun openSettings(action: String) {
            settingsUnavailable = false
            try {
                context.startActivity(Intent(action))
                if (action == Settings.ACTION_ACCESSIBILITY_SETTINGS) onDismiss()
            } catch (_: ActivityNotFoundException) {
                settingsUnavailable = true
            }
        }

        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.width(475.dp)) {
            Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(stringResource(R.string.remote_adb_setup), style = MaterialTheme.typography.titleLarge)
                val message = when {
                    settingsUnavailable -> R.string.remote_adb_settings_unavailable
                    working -> R.string.remote_adb_waiting
                    result == LocalAdbSetupResult.Allowed -> R.string.remote_adb_allowed
                    result == LocalAdbSetupResult.Unreachable -> R.string.remote_adb_unreachable
                    result == LocalAdbSetupResult.AuthorizationFailed -> R.string.remote_adb_authorization_failed
                    result == LocalAdbSetupResult.PermissionDenied -> R.string.remote_adb_permission_denied
                    else -> R.string.remote_adb_explanation
                }
                Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
                if (!working) {
                    if (result == LocalAdbSetupResult.Allowed) {
                        SmallScaleButton(
                            onClick = { openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
                            modifier = Modifier.focusRequester(actionFocus)
                        ) { Text(stringResource(R.string.remote_enable_access)) }
                    } else {
                        SmallScaleButton(
                            onClick = {
                                settingsUnavailable = false
                                working = true
                                scope.launch {
                                    try {
                                        result = allowRestrictedSettings(context.packageName)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        result = LocalAdbSetupResult.PermissionDenied
                                    } finally {
                                        working = false
                                    }
                                }
                            },
                            modifier = Modifier.focusRequester(actionFocus)
                        ) { Text(stringResource(R.string.remote_adb_allow)) }
                        SmallScaleButton(onClick = { openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) }) {
                            Text(stringResource(R.string.remote_adb_open_developer_options))
                        }
                    }
                }
                SmallScaleButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            }
        }
        LaunchedEffect(working, result) {
            if (!working) actionFocus.requestFocus()
        }
    }
}
