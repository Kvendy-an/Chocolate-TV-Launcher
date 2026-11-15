package com.kvendy.chocolatetvlauncher.ui.home

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * A black overlay that fades in once [lastInteractionAt] (an [SystemClock.elapsedRealtime]
 * timestamp, updated from MainActivity's dispatchKeyEvent on every key press) has been stale for
 * [timeoutSeconds]. Doesn't intercept input itself — MainActivity's dispatchKeyEvent already
 * swallows the first key press while dimmed (so it only wakes the screen, instead of also acting
 * on whatever's focused underneath) and flips [isDimmed] back off before this recomposes.
 */
@Composable
fun IdleDimOverlay(
    enabled: Boolean,
    timeoutSeconds: Int,
    strengthPercent: Int,
    lastInteractionAt: Long,
    isDimmed: Boolean,
    onDimmedChange: (Boolean) -> Unit
) {
    // Keyed on every value that should restart the wait: a fresh interaction, the timeout being
    // changed, or the feature being toggled. Sleeps for exactly the remaining time instead of
    // polling on an interval, so this coroutine only ever wakes up once (to dim) per idle period.
    LaunchedEffect(enabled, timeoutSeconds, lastInteractionAt) {
        if (!enabled) {
            if (isDimmed) onDimmedChange(false)
            return@LaunchedEffect
        }
        val timeoutMillis = timeoutSeconds * 1000L
        val remaining = timeoutMillis - (SystemClock.elapsedRealtime() - lastInteractionAt)
        if (remaining <= 0) {
            onDimmedChange(true)
        } else {
            delay(remaining)
            onDimmedChange(true)
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (enabled && isDimmed) strengthPercent / 100f else 0f,
        animationSpec = tween(1500),
        label = "IdleDimAlpha"
    )
    if (alpha > 0f) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha)))
    }
}
