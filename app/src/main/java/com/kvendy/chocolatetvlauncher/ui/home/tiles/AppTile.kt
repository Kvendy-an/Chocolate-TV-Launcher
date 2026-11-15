package com.kvendy.chocolatetvlauncher.ui.home.tiles

import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import com.kvendy.chocolatetvlauncher.cacheKey
import com.kvendy.chocolatetvlauncher.loadAppArtworkColor
import com.kvendy.chocolatetvlauncher.data.TileBackground
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.ColorCache
import com.kvendy.chocolatetvlauncher.data.CustomImageCache
import com.kvendy.chocolatetvlauncher.ui.components.focusGlow
import com.kvendy.chocolatetvlauncher.ui.components.tvClickable
import com.kvendy.chocolatetvlauncher.ui.components.TileLabel
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    modifier: Modifier = Modifier,
    app: AppInfo,
    aspectRatio: Float = 16f / 9f,
    renderAsSquare: Boolean = false,
    iconCornerRadius: Float,
    showAppLabels: Boolean,
    onAppClicked: (AppInfo) -> Unit,
    onAppLongClicked: ((AppInfo) -> Unit)? = null,
    isMoving: Boolean,
    onFocused: (Color) -> Unit = {},
    showGlow: Boolean = true,
    glowRadius: Float = 40f,
    labelScale: Float = 1f
) {
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageLoader = LocalImageLoader.current

    // Reading these maps here (not just inside remember{}) is what makes this tile recompose —
    // and therefore reload, since they're also folded into the request below — the moment a
    // custom icon/banner is set or reset elsewhere.
    val customIconVersion = CustomImageCache.iconVersion[app.packageName]
    val customBannerVersion = CustomImageCache.bannerVersion[app.packageName]
    val effectiveIsBanner = !renderAsSquare && (app.hasBanner || customBannerVersion != null)
    // True only when this tile wants a 16:9 banner but the app doesn't have one (real or custom)
    // — genuine square-tile mode already looks right with a plain Crop and isn't affected.
    val isBannerFallback = !renderAsSquare && !effectiveIsBanner
    val background = CustomImageCache.tileBackground(app.packageName)
    val model = remember(app, effectiveIsBanner) { AppImageModel(app, effectiveIsBanner) }
    val artworkKey = model.cacheKey()
    val imageRequest = remember(context, model, artworkKey) {
        ImageRequest.Builder(context)
            .data(model)
            .memoryCacheKey(artworkKey)
            .crossfade(false)
            .placeholder(ColorDrawable(android.graphics.Color.BLACK))
            .build()
    }
    val painter = rememberAsyncImagePainter(imageRequest)
    val usesAutoBackground = background == TileBackground.Auto
    val dominantColor = if (showGlow || usesAutoBackground) ColorCache.getColor(artworkKey) else null
    val fallbackColor = MaterialTheme.colorScheme.surfaceVariant
    val needsColor = (usesAutoBackground || (isFocused && showGlow)) && dominantColor == null
    LaunchedEffect(needsColor, model, artworkKey) {
        if (needsColor) imageLoader.loadAppArtworkColor(context, model, artworkKey, fallbackColor)
    }
    LaunchedEffect(isFocused, showGlow, dominantColor) {
        if (isFocused && showGlow && dominantColor != null) onFocused(dominantColor)
    }
    val backdropColor = if (usesAutoBackground) dominantColor ?: fallbackColor else background.color

    val scale = if (renderAsSquare) {
        animateFloatAsState(if (isFocused) 1.1f else 1f, label = "Scale").value
    } else {
        1f
    }
    val glowAlpha by animateFloatAsState(if (isFocused && dominantColor != null) 1f else 0f, label = "Glow Alpha")
    val shape = RoundedCornerShape(iconCornerRadius.dp)
    val border = when {
        isMoving -> BorderStroke(4.dp, MaterialTheme.colorScheme.primary)
        isFocused && !renderAsSquare -> BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
        else -> null
    }

    Column(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(
                onLongClick = onAppLongClicked?.let { { it(app) } },
                onClick = { onAppClicked(app) }
            )
            .zIndex(if (isFocused) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .then(if (renderAsSquare) Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                } else Modifier)
                .focusGlow(
                    color = dominantColor ?: Color.Transparent,
                    alpha = { glowAlpha },
                    radiusPx = glowRadius,
                    cornerRadiusDp = iconCornerRadius,
                    enabled = showGlow
                )
                .clip(shape)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .then(if (backdropColor != null) Modifier.background(backdropColor) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painter,
                contentDescription = app.label,
                modifier = when {
                    isBannerFallback -> Modifier.fillMaxSize(0.55f)
                    renderAsSquare && backdropColor != null -> Modifier.fillMaxSize(0.84f)
                        .clip(RoundedCornerShape((iconCornerRadius * 0.75f).dp))
                    else -> Modifier.fillMaxSize()
                },
                contentScale = if (renderAsSquare || effectiveIsBanner) ContentScale.Crop else ContentScale.Fit
            )
        }

        if (showAppLabels) TileLabel(app.label, isFocused, labelScale)
    }
}
