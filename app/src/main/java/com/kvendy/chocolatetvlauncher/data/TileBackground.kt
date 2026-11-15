package com.kvendy.chocolatetvlauncher.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.kvendy.chocolatetvlauncher.R

enum class TileBackground(val id: String, @StringRes val label: Int, val color: Color? = null) {
    None("none", R.string.tile_background_none),
    Auto("auto", R.string.tile_background_auto),
    Charcoal("charcoal", R.string.tile_background_charcoal, Color(0xFF22242A)),
    Slate("slate", R.string.tile_background_slate, Color(0xFF394454)),
    Gray("gray", R.string.tile_background_gray, Color(0xFF777C86)),
    Ivory("ivory", R.string.tile_background_ivory, Color(0xFFF2E8D8)),
    White("white", R.string.tile_background_white, Color(0xFFFFFFFF)),
    Navy("navy", R.string.tile_background_navy, Color(0xFF1A2C52)),
    Blue("blue", R.string.tile_background_blue, Color(0xFF245287)),
    Sky("sky", R.string.tile_background_sky, Color(0xFF478FD5)),
    Cyan("cyan", R.string.tile_background_cyan, Color(0xFF37AFC1)),
    Teal("teal", R.string.tile_background_teal, Color(0xFF176760)),
    Mint("mint", R.string.tile_background_mint, Color(0xFF78C9AA)),
    Forest("forest", R.string.tile_background_forest, Color(0xFF1A4636)),
    Green("green", R.string.tile_background_green, Color(0xFF35663F)),
    Lime("lime", R.string.tile_background_lime, Color(0xFF9DB64C)),
    Indigo("indigo", R.string.tile_background_indigo, Color(0xFF4342A5)),
    Purple("purple", R.string.tile_background_purple, Color(0xFF624384)),
    Lavender("lavender", R.string.tile_background_lavender, Color(0xFFAA8EDB)),
    Burgundy("burgundy", R.string.tile_background_burgundy, Color(0xFF602B49)),
    Rose("rose", R.string.tile_background_rose, Color(0xFF8A3E55)),
    Pink("pink", R.string.tile_background_pink, Color(0xFFDA82AB)),
    Red("red", R.string.tile_background_red, Color(0xFFB83C43)),
    Orange("orange", R.string.tile_background_orange, Color(0xFFCE6428)),
    Amber("amber", R.string.tile_background_amber, Color(0xFF8B5C22)),
    Gold("gold", R.string.tile_background_gold, Color(0xFFE7B950));

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: None
    }
}
