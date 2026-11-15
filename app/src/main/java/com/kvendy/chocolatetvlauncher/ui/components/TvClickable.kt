package com.kvendy.chocolatetvlauncher.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val confirmKeyCodes = setOf(
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER
)

/**
 * Focusable + DPAD-center-clickable modifier for TV remotes, with an optional hold-to-trigger
 * long click. Pass `onLongClick = null` to disable long-click handling entirely so [onClick]
 * fires immediately on key-up instead of waiting out [longPressTimeoutMillis].
 */
fun Modifier.tvClickable(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    longPressTimeoutMillis: Long = 500L,
    onClick: () -> Unit
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    var pressJob by remember { mutableStateOf<Job?>(null) }
    var longPressTriggered by remember { mutableStateOf(false) }
    // Distinct from `pressJob == null`: that's also true for a node that never saw a KeyDown at
    // all. If a long click swaps this tile out mid-press (e.g. merging into a folder changes its
    // identity), focus falls back to some other node, and the physical key-up that follows lands
    // there instead — without this guard that node reads its own untouched `pressJob == null` as
    // "quick tap" and fires onClick for a press it never received.
    var keyDownSeen by remember { mutableStateOf(false) }

    this
        .onKeyEvent { keyEvent ->
            if (!enabled || keyEvent.key.nativeKeyCode !in confirmKeyCodes) return@onKeyEvent false
            when (keyEvent.type) {
                KeyEventType.KeyDown -> {
                    // A held button can keep repeating after focus moves to another tile.
                    if (pressJob == null && keyEvent.nativeKeyEvent.repeatCount == 0) {
                        longPressTriggered = false
                        keyDownSeen = true
                        pressJob = coroutineScope.launch {
                            delay(longPressTimeoutMillis)
                            if (onLongClick != null) {
                                longPressTriggered = true
                                onLongClick()
                            }
                        }
                    }
                    true
                }
                KeyEventType.KeyUp -> {
                    pressJob?.cancel()
                    pressJob = null
                    if (keyDownSeen && !longPressTriggered) onClick()
                    keyDownSeen = false
                    true
                }
                else -> false
            }
        }
        .focusable(enabled = enabled)
}
