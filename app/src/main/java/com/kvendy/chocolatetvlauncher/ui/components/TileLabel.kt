package com.kvendy.chocolatetvlauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

/** A measured single-line caption; focus changes opacity, never the layout footprint. */
@Composable
fun TileLabel(text: String, focused: Boolean, labelScale: Float = 1f) {
    val alpha by animateFloatAsState(if (focused) 1f else 0f, label = "Tile label")
    val baseStyle = MaterialTheme.typography.bodyLarge
    val style = remember(baseStyle, labelScale) {
        baseStyle.copy(fontSize = baseStyle.fontSize * labelScale)
    }
    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small)
            .graphicsLayer { this.alpha = alpha }
            .basicMarquee(iterations = if (focused) Int.MAX_VALUE else 0)
    )
}
