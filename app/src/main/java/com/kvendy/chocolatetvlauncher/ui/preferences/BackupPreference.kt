package com.kvendy.chocolatetvlauncher.ui.preferences

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.preferences.core.Preferences
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.BackupCategory
import com.kvendy.chocolatetvlauncher.data.PreferenceBackupFormat
import com.kvendy.chocolatetvlauncher.data.PreferenceBackupFile
import com.kvendy.chocolatetvlauncher.data.PreferenceBackups
import com.kvendy.chocolatetvlauncher.ui.components.themedListItemColors
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import kotlinx.coroutines.CancellationException
import com.kvendy.chocolatetvlauncher.ui.components.PreferenceRow
import com.kvendy.chocolatetvlauncher.ui.components.SmallScaleButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun BackupPreference() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val backups = remember(context) { PreferenceBackups(context) }
    val scope = rememberCoroutineScope()
    val exportError = stringResource(R.string.preferences_backup_export_error)
    val importError = stringResource(R.string.preferences_backup_import_error)
    val imported = stringResource(R.string.preferences_backup_imported)
    val exported = stringResource(R.string.preferences_backup_exported)
    val pickerError = stringResource(R.string.preferences_backup_picker_unavailable)
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf<List<PreferenceBackupFile>?>(null) }

    var choosingExport by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Preferences?>(null) }
    var exportSelection by remember { mutableStateOf(emptySet<BackupCategory>()) }

    fun runBackupOperation(errorMessage: String, operation: suspend () -> Unit) {
        if (busy) return
        busy = true
        status = null
        scope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                status = errorMessage
            } finally {
                busy = false
            }
        }
    }

    fun importBackup(uri: Uri) {
        files = null
        runBackupOperation(importError) {
            pendingImport = backups.read(uri)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let(::importBackup)
    }
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                runBackupOperation(exportError) {
                    backups.exportTo(uri, exportSelection)
                    status = exported
                }
            }
        }
    }

    fun browse() {
        files = null
        try {
            picker.launch(Intent.createChooser(backupDocumentIntent(Intent.ACTION_GET_CONTENT), null))
        } catch (_: ActivityNotFoundException) {
            try {
                picker.launch(backupDocumentIntent(Intent.ACTION_OPEN_DOCUMENT))
            } catch (_: ActivityNotFoundException) {
                status = pickerError
            }
        }
    }

    Text(stringResource(R.string.preferences_backup_scope), style = MaterialTheme.typography.bodyMedium)
    BackupAction(
        title = stringResource(R.string.preferences_export),
        summary = stringResource(R.string.preferences_export_summary),
        enabled = !busy,
        onClick = { choosingExport = true }
    )
    if (choosingExport) {
        BackupCategoriesDialog(
            title = stringResource(R.string.preferences_export),
            categories = BackupCategory.entries.toSet(),
            onDismiss = { choosingExport = false }
        ) { selected ->
            choosingExport = false
            exportSelection = selected
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runBackupOperation(exportError) {
                    val file = backups.exportToDownloads(selected)
                    status = resources.getString(R.string.preferences_backup_saved_in_downloads, file.name)
                }
            } else {
                try {
                    createFile.launch(backupDocumentIntent(Intent.ACTION_CREATE_DOCUMENT).putExtra(
                        Intent.EXTRA_TITLE, PreferenceBackups.newFileName()
                    ))
                } catch (_: ActivityNotFoundException) {
                    status = pickerError
                }
            }
        }
    }
    pendingImport?.let { backup ->
        BackupCategoriesDialog(
            title = stringResource(R.string.preferences_import),
            categories = PreferenceBackupFormat.categories(backup),
            onDismiss = { pendingImport = null }
        ) { selected ->
            pendingImport = null
            runBackupOperation(importError) {
                backups.restore(backup, selected)
                status = imported
            }
        }
    }
    BackupAction(
        title = stringResource(R.string.preferences_import),
        summary = stringResource(R.string.preferences_import_summary),
        enabled = !busy,
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runBackupOperation(importError) {
                    files = backups.downloads()
                }
            } else {
                browse()
            }
        }
    )
    status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    if (busy) Text(stringResource(R.string.preferences_backup_working), style = MaterialTheme.typography.bodyMedium)

    files?.let { downloads ->
        DownloadBackupsDialog(
            files = downloads,
            onSelect = { importBackup(it.uri) },
            onBrowse = ::browse,
            onDismiss = { files = null }
        )
    }
}

private fun backupDocumentIntent(action: String) = Intent(action).apply {
    type = if (action == Intent.ACTION_CREATE_DOCUMENT) PreferenceBackups.mimeType else "*/*"
    addCategory(Intent.CATEGORY_OPENABLE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        putExtra(
            DocumentsContract.EXTRA_INITIAL_URI,
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DownloadBackupsDialog(
    files: List<PreferenceBackupFile>,
    onSelect: (PreferenceBackupFile) -> Unit,
    onBrowse: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.width(500.dp)) {
            Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(stringResource(R.string.preferences_import), style = MaterialTheme.typography.titleLarge)
                BackupAction(
                    title = stringResource(R.string.preferences_backup_browse),
                    modifier = Modifier.focusRequester(firstFocus),
                    onClick = onBrowse
                )
                if (files.isEmpty()) {
                    Text(stringResource(R.string.preferences_backup_no_downloads), style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(files, key = { it.uri.toString() }) { file ->
                            BackupAction(title = file.name, onClick = { onSelect(file) })
                        }
                    }
                }
            }
        }
        LaunchedEffect(Unit) { firstFocus.requestFocus() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BackupAction(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        selected = false,
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        scale = ListItemDefaults.scale(focusedScale = 1f),
        colors = themedListItemColors()
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BackupCategoriesDialog(
    title: String,
    categories: Set<BackupCategory>,
    onDismiss: () -> Unit,
    onConfirm: (Set<BackupCategory>) -> Unit
) {
    var selected by remember(categories) { mutableStateOf(categories) }
    val firstFocus = remember { FocusRequester() }
    val allSelected = selected == categories
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.width(460.dp)) {
            Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                BackupAction(
                    title = stringResource(if (allSelected) R.string.backup_clear_all else R.string.backup_select_all),
                    modifier = Modifier.focusRequester(firstFocus),
                    onClick = { selected = if (allSelected) emptySet() else categories }
                )
                LazyColumn(Modifier.heightIn(max = 230.dp)) {
                    items(categories.toList(), key = { it.name }) { category ->
                        val label = when (category) {
                            BackupCategory.Preferences -> R.string.backup_general
                            BackupCategory.GridOrder -> R.string.backup_grid
                            BackupCategory.HiddenApps -> R.string.backup_hidden
                            BackupCategory.TileColors -> R.string.backup_colors
                            BackupCategory.RemoteActions -> R.string.backup_remote
                        }
                        PreferenceRow(text = stringResource(label), checked = category in selected) { checked ->
                            selected = if (checked) selected + category else selected - category
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    SmallScaleButton(enabled = selected.isNotEmpty(), onClick = { onConfirm(selected) }) { Text(title) }
                    SmallScaleButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                }
            }
        }
        LaunchedEffect(Unit) { firstFocus.requestFocus() }
    }
}
