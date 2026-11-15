package com.kvendy.chocolatetvlauncher.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Expands this composable's measured width by [inset] on each side beyond what its parent
 * constrains it to, then shifts it left by [inset] so it renders wider without disturbing where
 * its parent thinks it sits — the size reported back up the tree is unchanged, so siblings (e.g.
 * a title above it) keep their normal position.
 *
 * Lets a horizontally scrolling row nested inside a screen that's globally inset (like
 * [com.kvendy.chocolatetvlauncher.ui.components.TvAppGrid]'s content padding) span all the way to
 * the display's true edges instead of clipping [inset] short of them. That inset clip boundary is
 * what made scrolled-off content look like it was being cut prematurely — there was visible empty
 * margin beyond the clip point. Clipping exactly at the real edge needs no extra fade/scale trick.
 */
fun Modifier.fullBleedHorizontal(inset: Dp): Modifier = layout { measurable, constraints ->
    val insetPx = inset.roundToPx()
    val extraPx = insetPx * 2
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extraPx))
    layout(placeable.width - extraPx, placeable.height) {
        placeable.placeRelative(-insetPx, 0)
    }
}
