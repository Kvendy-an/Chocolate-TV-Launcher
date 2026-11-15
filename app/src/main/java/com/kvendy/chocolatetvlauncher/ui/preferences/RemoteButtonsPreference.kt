package com.kvendy.chocolatetvlauncher.ui.preferences

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.remote.RemoteAction
import com.kvendy.chocolatetvlauncher.remote.RemoteButtons
import com.kvendy.chocolatetvlauncher.remote.RemoteMapping
import com.kvendy.chocolatetvlauncher.remote.RemoteMappings
import com.kvendy.chocolatetvlauncher.remote.RemoteScope
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import com.kvendy.chocolatetvlauncher.ui.components.SectionDivider
import com.kvendy.chocolatetvlauncher.ui.components.SmallScaleButton
import com.kvendy.chocolatetvlauncher.ui.components.TvSearchField
import com.kvendy.chocolatetvlauncher.ui.components.themedListItemColors
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import com.kvendy.chocolatetvlauncher.ui.util.openAppInfo
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun RemoteButtonsPreference(apps: List<AppInfo>) {
    val context = LocalContext.current
    val store = remember(context) { RemoteMappings(context) }
    val coroutineScope = rememberCoroutineScope()
    val connected by RemoteButtons.connected.collectAsState()
    val overrideHome by store.overrideHome.collectAsState(initial = false)
    val mappings by store.mappings.collectAsState(initial = emptyMap())
    val sortedMappings = remember(mappings) { mappings.values.sortedBy { it.keyCode } }
    val appLabels = remember(apps) { apps.associate { it.packageName to it.label } }
    var learning by remember { mutableStateOf(false) }
    var showAdbSetup by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(connected) {
        if (!connected) learning = false
    }

    PreferenceList {
        item {
            RemoteSettingItem(
                title = stringResource(if (connected) R.string.remote_access_enabled else R.string.remote_enable_access),
                summary = stringResource(if (connected) R.string.remote_manage_access else R.string.remote_access_explanation),
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, R.string.remote_action_unavailable, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        if (connected) {
            item {
                PreferenceRow(
                    text = stringResource(R.string.remote_override_home),
                    checked = overrideHome,
                    summary = stringResource(R.string.remote_override_home_summary)
                ) { enabled -> coroutineScope.launch { store.setOverrideHome(enabled) } }
            }
            item {
                RemoteSettingItem(
                    title = stringResource(R.string.remote_learn),
                    summary = stringResource(R.string.remote_learn_summary),
                    onClick = { learning = true }
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                RemoteSettingItem(
                    title = stringResource(R.string.remote_adb_setup),
                    summary = stringResource(R.string.remote_adb_setup_summary),
                    onClick = { showAdbSetup = true }
                )
            }
            item {
                RemoteSettingItem(
                    title = stringResource(R.string.remote_open_app_info),
                    summary = stringResource(R.string.remote_restricted_settings_hint),
                    onClick = {
                        try {
                            context.openAppInfo(context.packageName)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, R.string.remote_action_unavailable, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
        if (sortedMappings.isNotEmpty()) {
            item(key = "learned_buttons") {
                Column {
                    SectionDivider()
                    Text(
                        text = stringResource(R.string.remote_learned_buttons),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        items(sortedMappings, key = { it.keyCode }) { mapping ->
            val action = mappingActionLabel(mapping.action, mapping.packageName, appLabels)
            RemoteSettingItem(
                title = buttonLabel(mapping.keyCode),
                summary = stringResource(R.string.remote_mapping_summary, action, stringResource(mapping.scope.title)),
                onClick = { editingKey = mapping.keyCode }
            )
        }
    }

    if (showAdbSetup) RemoteAdbSetupDialog(onDismiss = { showAdbSetup = false })

    if (learning) {
        LearnButtonDialog(
            onLearned = {
                learning = false
                editingKey = it
            },
            onDismiss = { learning = false }
        )
    }

    editingKey?.let { keyCode ->
        RemoteMappingDialog(
            keyCode = keyCode,
            existing = mappings[keyCode],
            apps = apps,
            appLabels = appLabels,
            onSave = {
                coroutineScope.launch { store.save(it) }
                editingKey = null
            },
            onRemove = {
                coroutineScope.launch { store.remove(keyCode) }
                editingKey = null
            },
            onDismiss = { editingKey = null }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LearnButtonDialog(onLearned: (Int) -> Unit, onDismiss: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    DisposableEffect(Unit) {
        RemoteButtons.learnButton = onLearned
        onDispose {
            if (RemoteButtons.learnButton === onLearned) RemoteButtons.learnButton = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.width(440.dp)) {
            Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(stringResource(R.string.remote_press_button), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.remote_protected_buttons), color = MaterialTheme.colorScheme.onSurfaceVariant)
                SmallScaleButton(onClick = onDismiss, modifier = Modifier.focusRequester(cancelFocus)) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
        LaunchedEffect(Unit) { cancelFocus.requestFocus() }
    }
}

private enum class MappingEditorPage { Mapping, Actions, Apps }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RemoteMappingDialog(
    keyCode: Int,
    existing: RemoteMapping?,
    apps: List<AppInfo>,
    appLabels: Map<String, String>,
    onSave: (RemoteMapping) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var page by remember { mutableStateOf(MappingEditorPage.Mapping) }
    var action by remember { mutableStateOf(existing?.action) }
    var packageName by remember { mutableStateOf(existing?.packageName) }
    var everywhere by remember { mutableStateOf(existing?.scope == RemoteScope.Everywhere) }
    var query by remember { mutableStateOf("") }
    val firstFocus = remember(page) { FocusRequester() }
    val saveFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }

    fun navigateBack() {
        when (page) {
            MappingEditorPage.Mapping -> onDismiss()
            MappingEditorPage.Actions -> page = MappingEditorPage.Mapping
            MappingEditorPage.Apps -> page = MappingEditorPage.Actions
        }
    }

    Dialog(onDismissRequest = ::navigateBack) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.width(460.dp)) {
            Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                when (page) {
                    MappingEditorPage.Mapping -> {
                        Text(buttonLabel(keyCode), style = MaterialTheme.typography.titleLarge)
                        RemoteSettingItem(
                            title = stringResource(R.string.remote_action),
                            summary = mappingActionLabel(action, packageName, appLabels),
                            modifier = Modifier.focusRequester(firstFocus),
                            onClick = { page = MappingEditorPage.Actions }
                        )
                        PreferenceRow(
                            text = stringResource(R.string.remote_everywhere),
                            checked = everywhere,
                            modifier = Modifier.focusProperties {
                                down = if (action != null) saveFocus else cancelFocus
                            }
                        ) { everywhere = it }
                        Text(
                            stringResource(if (everywhere) R.string.remote_scope_everywhere_hint else R.string.remote_scope_launcher_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            SmallScaleButton(enabled = action != null, modifier = Modifier.focusRequester(saveFocus), onClick = {
                                val chosen = action ?: return@SmallScaleButton
                                val scope = if (everywhere) RemoteScope.Everywhere else RemoteScope.LauncherOnly
                                onSave(RemoteMapping(keyCode, chosen, scope, packageName))
                            }) { Text(stringResource(R.string.remote_save)) }
                            SmallScaleButton(onClick = onDismiss, modifier = Modifier.focusRequester(cancelFocus)) {
                                Text(stringResource(android.R.string.cancel))
                            }
                            if (existing != null) {
                                SmallScaleButton(onClick = onRemove) { Text(stringResource(R.string.remote_remove)) }
                            }
                        }
                    }
                    MappingEditorPage.Actions -> {
                        Text(stringResource(R.string.remote_choose_action), style = MaterialTheme.typography.titleLarge)
                        val options = remember { RemoteAction.entries.filter { it.isSupported } }
                        LazyColumn(Modifier.heightIn(max = 280.dp)) {
                            itemsIndexed(options, key = { _, option -> option.id }) { index, option ->
                                RemoteSettingItem(
                                    title = stringResource(option.title),
                                    modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                                    onClick = {
                                        if (option == RemoteAction.LaunchApp) {
                                            query = ""
                                            page = MappingEditorPage.Apps
                                        } else {
                                            action = option
                                            packageName = null
                                            page = MappingEditorPage.Mapping
                                        }
                                    }
                                )
                            }
                        }
                    }
                    MappingEditorPage.Apps -> {
                        val filteredApps = remember(query, apps) {
                            if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
                        }
                        val appFocus = remember { FocusRequester() }
                        val listState = rememberLazyListState()
                        var submitSearch by remember { mutableStateOf(false) }
                        Text(stringResource(R.string.remote_launch_app), style = MaterialTheme.typography.titleLarge)
                        TvSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            modifier = Modifier.focusRequester(firstFocus),
                            onSearch = { submitSearch = filteredApps.isNotEmpty() }
                        )
                        LazyColumn(Modifier.heightIn(max = 280.dp), state = listState) {
                            if (filteredApps.isEmpty()) {
                                item {
                                    Text(stringResource(R.string.search_no_results), modifier = Modifier.padding(Spacing.Small))
                                }
                            }
                            itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                                RemoteSettingItem(
                                    title = app.label,
                                    modifier = if (index == 0) Modifier.focusRequester(appFocus) else Modifier,
                                    onClick = {
                                        action = RemoteAction.LaunchApp
                                        packageName = app.packageName
                                        page = MappingEditorPage.Mapping
                                    }
                                )
                            }
                        }
                        LaunchedEffect(submitSearch) {
                            if (submitSearch) {
                                listState.scrollToItem(0)
                                appFocus.requestFocus()
                                submitSearch = false
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(page) { firstFocus.requestFocus() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RemoteSettingItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null
) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        scale = ListItemDefaults.scale(focusedScale = 1f),
        colors = themedListItemColors()
    )
}

@Composable
private fun buttonLabel(keyCode: Int): String {
    val name = KeyEvent.keyCodeToString(keyCode)
    if (!name.startsWith("KEYCODE_")) return stringResource(R.string.remote_numbered_key, keyCode)
    return name.removePrefix("KEYCODE_").split('_').joinToString(" ") { word ->
        word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}

@Composable
private fun mappingActionLabel(action: RemoteAction?, packageName: String?, appLabels: Map<String, String>): String {
    return when (action) {
        null -> stringResource(R.string.remote_choose_action)
        RemoteAction.LaunchApp -> appLabels[packageName] ?: packageName ?: stringResource(action.title)
        else -> stringResource(action.title)
    }
}
