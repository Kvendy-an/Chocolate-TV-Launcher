package com.kvendy.chocolatetvlauncher

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import coil.ImageLoader
import coil.decode.ImageSource
import coil.fetch.SourceResult
import okio.Path.Companion.toOkioPath
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.ImageRequest
import coil.request.Options
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.ColorCache
import com.kvendy.chocolatetvlauncher.data.CustomImageCache
import com.kvendy.chocolatetvlauncher.data.calculateDominantColor
import com.kvendy.chocolatetvlauncher.data.customBannerFile
import com.kvendy.chocolatetvlauncher.data.customIconFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class AppImageModel(
    val appInfo: AppInfo,
    val isBanner: Boolean
)

class AppImageFetcher(
    private val context: Context,
    private val data: AppImageModel
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packageName = data.appInfo.packageName
            val customFile = if (data.isBanner) customBannerFile(context, packageName) else customIconFile(context, packageName)

            if (customFile.exists()) {
                // Let Coil decode the source with the actual request size/configuration.
                return@withContext SourceResult(
                    source = ImageSource(file = customFile.toOkioPath()),
                    mimeType = "image/png",
                    dataSource = DataSource.DISK
                )
            }
            val drawable = try {
                if (data.isBanner) {
                    // Tries every signal hasBanner (MainActivity.loadLaunchableApps) also tries —
                    // a plain flags=0 getActivityInfo() call didn't resolve a banner for some apps
                    // (e.g. Disney+) that a GET_META_DATA-flagged call did, so hasBanner could come
                    // back true while this fell through to loadIcon() anyway, rendering the tile in
                    // "banner" layout with icon pixels. Keeping both resolutions identical means
                    // they can't disagree.
                    val plainInfo = pm.getActivityInfo(data.appInfo.componentName, 0)
                    val metaDataInfo = pm.getActivityInfo(data.appInfo.componentName, android.content.pm.PackageManager.GET_META_DATA)
                    val appInfo = pm.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
                    plainInfo.loadBanner(pm)
                        ?: metaDataInfo.loadBanner(pm)
                        ?: (if (appInfo.banner != 0) pm.getDrawable(packageName, appInfo.banner, appInfo) else null)
                        ?: plainInfo.loadIcon(pm)
                } else {
                    pm.getActivityIcon(data.appInfo.componentName)
                }
            } catch (e: Exception) {
                null
            }

            if (drawable != null) {
                DrawableResult(
                    drawable = drawable,
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } else {
                null
            }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<AppImageModel> {
        override fun create(data: AppImageModel, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppImageFetcher(context, data)
        }
    }
}
internal fun AppImageModel.cacheKey(): String {
    val packageName = appInfo.packageName
    val version = if (isBanner) CustomImageCache.bannerVersion[packageName] else CustomImageCache.iconVersion[packageName]
    return "$packageName:$isBanner:${version ?: 0L}"
}

class AppImageKeyer : Keyer<AppImageModel> {
    override fun key(data: AppImageModel, options: Options): String = data.cacheKey()
}

/** Warms only the caller-selected first visible/nearby apps in their display variant. */
fun ImageLoader.prefetchAppIcons(context: Context, apps: List<AppInfo>, renderAsSquare: Boolean = false) {
    apps.forEach { app ->
        val request = ImageRequest.Builder(context)
            .data(AppImageModel(appInfo = app, isBanner = !renderAsSquare && (app.hasBanner || CustomImageCache.hasCustomBanner(app.packageName))))
            .size(if (renderAsSquare) 192 else 384, if (renderAsSquare) 192 else 216)
            .build()
        enqueue(request)
    }
}

private val appColorSemaphore = Semaphore(2)

/** Share the artwork palette between prefetch, focus glow, and automatic tile backgrounds. */
internal suspend fun ImageLoader.loadAppArtworkColor(
    context: Context,
    model: AppImageModel,
    cacheKey: String,
    fallbackColor: Color
) {
    appColorSemaphore.withPermit {
        if (ColorCache.getColor(cacheKey) != null) return@withPermit
        val request = ImageRequest.Builder(context)
            .data(model)
            .memoryCacheKey(cacheKey)
            .allowHardware(false)
            .size(100, 100)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .build()
        val drawable = execute(request).drawable ?: return@withPermit
        val color = withContext(Dispatchers.Default) { calculateDominantColor(drawable, fallbackColor) }
        ColorCache.setColor(cacheKey, color)
    }
}

suspend fun ImageLoader.prefetchAppColors(
    context: Context,
    apps: List<AppInfo>,
    fallbackColor: Color,
    renderAsSquare: Boolean = false
) = coroutineScope {
    apps.forEach { app ->
        val isBanner = !renderAsSquare && (app.hasBanner || CustomImageCache.hasCustomBanner(app.packageName))
        val model = AppImageModel(app, isBanner)
        val key = model.cacheKey()
        if (ColorCache.getColor(key) == null) {
            launch { loadAppArtworkColor(context, model, key, fallbackColor) }
        }
    }
}
