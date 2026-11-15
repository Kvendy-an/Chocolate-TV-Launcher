package com.kvendy.chocolatetvlauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.theme.GradientBackground
import java.io.File

/**
 * The wallpaper/gradient layer, rendered once behind whichever page (Home or Recommendations) is
 * currently showing. Used to live inside each page's own composable, but that meant swapping
 * pages tore it down and remounted it from scratch — for the brief gap before the new page's copy
 * drew anything, the plain theme background showed through instead, which in light mode (a
 * near-white surface color) read as a jarring white flash. Hoisting it here keeps the actual
 * background continuously on screen through the swap, so there's nothing to flash to.
 */
@Composable
fun HomeBackground(preferencesData: PreferencesData, modifier: Modifier = Modifier) {
    val wallpaperPath by preferencesData.wallpaperPath.collectAsState(initial = null)
    val gradientBackgroundId by preferencesData.gradientBackground.collectAsState(initial = "none")

    if (wallpaperPath != null) {
        AsyncImage(
            model = File(wallpaperPath!!),
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
    } else {
        // Mutually exclusive with the wallpaper image at the data layer (see
        // PreferencesData.setWallpaperPath/setGradientBackground), so this only ever applies when
        // there's no custom wallpaper set.
        val gradient = GradientBackground.fromId(gradientBackgroundId)
        if (gradient != GradientBackground.None) {
            Box(modifier = modifier.fillMaxSize().background(Brush.verticalGradient(gradient.colors)))
            Box(modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))
        }
    }
}
