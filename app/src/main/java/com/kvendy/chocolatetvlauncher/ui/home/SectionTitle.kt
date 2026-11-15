package com.kvendy.chocolatetvlauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

@Composable
internal fun SectionTitle(
    text: String,
    icon: ImageVector,
    scale: Float,
    showLabel: Boolean,
    showIcon: Boolean
) {
    if (!showLabel && !showIcon) return

    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            Icon(
                imageVector = icon,
                contentDescription = if (showLabel) null else text,
                modifier = Modifier.size(24.dp * scale)
            )
        }
        if (showLabel) {
            val base = MaterialTheme.typography.headlineSmall
            val style = remember(base, scale) {
                base.copy(fontSize = base.fontSize * scale, lineHeight = base.lineHeight * scale)
            }
            Text(text, style = style)
        }
    }
}
