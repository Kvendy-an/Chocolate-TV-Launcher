package com.kvendy.chocolatetvlauncher.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Shared spec for the `animateColorAsState` every screen uses to cross-fade [ambientGlow]'s color
 * as focus moves between tiles — the default Color spring settled quickly enough that scrolling
 * fast through several tiles read as the glow popping between colors rather than drifting through
 * them. A slower, explicit tween reads as a smooth, ambient color drift instead, matching this
 * effect's own "mood lighting" intent rather than a highlight that's meant to track focus tightly.
 */
val AmbientGlowColorSpec: AnimationSpec<Color> = tween(durationMillis = 600)

/** Where [Modifier.ambientGlow] anchors its radial gradient, user-configurable in Preferences. */
enum class GlowPosition(val label: String) {
    TopRight("Top Right"),
    TopLeft("Top Left"),
    Center("Center"),
    Bottom("Bottom");

    companion object {
        fun fromId(id: String): GlowPosition = entries.find { it.name.lowercase() == id } ?: TopRight
    }
}

val GlowPosition.id: String get() = name.lowercase()

/**
 * Draws a radial gradient anchored to [position], tinted by [color].
 * Used to tint the background with the dominant color of the currently focused tile.
 */
fun Modifier.ambientGlow(
    color: Color,
    alpha: Float = 0.3f,
    radiusFraction: Float = 1.1f,
    position: GlowPosition = GlowPosition.TopRight
): Modifier = ambientGlow({ color }, alpha, radiusFraction, position)

/** Read animated color in the draw phase, without recomposing the screen for each frame. */
fun Modifier.ambientGlow(
    color: () -> Color,
    alpha: Float = 0.3f,
    radiusFraction: Float = 1.1f,
    position: GlowPosition = GlowPosition.TopRight
): Modifier = drawBehind {
    val tint = color()
    val radius = size.width * radiusFraction
    val center = when (position) {
        GlowPosition.TopRight -> Offset(size.width, 0f)
        GlowPosition.TopLeft -> Offset(0f, 0f)
        GlowPosition.Center -> Offset(size.width / 2f, size.height / 2f)
        GlowPosition.Bottom -> Offset(size.width / 2f, size.height)
    }
    // A plain 2-stop gradient falls off linearly, which reads as a visible ring where it fades
    // past roughly the midpoint — these extra in-between stops (eased: each step fades faster
    // than the last) spread the same drop-off across more of the radius, so it dissolves into
    // the background gradually instead of tapering off gradually then stopping conspicuously.
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to tint.copy(alpha = alpha),
                0.25f to tint.copy(alpha = alpha * 0.65f),
                0.5f to tint.copy(alpha = alpha * 0.35f),
                0.75f to tint.copy(alpha = alpha * 0.12f),
                1.0f to Color.Transparent
            ),
            center = center,
            radius = radius
        )
    )
}
