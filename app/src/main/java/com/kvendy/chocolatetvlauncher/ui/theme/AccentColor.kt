package com.kvendy.chocolatetvlauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * A named accent color the user can pick instead of the default Material purple. [Default] used
 * to mean "don't override anything" (seed = null), falling back to tv-material3's own stock
 * purple-based primary — but that made its swatch (a neutral grey dot, so it wouldn't be mistaken
 * for an actual color choice) misleading, since picking it still left every focus highlight
 * purple. It now has a real grey seed of its own, so it behaves as an actual neutral/grey theme
 * rather than "whatever the library ships with."
 */
enum class AccentColor(val label: String, val seed: Color?) {
    Default("Default", Color(0xFF6E6E6E)),
    Purple("Purple", Color(0xFF8B5CF6)),
    Red("Red", Color(0xFFEF4444)),
    Orange("Orange", Color(0xFFF97316)),
    Green("Green", Color(0xFF22C55E)),
    Blue("Blue", Color(0xFF3B82F6)),
    Yellow("Yellow", Color(0xFFCA8A04)),
    Pink("Pink", Color(0xFFEC4899)),
    // No fixed seed of its own — LauncherTheme resolves this one dynamically from the current
    // wallpaper/gradient background instead (see AutoAccentColor), the same way Android's own
    // wallpaper-based theming works. Kept last so it reads as the "special" option after the
    // fixed color choices.
    Auto("Auto", null);

    companion object {
        fun fromId(id: String): AccentColor = entries.find { it.name.lowercase() == id } ?: Default
    }
}

val AccentColor.id: String get() = name.lowercase()

/** The four color-scheme roles this app actually reads [MaterialTheme.colorScheme.primary]* for. */
data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color
)

/**
 * Derives a full primary/onPrimary/primaryContainer/onPrimaryContainer set from a single seed
 * color, following the same "container is a muted tone, onContainer is a strongly-separated tone
 * of the same hue" convention Material's own tonal palettes use — [primaryContainer] and
 * [onPrimaryContainer] need reliable contrast against *each other* to stay readable, not just
 * individually plausible colors. Blending onContainer only 60% of the way to black/white (i.e.
 * the same amount as the container itself) put both endpoints in the same lightness neighborhood,
 * which read as low-contrast/muddy in both theme modes — this pushes onContainer further so the
 * two land clearly apart (dark container + light text in dark mode, pale container + dark text in
 * light mode, matching how Material itself pairs these roles).
 */
fun AccentColor.paletteFor(isDark: Boolean): AccentPalette? {
    val base = seed ?: return null
    return accentPaletteFrom(base, isDark)
}

/**
 * The actual tonal derivation, split out from [AccentColor.paletteFor] so [AccentColor.Auto] —
 * which has no fixed [AccentColor.seed] of its own — can build the same palette shape from
 * whatever seed LauncherTheme resolves at runtime (see AutoAccentColor).
 */
fun accentPaletteFrom(base: Color, isDark: Boolean): AccentPalette {
    return if (isDark) {
        AccentPalette(
            primary = base,
            onPrimary = Color.White,
            primaryContainer = lerp(base, Color.Black, 0.6f),
            onPrimaryContainer = lerp(base, Color.White, 0.75f)
        )
    } else {
        AccentPalette(
            primary = base,
            onPrimary = Color.White,
            primaryContainer = lerp(base, Color.White, 0.8f),
            onPrimaryContainer = lerp(base, Color.Black, 0.65f)
        )
    }
}
