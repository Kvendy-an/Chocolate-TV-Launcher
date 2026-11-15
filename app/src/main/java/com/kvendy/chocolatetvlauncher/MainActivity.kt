package com.kvendy.chocolatetvlauncher

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.memory.MemoryCache
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.data.RecentAppsTracker
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.CustomImageCache
import com.kvendy.chocolatetvlauncher.data.HomeGridItem
import com.kvendy.chocolatetvlauncher.data.LauncherDefaults
import com.kvendy.chocolatetvlauncher.remote.RemoteButtons
import com.kvendy.chocolatetvlauncher.ui.home.IdleDimOverlay
import com.kvendy.chocolatetvlauncher.ui.home.Launcher
import com.kvendy.chocolatetvlauncher.ui.home.LoadingScreen
import com.kvendy.chocolatetvlauncher.ui.theme.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

internal const val ACTION_OPEN_PREFERENCES = "com.kvendy.chocolatetvlauncher.OPEN_PREFERENCES"

class MainActivity : ComponentActivity() {

    private lateinit var preferencesData: PreferencesData
    private lateinit var recentAppsTracker: RecentAppsTracker
    private lateinit var imageLoader: ImageLoader
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    // Read imperatively from dispatchKeyEvent, which isn't a composable and can't collect a Flow
    // directly — kept in sync with the real preference by the collector started in onCreate below.
    @Volatile private var navigationSoundEnabled = true

