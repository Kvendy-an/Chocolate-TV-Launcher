package com.kvendy.chocolatetvlauncher.ui.home.tiles

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.LocalImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.kvendy.chocolatetvlauncher.AppImageModel
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.ColorCache
import com.kvendy.chocolatetvlauncher.data.CustomImageCache
import com.kvendy.chocolatetvlauncher.data.HomeGridItem
import com.kvendy.chocolatetvlauncher.data.calculateDominantColor
import com.kvendy.chocolatetvlauncher.ui.components.focusGlow
import com.kvendy.chocolatetvlauncher.ui.components.tvClickable
import com.kvendy.chocolatetvlauncher.ui.components.TileLabel
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A folder tile: a 2x2 preview of its first few apps' icons inside a tinted container, plus a
 * name label below (blank when the folder is unnamed). Mirrors [AppTile]'s focus/glow/label
 * conventions so it reads as the same tile family in the grid.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderTile(
    modifier: Modifier = Modifier,
    folder: HomeGridItem.FolderItem,
    apps: List<AppInfo>,
    iconCornerRadius: Float,
    showAppLabels: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isMoving: Boolean,
    onFocused: (Color) -> Unit = {},
    showGlow: Boolean = true,
    glowRadius: Float = 40f,
    labelScale: Float = 1f,
    renderAsSquare: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageLoader = LocalImageLoader.current

    val previewApps = remember(apps) { apps.take(4) }
    // Separate key space from AppTile's per-package entries: derived from the folder's own id,
    // not any single member app, so it survives members being added/removed/reordered.
    val colorCacheKey = "folder:${folder.id}"
    val dominantColor = ColorCache.getColor(colorCacheKey) ?: Color.Transparent
    val fallbackColor = MaterialTheme.colorScheme.surfaceVariant

    LaunchedEffect(isFocused, previewApps) {
        val firstApp = previewApps.firstOrNull()
        if (isFocused && firstApp != null && ColorCache.getColor(colorCacheKey) == null) {
            launch(Dispatchers.IO) {
                val colorRequest = ImageRequest.Builder(context)
                    .data(AppImageModel(appInfo = firstApp, isBanner = false))
                    .allowHardware(false)
                    .size(100, 100)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .build()
                val result = imageLoader.execute(colorRequest)
                if (result.drawable != null) {
                    val color = calculateDominantColor(result.drawable!!, fallbackColor)
                    ColorCache.setColor(colorCacheKey, color)
                    onFocused(color)
                }
            }
        } else if (isFocused && dominantColor != Color.Transparent) {
            onFocused(dominantColor)
        }
    }

    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "Scale")
    val glowAlpha by animateFloatAsState(if (isFocused && dominantColor != Color.Transparent) 1f else 0f, label = "Glow Alpha")
    val borderColor by animateColorAsState(if (isMoving) MaterialTheme.colorScheme.primary else Color.Transparent, label = "Border Color")
    Column(
        modifier = modifier
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused && dominantColor != Color.Transparent) {
                    onFocused(dominantColor)
                }
            }
            .tvClickable(onLongClick = onLongClick, onClick = onClick)
            .zIndex(if (isFocused) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (renderAsSquare) 1f else 16f / 9f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .focusGlow(
                    color = dominantColor,
                    alpha = { glowAlpha },
                    radiusPx = glowRadius,
                    cornerRadiusDp = iconCornerRadius,
                    enabled = showGlow
                )
                .clip(RoundedCornerShape(iconCornerRadius.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(BorderStroke(4.dp, borderColor), RoundedCornerShape(iconCornerRadius.dp))
                .padding(Spacing.Small),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
            ) {
                for (rowStart in 0 until 4 step 2) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
                    ) {
                        for (col in 0..1) {
                            val previewApp = previewApps.getOrNull(rowStart + col)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                if (previewApp != null) {
                                    // Mirrors AppTile's effectiveIsBanner: a banner-mode folder
                                    // (renderAsSquare == false) should preview its apps' actual
                                    // banners, same as the grid outside it, not force square icons
                                    // regardless of the folder's own shape.
                                    val customBannerVersion = CustomImageCache.bannerVersion[previewApp.packageName]
                                    val effectiveIsBanner = !renderAsSquare && (previewApp.hasBanner || customBannerVersion != null)
                                    Image(
                                        painter = rememberAsyncImagePainter(AppImageModel(appInfo = previewApp, isBanner = effectiveIsBanner)),
                                        contentDescription = previewApp.label,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape((iconCornerRadius / 2).dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAppLabels) TileLabel(folder.name?.takeIf { it.isNotBlank() } ?: "Folder", isFocused, labelScale)
    }
}
