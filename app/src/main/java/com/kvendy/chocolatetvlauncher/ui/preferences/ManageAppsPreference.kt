package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.home.menus.AppContextMenu
import com.kvendy.chocolatetvlauncher.ui.util.launchApp
import com.kvendy.chocolatetvlauncher.ui.util.openAppInfo
import com.kvendy.chocolatetvlauncher.ui.util.requestUninstall
import kotlinx.coroutines.launch

@Composable
fun ManageAppsPreference(preferencesData: PreferencesData, apps: List<AppInfo>) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hiddenApps by preferencesData.hiddenApps.collectAsState(initial = emptySet())
    var contextMenuApp by remember { mutableStateOf<AppInfo?>(null) }

    var query by remember { mutableStateOf("") }
    val filteredApps = remember(query, apps) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    PreferenceList {
        item {
            Text(
                "Press Select to show or hide an app on Home. Hold Select for more options.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        manageAppsList(
            filteredApps = filteredApps,
            query = query,
            onQueryChange = { query = it },
            hiddenApps = hiddenApps,
            onToggleApp = { packageName -> coroutineScope.launch { preferencesData.toggleHiddenApp(packageName) } },
            onShowAll = { coroutineScope.launch { preferencesData.setHiddenApps(emptySet()) } },
            onHideAll = { coroutineScope.launch { preferencesData.setHiddenApps(apps.map { it.packageName }.toSet()) } },
            onLongPressApp = { app -> contextMenuApp = app }
        )
    }

    contextMenuApp?.let { app ->
        val isHidden = app.packageName in hiddenApps
        AppContextMenu(
            app = app,
            onDismiss = { contextMenuApp = null },
            onOpen = {
                context.launchApp(app.packageName)
                contextMenuApp = null
            },
            onReorder = {},
            onHide = {
                coroutineScope.launch {
                    if (isHidden) preferencesData.unhideApp(app.packageName) else preferencesData.hideApp(app.packageName)
                }
                contextMenuApp = null
            },
            onAppInfo = {
                context.openAppInfo(app.packageName)
                contextMenuApp = null
            },
            onUninstall = {
                context.requestUninstall(app.packageName)
                contextMenuApp = null
            },
            showHide = !isHidden,
            reorderEnabled = false
        )
    }
}
