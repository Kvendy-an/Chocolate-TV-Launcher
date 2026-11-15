package com.kvendy.chocolatetvlauncher.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Reactive holder for [AccentColor.Auto]'s currently-resolved seed color, recomputed by
 * LauncherTheme whenever the wallpaper/gradient background changes. Same
 * mutableStateOf-singleton shape as ColorCache/CustomImageCache: writing here from a background
 * coroutine and reading it inside a composable subscribes to updates directly, without needing a
 * separate event bus.
 */
object AutoAccentColor {
    var seed by mutableStateOf<Color?>(null)
}
