package com.kvendy.chocolatetvlauncher.ui.theme

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.data.calculateDominantColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * tv-material3's own "neutral" tokens (background/surface/surfaceVariant/border/etc.) are derived
 * from the same purple seed as its default Primary color, so even with no accent override they
 * carry a faint violet cast (e.g. dark SurfaceVariant is `#494F` — blue channel visibly higher
 * than red/green) rather than reading as true grey. These are hue-free replacements at matching
 * lightness, applied regardless of which accent is picked — the accent should only ever show up
 * in [MaterialTheme.colorScheme.primary]-family roles, not tint every neutral surface too.
 */
private object NeutralDark {
    val Background = Color(0xFF1D1D1D)
    val OnBackground = Color(0xFFE4E4E4)
    val SurfaceVariant = Color(0xFF4A4A4A)
    val OnSurfaceVariant = Color(0xFFCACACA)
    val InverseOnSurface = Color(0xFF313131)
    val Border = Color(0xFF949494)
}

private object NeutralLight {
    val Background = Color(0xFFFDFDFD)
    val OnBackground = Color(0xFF1D1D1D)
    val InverseSurface = Color(0xFF313131)
    val InverseOnSurface = Color(0xFFF2F2F2)
    val SurfaceVariant = Color(0xFFE6E6E6)
    val OnSurfaceVariant = Color(0xFF4A4A4A)
    val Border = Color(0xFF797979)
    val BorderVariant = Color(0xFFCACACA)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LauncherTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val preferencesData = remember(context) { PreferencesData(context) }
    val theme by preferencesData.theme.collectAsState(initial = "system")
    val accentColorId by preferencesData.accentColor.collectAsState(initial = "default")
    val uiScale by preferencesData.uiScale.collectAsState(initial = 1f)
    val wallpaperPath by preferencesData.wallpaperPath.collectAsState(initial = null)
    val gradientBackgroundId by preferencesData.gradientBackground.collectAsState(initial = "none")

    val useDarkTheme = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    // Only bothers computing anything when Auto is actually selected — recomputes whenever the
    // background it's sampling from changes. A custom wallpaper's dominant color comes from the
    // same Palette-based extraction already used for tile focus glows; a picked gradient (no
    // photo to sample) uses its own most-saturated stop instead, which every entry defines as the
    // last color in its list; with neither set there's no real "background" to derive from at
    // all, so it just falls back to the neutral Default seed.
    LaunchedEffect(accentColorId, wallpaperPath, gradientBackgroundId) {
        if (AccentColor.fromId(accentColorId) != AccentColor.Auto) return@LaunchedEffect
        AutoAccentColor.seed = withContext(Dispatchers.IO) {
            val path = wallpaperPath
            when {
                path != null -> {
                    try {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(path, bounds)
                        var sample = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 256) sample *= 2
                        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
                            inSampleSize = sample
                        })
                        if (bitmap != null) {
                            try {
                                calculateDominantColor(BitmapDrawable(context.resources, bitmap), AccentColor.Default.seed!!)
                            } finally {
                                bitmap.recycle()
                            }
                        } else {
                            AccentColor.Default.seed!!
                        }
                    } catch (e: Exception) {
                        AccentColor.Default.seed!!
                    }
                }
                GradientBackground.fromId(gradientBackgroundId) != GradientBackground.None ->
                    GradientBackground.fromId(gradientBackgroundId).colors.last()
                else -> AccentColor.Default.seed!!
            }
        }
    }

    val selectedAccent = AccentColor.fromId(accentColorId)
    val accentPalette = if (selectedAccent == AccentColor.Auto) {
        accentPaletteFrom(AutoAccentColor.seed ?: AccentColor.Default.seed!!, isDark = useDarkTheme)
    } else {
        selectedAccent.paletteFor(isDark = useDarkTheme)
    }
    val colorScheme = if (useDarkTheme) {
        val defaults = darkColorScheme()
        darkColorScheme(
            primary = accentPalette?.primary ?: defaults.primary,
            onPrimary = accentPalette?.onPrimary ?: defaults.onPrimary,
            primaryContainer = accentPalette?.primaryContainer ?: defaults.primaryContainer,
            onPrimaryContainer = accentPalette?.onPrimaryContainer ?: defaults.onPrimaryContainer,
            background = NeutralDark.Background,
            onBackground = NeutralDark.OnBackground,
            surface = NeutralDark.Background,
            onSurface = NeutralDark.OnBackground,
            surfaceVariant = NeutralDark.SurfaceVariant,
            onSurfaceVariant = NeutralDark.OnSurfaceVariant,
            inverseSurface = NeutralDark.OnBackground,
            inverseOnSurface = NeutralDark.InverseOnSurface,
            border = NeutralDark.Border,
            borderVariant = NeutralDark.SurfaceVariant
        )
    } else {
        val defaults = lightColorScheme()
        lightColorScheme(
            primary = accentPalette?.primary ?: defaults.primary,
            onPrimary = accentPalette?.onPrimary ?: defaults.onPrimary,
            primaryContainer = accentPalette?.primaryContainer ?: defaults.primaryContainer,
            onPrimaryContainer = accentPalette?.onPrimaryContainer ?: defaults.onPrimaryContainer,
            background = NeutralLight.Background,
            onBackground = NeutralLight.OnBackground,
            surface = NeutralLight.Background,
            onSurface = NeutralLight.OnBackground,
            surfaceVariant = NeutralLight.SurfaceVariant,
            onSurfaceVariant = NeutralLight.OnSurfaceVariant,
            inverseSurface = NeutralLight.InverseSurface,
            inverseOnSurface = NeutralLight.InverseOnSurface,
            border = NeutralLight.Border,
            borderVariant = NeutralLight.BorderVariant
        )
    }

    val baseDensity = LocalDensity.current
    val scaledDensity = Density(density = baseDensity.density * uiScale, fontScale = baseDensity.fontScale)

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            // Wrap content in a Surface to set default background and content (text) color
            androidx.tv.material3.Surface(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                colors = androidx.tv.material3.SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                content()
            }
        }
    }
}
