package com.kvendy.chocolatetvlauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

/** Extracts a representative accent color from [drawable] for the focus glow. */
fun calculateDominantColor(drawable: Drawable, fallbackColor: Color): Color {
    val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    var color = Color.Transparent
    val palette = Palette.from(bmp).generate()

    val swatch = palette.vibrantSwatch
        ?: palette.darkVibrantSwatch
        ?: palette.lightVibrantSwatch
        ?: palette.dominantSwatch
        ?: palette.mutedSwatch

    if (swatch != null) {
        color = Color(swatch.rgb)
    } else {
        val centerPixel = bmp.getPixel(50, 50)
        color = if (centerPixel != android.graphics.Color.TRANSPARENT) {
            Color(centerPixel)
        } else {
            fallbackColor
        }
    }
    return color
}
