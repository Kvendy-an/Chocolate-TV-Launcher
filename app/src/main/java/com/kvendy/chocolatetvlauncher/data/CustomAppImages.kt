package com.kvendy.chocolatetvlauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/** Deterministic per-package file paths for user-imported icon/banner overrides. */
fun customIconFile(context: Context, packageName: String): File =
    File(File(context.filesDir, "custom_icons"), packageName)

fun customBannerFile(context: Context, packageName: String): File =
    File(File(context.filesDir, "custom_banners"), packageName)

/**
 * Tracks which packages currently have a custom icon/banner override, keyed by the override
 * file's lastModified() timestamp (0 = no override). Backed by mutableStateMapOf so reading an
 * entry inside a composable (AppTile deciding its render mode, AppContextMenu deciding which
 * reset buttons to show) subscribes it to recomposition — without this, setting or resetting an
 * override wouldn't visibly update anything already on screen.
 *
 * The version doubles as Coil's cache-busting signal (see AppImageKeyer): a changed file gets a
 * new version, which is folded into the cache key, so Coil treats it as a fresh image instead of
 * serving the old cached bitmap.
 */
object CustomImageCache {
    private const val PREFS_NAME = "custom_app_images"
    private const val KEY_ICON_BACKGROUND_APPS = "icon_background_apps"
    private const val KEY_BACKGROUND_PREFIX = "tile_background:"

    val iconVersion = mutableStateMapOf<String, Long>()
    val bannerVersion = mutableStateMapOf<String, Long>()
    private val tileBackgrounds = mutableStateMapOf<String, TileBackground>()

    fun hasCustomIcon(packageName: String): Boolean = iconVersion.containsKey(packageName)
    fun hasCustomBanner(packageName: String): Boolean = bannerVersion.containsKey(packageName)
    fun tileBackground(packageName: String): TileBackground = tileBackgrounds[packageName] ?: TileBackground.None

    fun seed(context: Context, packageNames: List<String>) {
        packageNames.forEach { pkg ->
            val iconFile = customIconFile(context, pkg)
            if (iconFile.exists()) iconVersion[pkg] = iconFile.lastModified()
            val bannerFile = customBannerFile(context, pkg)
            if (bannerFile.exists()) bannerVersion[pkg] = bannerFile.lastModified()
        }
    }

    internal fun legacyTileBackgrounds(context: Context): Map<String, TileBackground> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return buildMap {
            prefs.getStringSet(KEY_ICON_BACKGROUND_APPS, emptySet()).orEmpty()
                .forEach { put(it, TileBackground.Auto) }
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(KEY_BACKGROUND_PREFIX)) {
                    val packageName = key.removePrefix(KEY_BACKGROUND_PREFIX)
                    val background = TileBackground.fromId(value as? String)
                    if (background == TileBackground.None) remove(packageName)
                    else put(packageName, background)
                }
            }
        }
    }

    internal fun observeTileBackgrounds(context: Context) = context.dataStore.data
        .map { it[TILE_BACKGROUNDS].orEmpty() }
        .distinctUntilChanged()
        .map(::decodeTileBackgrounds)

    internal fun updateTileBackgrounds(backgrounds: Map<String, TileBackground>) {
        Snapshot.withMutableSnapshot {
            tileBackgrounds.keys.toList().forEach { if (it !in backgrounds) tileBackgrounds.remove(it) }
            backgrounds.forEach { (name, background) -> tileBackgrounds[name] = background }
        }
    }

    suspend fun setTileBackground(context: Context, packageName: String, background: TileBackground) {
        context.dataStore.edit { prefs ->
            val backgrounds = decodeTileBackgrounds(prefs[TILE_BACKGROUNDS].orEmpty()).toMutableMap()
            if (background == TileBackground.None) backgrounds.remove(packageName)
            else backgrounds[packageName] = background
            prefs[TILE_BACKGROUNDS] = encodeTileBackgrounds(backgrounds)
        }
    }

    fun setCustomIcon(context: Context, packageName: String, sourceInputStream: InputStream) {
        val file = customIconFile(context, packageName)
        file.parentFile?.mkdirs()
        saveDownscaled(sourceInputStream, file, maxDimension = 512)
        iconVersion[packageName] = file.lastModified()
    }

    fun setCustomBanner(context: Context, packageName: String, sourceInputStream: InputStream) {
        val file = customBannerFile(context, packageName)
        file.parentFile?.mkdirs()
        saveDownscaled(sourceInputStream, file, maxDimension = 960)
        bannerVersion[packageName] = file.lastModified()

    }

    /**
     * Decodes, downscales, and re-encodes the picked image once at import time rather than
     * storing it as-is: a raw gallery photo can be many megapixels, and decoding that at full
     * resolution on every single tile fetch (there's no size hint at read time to sample down
     * against, unlike the PackageManager-provided icons/banners this replaces, which are already
     * reasonably sized) was the direct cause of scroll stutter wherever a custom image was set.
     * Paying the decode cost once here means every later read is already display-sized.
     */
    private fun saveDownscaled(sourceInputStream: InputStream, destFile: File, maxDimension: Int) {
        val bytes = sourceInputStream.use { it.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= maxDimension || bounds.outHeight / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            ?: return
        FileOutputStream(destFile).use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
        bitmap.recycle()
    }

    fun clearCustomIcon(context: Context, packageName: String) {
        customIconFile(context, packageName).delete()
        iconVersion.remove(packageName)
    }

    fun clearCustomBanner(context: Context, packageName: String) {
        customBannerFile(context, packageName).delete()
        bannerVersion.remove(packageName)
    }
}


internal val TILE_BACKGROUNDS = stringSetPreferencesKey("tile_backgrounds")

internal fun encodeTileBackgrounds(backgrounds: Map<String, TileBackground>): Set<String> =
    backgrounds.mapTo(mutableSetOf()) { (name, background) -> "$name:${background.id}" }

internal fun decodeTileBackgrounds(entries: Set<String>): Map<String, TileBackground> = buildMap {
    entries.forEach { entry ->
        val background = TileBackground.fromId(entry.substringAfter(':', ""))
        if (background != TileBackground.None) put(entry.substringBefore(':'), background)
    }
}
