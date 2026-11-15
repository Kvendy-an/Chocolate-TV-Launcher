package com.kvendy.chocolatetvlauncher.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * A pre-made background gradient for users without a photo they want to set as wallpaper.
 * Deliberately dark/muted tones so grid labels and status bar text stay legible without needing
 * as heavy a dimming scrim as an arbitrary user photo does.
 */
enum class GradientBackground(val label: String, val colors: List<Color>) {
    None("None", emptyList()),
    // Trimmed from 17 down to one per genuinely distinct hue family — Ocean/Cosmic/Indigo/Teal
    // and Coral/Rose/Lavender and Forest/Mint/Aurora and Slate/Charcoal were close enough to each
    // other that the picker was mostly showing near-duplicates rather than real choices.
    Sunset("Sunset", listOf(Color(0xFF2D1B4E), Color(0xFF8B3A62), Color(0xFFD97757))),
    SkyBlue("Sky Blue", listOf(Color(0xFF0B1D33), Color(0xFF1B4D6B), Color(0xFF4A9FD9))),
    Midnight("Midnight", listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))),
    Forest("Forest", listOf(Color(0xFF0B3D2E), Color(0xFF14532D), Color(0xFF1B4332))),
    Berry("Berry", listOf(Color(0xFF2E1437), Color(0xFF6D2E6F), Color(0xFF9C3D54))),
    Charcoal("Charcoal", listOf(Color(0xFF141416), Color(0xFF232326), Color(0xFF35353A))),
    Amber("Amber", listOf(Color(0xFF1F1608), Color(0xFF4A3210), Color(0xFF8C5E1D)));

    companion object {
        fun fromId(id: String): GradientBackground = entries.find { it.name.lowercase() == id } ?: None
    }
}

val GradientBackground.id: String get() = name.lowercase()
