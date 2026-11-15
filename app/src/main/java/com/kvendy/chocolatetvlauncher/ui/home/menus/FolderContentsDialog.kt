package com.kvendy.chocolatetvlauncher.ui.home.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.HomeGridItem
import com.kvendy.chocolatetvlauncher.ui.components.TvAppGrid
import com.kvendy.chocolatetvlauncher.ui.home.tiles.AppTile
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

/**
 * A folder's contents as a small grid — mirrors the top-level Home grid's own [TvAppGrid]/[AppTile]
 * call (same content padding, same measured caption, same self-drawn focus glow) so it behaves
 * identically instead of carrying its own bespoke layout, with [gridSize] as the one deliberate
 * difference (a folder's grid is its own, separately-sized layout — see "Apps Per Row (Folder)").
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderContentsDialog(
    folder: HomeGridItem.FolderItem,
    apps: List<AppInfo>,
    gridSize: Int,
    iconCornerRadius: Float,
    showAppLabels: Boolean,
    showGlow: Boolean,
    glowRadius: Float,
    labelScale: Float = 1f,
    renderAsSquare: Boolean = false,
    // Package name of the app currently picked up for an in-folder reorder, or null if none —
    // drives the moving-tile border highlight.
    movingPackage: String? = null,
    // See TvAppGrid's own doc comment — keeps focus pinned to the slot being edited instead of
    // letting it follow whichever app a swap just moved out of it. Index is within this folder's
    // own apps list, not the top-level grid.
    focusIndexRequest: Int? = null,
    onDismiss: () -> Unit,
    onAppClicked: (AppInfo) -> Unit,
    onAppLongClicked: (AppInfo) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        // A default (wrap-content) Dialog window is sized tight to the visible card, so a
        // Surface's shape-clip and the window edge itself both cut off the focus glow at high
        // Glow Intensity. Sizing the window to the full screen and centering a plain (non-
        // clipping) background box in it instead gives the glow room to bleed into the dimmed
        // margin around the card rather than being cut off.
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = folder.name?.takeIf { it.isNotBlank() } ?: "Folder",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(560.dp)
                        .padding(bottom = Spacing.Small)
                        .basicMarquee()
                )

                Box(
                    modifier = Modifier
                        .width(560.dp)
                        .height(420.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(Spacing.Medium)
                ) {
                    TvAppGrid(
                        items = apps,
                        gridSize = gridSize,
                        rowSpacing = if (showAppLabels) Spacing.Small else Spacing.Large,
                        itemKey = { it.packageName },
                        modifier = Modifier.fillMaxSize(),
                        focusIndexRequest = focusIndexRequest,
                        // Even on all sides — this only ever protects the true first/last row
                        // regardless of amount (LazyColumn clips its scroll axis with no way to
                        // opt out, and content padding doesn't reserve space at arbitrary scroll
                        // positions), so there's no reason to make it smaller than the horizontal
                        // value already in use.
                        contentPadding = PaddingValues(Spacing.ExtraLarge)
                    ) { app, tileModifier ->
                        AppTile(
                            modifier = tileModifier,
                            app = app,
                            aspectRatio = if (renderAsSquare) 1f else 16f / 9f,
                            renderAsSquare = renderAsSquare,
                            iconCornerRadius = iconCornerRadius,
                            showAppLabels = showAppLabels,
                            onAppClicked = onAppClicked,
                            onAppLongClicked = onAppLongClicked,
                            isMoving = app.packageName == movingPackage,
                            showGlow = showGlow,
                            glowRadius = glowRadius,
                            labelScale = labelScale
                        )
                    }
                }
            }
        }
    }
}
