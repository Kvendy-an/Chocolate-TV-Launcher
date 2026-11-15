package com.kvendy.chocolatetvlauncher.ui.home.tiles

import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.LocalImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.kvendy.chocolatetvlauncher.AppImageModel
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.ColorCache
import com.kvendy.chocolatetvlauncher.data.RecommendedContent
import com.kvendy.chocolatetvlauncher.data.calculateDominantColor
import com.kvendy.chocolatetvlauncher.ui.components.focusGlow
import com.kvendy.chocolatetvlauncher.ui.components.tvClickable
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** A poster-art card for a piece of recommended content (see [RecommendedContent]). */
@Composable
fun ContentCard(
    content: RecommendedContent,
    sourceApp: AppInfo?,
    cornerRadius: Float,
    onClick: (RecommendedContent) -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (Color) -> Unit = {},
    showGlow: Boolean = true,
    glowRadius: Float = 40f,
    labelScale: Float = 1f
) {
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageLoader = LocalImageLoader.current
    // Separate key space from ColorCache's app-packageName entries (AppTile) since this is keyed
    // by content id, not a package name; read reactively rather than snapshotting into local
    // state for the same reason AppTile does (see its comment) — avoids a stale copy permanently
    // showing the black-RGB "unresolved" placeholder if another instance resolves it first.
    val colorCacheKey = "content:${content.id}"
    val dominantColor = ColorCache.getColor(colorCacheKey) ?: Color.Transparent
    val fallbackColor = MaterialTheme.colorScheme.surfaceVariant

    LaunchedEffect(isFocused) {
        if (isFocused && content.posterArtUri != null && ColorCache.getColor(colorCacheKey) == null) {
            launch(Dispatchers.IO) {
                val colorRequest = ImageRequest.Builder(context)
                    .data(content.posterArtUri)
                    .allowHardware(false)
                    .size(100, 100)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .build()
                val result = imageLoader.execute(colorRequest)
                result.drawable?.let { drawable ->
                    val color = calculateDominantColor(drawable, fallbackColor)
                    ColorCache.setColor(colorCacheKey, color)
                    onFocused(color)
                }
            }
        } else if (isFocused && dominantColor != Color.Transparent) {
            // Same "already resolved" path AppTile has — without this, the ambient screen glow
            // never updates when moving focus onto a card whose color was cached by an earlier
            // instance of this same card (e.g. scrolling the row back and forth).
            onFocused(dominantColor)
        }
    }

    val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, label = "Scale")
    val glowAlpha by animateFloatAsState(if (isFocused && dominantColor != Color.Transparent) 1f else 0f, label = "Glow Alpha")
    val baseTitleStyle = MaterialTheme.typography.bodyMedium
    val titleStyle = remember(baseTitleStyle, labelScale) {
        baseTitleStyle.copy(fontSize = baseTitleStyle.fontSize * labelScale)
    }
    // Same black placeholder as AppTile, for the same reason: without one, the card is just
    // blank/invisible against the background until Coil finishes fetching the poster.
    val posterImageRequest = remember(content.posterArtUri) {
        ImageRequest.Builder(context)
            .data(content.posterArtUri)
            .crossfade(false)
            .placeholder(ColorDrawable(android.graphics.Color.BLACK))
            .build()
    }

    Column(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(onClick = { onClick(content) }),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .focusGlow(
                    color = dominantColor,
                    alpha = { glowAlpha },
                    radiusPx = glowRadius,
                    cornerRadiusDp = cornerRadius,
                    enabled = showGlow
                )
            // No .clip() here — it's on the poster Box below instead, so the source-app badge
            // (a sibling, not a descendant of that clip) can sit right at the corner without a
            // high corner radius rounding it away too, same as the card's own rounding does.
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius.dp))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(posterImageRequest),
                    contentDescription = content.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (sourceApp != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.ExtraSmall)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(AppImageModel(appInfo = sourceApp, isBanner = false)),
                        contentDescription = sourceApp.label,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .padding(top = Spacing.Small),
        ) {
            Text(
                text = content.title,
                style = titleStyle,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                // See AppTile's identical comment: WhileFocused needs a local focus target, which
                // this Text doesn't have (the real one is the Column above). Driving iterations
                // off our own isFocused gets the same effect reliably.
                modifier = Modifier.basicMarquee(iterations = if (isFocused) Int.MAX_VALUE else 0)
            )
        }
    }
}
