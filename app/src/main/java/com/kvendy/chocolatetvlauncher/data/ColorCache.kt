package com.kvendy.chocolatetvlauncher.data

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color

// A simple in-memory cache to store app colors
object ColorCache {
    private val cache = mutableStateMapOf<String, Color>()

    fun getColor(packageName: String): Color? {
        return cache[packageName]
    }

    fun setColor(packageName: String, color: Color) {
        cache[packageName] = color
    }
}
