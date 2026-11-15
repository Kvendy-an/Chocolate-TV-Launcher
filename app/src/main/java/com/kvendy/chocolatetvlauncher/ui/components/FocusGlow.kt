package com.kvendy.chocolatetvlauncher.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * Soft ambient blur glow drawn behind a focused tile, tinted by [color]. Shared by AppTile and
 * ContentCard so the focused-tile look stays visually consistent across the app instead of each
 * reimplementing its own highlight treatment (a hard border, etc).
 *
 * @param radiusPx blur radius in raw pixels — matches the existing glowRadius preference, which is
 *   passed straight into [BlurMaskFilter] rather than treated as a dp value.
 * @param cornerRadiusDp corner radius in dp, matching the tile's own clip shape.
 */
fun Modifier.focusGlow(
    color: Color,
    alpha: () -> Float,
    radiusPx: Float,
    cornerRadiusDp: Float,
    enabled: Boolean = true
): Modifier = composed {
    val glowPaint = remember(color, radiusPx) {
        Paint().asFrameworkPaint().apply {
            this.color = color.toArgb()
            maskFilter = BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.OUTER)
        }
    }
    drawBehind {
        val opacity = alpha()
        if (enabled && opacity > 0f) {
            drawIntoCanvas { canvas ->
                // Update alpha without recreating the whole paint object.
                glowPaint.alpha = (opacity * 255).toInt()
                val cornerPx = cornerRadiusDp.dp.toPx()
                canvas.nativeCanvas.drawRoundRect(
                    0f, 0f, size.width, size.height,
                    cornerPx, cornerPx,
                    glowPaint
                )
            }
        }
    }
}
