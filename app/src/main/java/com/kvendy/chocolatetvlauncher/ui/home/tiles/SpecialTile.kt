package com.kvendy.chocolatetvlauncher.ui.home.tiles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.ui.components.tvClickable
import com.kvendy.chocolatetvlauncher.ui.components.TileLabel
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SpecialTile(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    width: Dp,
    aspectRatio: Float,
    cornerRadius: Float,
    onClick: () -> Unit,
    isSquare: Boolean,
    onFocused: () -> Unit,
    showLabel: Boolean = true,
    labelScale: Float = 1f
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "Scale")
    val border by animateColorAsState(if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent, label = "Border")
    LaunchedEffect(isFocused, onFocused) {
        if (isFocused) onFocused()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(width)
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(cornerRadius.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, border, RoundedCornerShape(cornerRadius.dp)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = icon,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "IconTransition"
            ) { targetIcon ->
                Icon(
                    imageVector = targetIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxSize(0.4f)
                )
            }
        }

        if (showLabel) TileLabel(text, isFocused, labelScale)
    }
}