    // Backed by Compose State (not a plain field) so IdleDimOverlay recomposes off these directly,
    // even though both are written from dispatchKeyEvent — a plain Activity method, not a
    // composable. Safe to mutate from there: key events are always dispatched on the main thread,
    // same as Compose itself.
    private var lastInteractionAt by mutableLongStateOf(SystemClock.elapsedRealtime())
    private var isIdleDimmed by mutableStateOf(false)
    private var launcherHasFocus by mutableStateOf(false)
    private var wakePress: KeyEvent? = null
    private var showPreferences by mutableStateOf(false)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        launcherHasFocus = hasFocus
        lastInteractionAt = SystemClock.elapsedRealtime()
        isIdleDimmed = false
        wakePress = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_OPEN_PREFERENCES) showPreferences = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("showPreferences", showPreferences)
    }

    // This is Activity's public input-dispatch hook. AndroidX Core annotates its bridge
    // as restricted, but forwarding to super is required to preserve normal key dispatch.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val waking = wakePress
        if (waking != null && event.deviceId == waking.deviceId &&
            event.keyCode == waking.keyCode && event.downTime == waking.downTime
        ) {
            lastInteractionAt = SystemClock.elapsedRealtime()
            if (event.action == KeyEvent.ACTION_UP) wakePress = null
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (isIdleDimmed) {
                // First press after dimming only wakes the screen — swallowed here (before Compose
                // ever sees it) so it can't also act on whatever tile/button happens to be focused
                // underneath, the same way a phone's screen-off tap doesn't also trigger the app.
                wakePress = KeyEvent(event)
                isIdleDimmed = false
                lastInteractionAt = SystemClock.elapsedRealtime()
                return true
            }
            lastInteractionAt = SystemClock.elapsedRealtime()
            if (navigationSoundEnabled) {
                val effect = when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> AudioManager.FX_FOCUS_NAVIGATION_UP
                    KeyEvent.KEYCODE_DPAD_DOWN -> AudioManager.FX_FOCUS_NAVIGATION_DOWN
                    KeyEvent.KEYCODE_DPAD_LEFT -> AudioManager.FX_FOCUS_NAVIGATION_LEFT
                    KeyEvent.KEYCODE_DPAD_RIGHT -> AudioManager.FX_FOCUS_NAVIGATION_RIGHT
                    else -> null
                }
                // The same effect IDs (and playSoundEffect itself) the platform's own D-pad
                // navigation already uses elsewhere in Android TV — no bundled sound asset needed,
                // and it already respects the system's own "Sound effects" setting.
                effect?.let { audioManager.playSoundEffect(it) }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        RemoteButtons.launcherForeground = true
    }

    override fun onPause() {
        RemoteButtons.launcherForeground = false
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPreferences = savedInstanceState?.getBoolean("showPreferences")
            ?: (intent.action == ACTION_OPEN_PREFERENCES)
        preferencesData = PreferencesData(this)
        recentAppsTracker = RecentAppsTracker(this)

        lifecycleScope.launch {
            preferencesData.navigationSoundEnabled.collect { navigationSoundEnabled = it }
        }
        lifecycleScope.launch {
            CustomImageCache.observeTileBackgrounds(this@MainActivity).collect(CustomImageCache::updateTileBackgrounds)
        }

        imageLoader = ImageLoader.Builder(this)
            .components {
                add(AppImageKeyer())
                add(AppImageFetcher.Factory(this@MainActivity))
            }
            // Coil's own default sizes this at ~20-25% of the app's available memory — reasonable
            // on a typical phone, but generous for a TV box that's often already low on RAM to
            // begin with, holding nothing bigger than app icons/banners here. 15% keeps enough
            // headroom to avoid re-decoding the same handful of on-screen tiles during scroll
            // without competing as hard with the rest of the app for memory.
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.15).build() }
            .build()

        setContent {
            LauncherTheme {
                CompositionLocalProvider(coil.compose.LocalImageLoader provides imageLoader) {
                    var allInstalledApps by remember { mutableStateOf(emptyList<AppInfo>()) }
                    // The app scan below (PackageManager queries plus a per-app banner/icon
                    // resolution) is the one genuinely slow part of startup — without this, the
                    // grid would render empty for that whole stretch and then suddenly populate.
                    var isLoading by remember { mutableStateOf(true) }
                    val coroutineScope = rememberCoroutineScope()

                    suspend fun refreshApps(): List<AppInfo> {
                        val apps = loadLaunchableApps()
                        allInstalledApps = apps
                        // Limit startup warming to likely visible content in saved order.
                        coroutineScope.launch {
                            val hidden = preferencesData.hiddenApps.first()
                            val columns = preferencesData.gridSize.first().coerceIn(2, 12)
                            val square = preferencesData.gridSquareIcons.first()
                            val byPackage = apps.associateBy { it.packageName }
                            val savedPackages = preferencesData.gridOrder.first().flatMap { item ->
                                when (item) {
                                    is HomeGridItem.AppItem -> listOf(item.packageName)
                                    is HomeGridItem.FolderItem -> item.packageNames.take(4)
                                }
                            }
                            val candidates = (savedPackages + apps.map { it.packageName })
                                .distinct().filter { it !in hidden }.take(columns * 2)
                                .mapNotNull { byPackage[it] }
                            imageLoader.prefetchAppIcons(this@MainActivity, candidates, renderAsSquare = square)
                            if (preferencesData.showGlowEffect.first()) {
                                imageLoader.prefetchAppColors(
                                    this@MainActivity, candidates,
                                    fallbackColor = Color(0xFF3A3A3A), renderAsSquare = square
                                )
                            }
                        }
                        return apps
                    }

                    DisposableEffect(Unit) {
                        // Coalesces a burst of package events (e.g. bulk-restoring apps right
                        // after a factory reset, or a system update touching many packages at
                        // once) into a single rescan instead of one full PackageManager query +
                        // per-app banner/icon resolution per event — back-to-back, those can
                        // visibly stall a weak-SoC device for several seconds.
                        var rescanJob: Job? = null
                        val receiver = object : BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: Intent) {
                                // Immediate and independent of the debounce below: this is the
                                // only path allowed to shrink the saved grid order (see
                                // Launcher.kt's orderedGridItems comment) — it only fires here
                                // because the system has confirmed this specific package is
                                // actually gone, not inferred from a scan that might just be
                                // incomplete.
                                if ((intent.action == Intent.ACTION_PACKAGE_REMOVED || intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED)
                                    && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                                    intent.data?.schemeSpecificPart?.let { packageName ->
                                        coroutineScope.launch { preferencesData.removePackageFromGridOrder(packageName) }
                                    }
                                }
                                rescanJob?.cancel()
                                rescanJob = coroutineScope.launch {
                                    delay(800)
                                    refreshApps()
                                }
                            }
                        }
                        val filter = IntentFilter().apply {
                            addAction(Intent.ACTION_PACKAGE_ADDED)
                            addAction(Intent.ACTION_PACKAGE_REMOVED)
                            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
                            addDataScheme("package")
                        }
                        registerReceiver(receiver, filter)
                        onDispose {
                            rescanJob?.cancel()
                            unregisterReceiver(receiver)
                        }
                    }

                    LaunchedEffect(Unit) {
                        val apps = refreshApps()
                        preferencesData.appendNewApps(apps.map { it.packageName })
                        isLoading = false
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isLoading) {
                            LoadingScreen(preferencesData = preferencesData)
                        } else {
                            Launcher(
                                preferencesData = preferencesData,
                                recentAppsTracker = recentAppsTracker,
                                allInstalledApps = allInstalledApps,
                                showSettings = showPreferences,
                                onShowSettingsChange = { showPreferences = it }
                            )
                        }

                        val idleDimEnabled by preferencesData.idleDimEnabled.collectAsState(initial = true)
                        val idleDimTimeoutSeconds by preferencesData.idleDimTimeoutSeconds.collectAsState(initial = 30)
                        val idleDimStrength by preferencesData.idleDimStrength.collectAsState(initial = LauncherDefaults.IDLE_DIM_STRENGTH)
                        IdleDimOverlay(
                            enabled = idleDimEnabled && launcherHasFocus,
                            strengthPercent = idleDimStrength,
                            timeoutSeconds = idleDimTimeoutSeconds,
                            lastInteractionAt = lastInteractionAt,
                            isDimmed = isIdleDimmed,
                            onDimmedChange = { isIdleDimmed = it }
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }

        val allApps = mutableMapOf<String, ResolveInfo>()
        pm.queryIntentActivities(mainIntent, 0).forEach { allApps[it.activityInfo.packageName] = it }
        pm.queryIntentActivities(leanbackIntent, 0).forEach { allApps[it.activityInfo.packageName] = it }

        val apps = allApps.values
            .map {
                val componentName = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                // Resolved through every signal available rather than trusting any single one —
                // a handful of apps (e.g. Disney+) still came back bannerless checking only
                // getActivityInfo(..., 0).loadBanner(), so this also tries with GET_META_DATA (some
                // devices/OS versions only populate certain ActivityInfo/ApplicationInfo fields with
                // that flag) and a direct getApplicationInfo() resource-id check as a last resort.
                val hasBanner = try {
                    pm.getActivityInfo(componentName, 0).loadBanner(pm) != null
                            || pm.getActivityInfo(componentName, android.content.pm.PackageManager.GET_META_DATA).loadBanner(pm) != null
                            || pm.getApplicationInfo(componentName.packageName, android.content.pm.PackageManager.GET_META_DATA).banner != 0
                } catch (e: Exception) {
                    false
                }
                AppInfo(
                    it.activityInfo.packageName,
                    it.loadLabel(pm).toString(),
                    componentName,
                    hasBanner = hasBanner
                )
            }
            .filter { it.packageName != packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }

        CustomImageCache.seed(this@MainActivity, apps.map { it.packageName })
        apps
    }
}