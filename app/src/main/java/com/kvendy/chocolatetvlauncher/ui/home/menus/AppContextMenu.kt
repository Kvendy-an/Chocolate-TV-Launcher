package com.kvendy.chocolatetvlauncher.ui.home.menus

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.CustomImageCache
import com.kvendy.chocolatetvlauncher.ui.components.themedButtonColors
import com.kvendy.chocolatetvlauncher.ui.components.themedButtonScale
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppContextMenu(
    app: AppInfo,
    onDismiss: () -> Unit,
    onReorder: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    showHide: Boolean = true,
    reorderEnabled: Boolean = true,
    // Null hides the button entirely — callers where a plain tap already opens the app (e.g. the
    // home grid, or a folder's contents dialog where a tap also opens it) don't need a redundant
    // menu entry for it. Manage Apps in Preferences is the one place that DOES need this: tapping
    // there toggles hide/show instead of launching, so this is the only way to open an app from
    // that screen.
    onOpen: (() -> Unit)? = null,
    // Non-null only when this menu was opened for an app inside a folder's contents dialog.
    onRemoveFromFolder: (() -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Reactive reads so the Reset buttons appear/disappear immediately after a change, without
    // needing to reopen this menu.
    val hasCustomIcon = CustomImageCache.hasCustomIcon(app.packageName)
    val hasCustomBanner = CustomImageCache.hasCustomBanner(app.packageName)
    val background = CustomImageCache.tileBackground(app.packageName)
    var showBackgroundPicker by remember(app.packageName) { mutableStateOf(false) }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri)?.let { input ->
                        CustomImageCache.setCustomIcon(context, app.packageName, input)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(selectedUri)?.let { input ->
                        CustomImageCache.setCustomBanner(context, app.packageName, input)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = Spacing.Small)
                )

                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .alpha(0f)
                        .focusRequester(focusRequester)
                        .focusable()
                )

                if (onOpen != null) {
                    Button(
                        onClick = onOpen,
                        colors = themedButtonColors(),
                        scale = themedButtonScale(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open") }
                }

                if (reorderEnabled) {
                    Button(
                        onClick = onReorder,
                        colors = themedButtonColors(),
                        scale = themedButtonScale(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Edit Mode") }
                }

                Button(
                    onClick = onHide,
                    colors = themedButtonColors(),
                    scale = themedButtonScale(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (showHide) "Hide" else "Unhide") }

                Button(
                    onClick = { iconPickerLauncher.launch("image/*") },
                    colors = themedButtonColors(),
                    scale = themedButtonScale(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Change Icon (1:1)") }

                if (hasCustomIcon) {
                    Button(
                        onClick = { coroutineScope.launch(Dispatchers.IO) { CustomImageCache.clearCustomIcon(context, app.packageName) } },
                        colors = themedButtonColors(),
                        scale = themedButtonScale(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Reset Icon") }
                }

                Button(
                    onClick = { bannerPickerLauncher.launch("image/*") },
                    colors = themedButtonColors(),
                    scale = themedButtonScale(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Change Banner (16:9)") }

                if (hasCustomBanner) {
                    Button(
                        onClick = { coroutineScope.launch(Dispatchers.IO) { CustomImageCache.clearCustomBanner(context, app.packageName) } },
                        colors = themedButtonColors(),
                        scale = themedButtonScale(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Reset Banner") }
                }

                Button(
                    onClick = { showBackgroundPicker = true },
                    colors = themedButtonColors(),
                    scale = themedButtonScale(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.tile_background)) }

                Button(
                    onClick = onAppInfo,
                    colors = themedButtonColors(),
                    scale = themedButtonScale(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("App Info") }

                Button(
                    onClick = onUninstall,
                    colors = themedButtonColors(),
                    scale = themedButtonScale(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Uninstall") }

                if (onRemoveFromFolder != null) {
                    Button(
                        onClick = onRemoveFromFolder,
                        colors = themedButtonColors(),
                        scale = themedButtonScale(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Remove from Folder") }
                }
            }
        }
    }

    if (showBackgroundPicker) {
        TileBackgroundPicker(
            selected = background,
            onSelect = {
                coroutineScope.launch { CustomImageCache.setTileBackground(context, app.packageName, it) }
                showBackgroundPicker = false
            },
            onDismiss = { showBackgroundPicker = false }
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}