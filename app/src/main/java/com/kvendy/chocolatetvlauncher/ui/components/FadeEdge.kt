package com.kvendy.chocolatetvlauncher.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Draws content into an offscreen layer so it can be masked, required by [edgeFadeVertical]. */
private fun Modifier.offscreenLayerForFade(): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; clip = false }

/** Fades content out at the top and/or bottom edge. Pass 0.dp to skip fading that edge. */
fun Modifier.edgeFadeVertical(topFade: Dp = 0.dp, bottomFade: Dp = 0.dp): Modifier = offscreenLayerForFade().drawWithContent {
    drawContent()
    val topPx = topFade.toPx()
    val bottomPx = bottomFade.toPx()
    val height = size.height
    val stops = buildList {
        add(0f to if (topPx > 0f) Color.Transparent else Color.Black)
        if (topPx > 0f) add((topPx / height).coerceIn(0f, 1f) to Color.Black)
        if (bottomPx > 0f) add(((height - bottomPx) / height).coerceIn(0f, 1f) to Color.Black)
        add(1f to if (bottomPx > 0f) Color.Transparent else Color.Black)
    }
    drawRect(
        brush = Brush.verticalGradient(colorStops = stops.toTypedArray()),
        blendMode = BlendMode.DstIn
    )
}
