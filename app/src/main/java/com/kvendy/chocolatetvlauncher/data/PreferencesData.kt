package com.kvendy.chocolatetvlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context -> listOf(LauncherPreferenceMigration(context)) }
)

class PreferencesData(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        // General
        val ICON_CORNER_RADIUS = floatPreferencesKey("icon_corner_radius")
        val SHOW_APP_LABELS = booleanPreferencesKey("show_app_labels")
        val GRID_SIZE = intPreferencesKey("grid_size")
        val FOLDER_GRID_SIZE = intPreferencesKey("folder_grid_size")
        val GRID_SQUARE_ICONS = booleanPreferencesKey("grid_square_icons")
        val SHOW_FEATURED_TILE = booleanPreferencesKey("show_featured_tile")
        val SHOW_RECOMMENDATIONS_ON_HOME = booleanPreferencesKey("show_recommendations_on_home")
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val SHOW_NOTIFICATIONS_ON_HOME = booleanPreferencesKey("show_notifications_on_home")
        val CONTENT_CARD_SCALE = floatPreferencesKey("content_card_scale")
        val EXCLUDED_CONTENT_APPS = stringSetPreferencesKey("excluded_content_apps")
        val SHOW_SECTION_TITLES = booleanPreferencesKey("show_section_titles")
        val SHOW_SECTION_ICONS = booleanPreferencesKey("show_section_icons")
        val SECTION_TITLE_SCALE = floatPreferencesKey("section_title_scale")
        val SECTION_ORDER = stringPreferencesKey("section_order")
        val APP_LABEL_SCALE = floatPreferencesKey("app_label_scale")
        val UI_SCALE = floatPreferencesKey("ui_scale")
        val NAVIGATION_SOUND_ENABLED = booleanPreferencesKey("navigation_sound_enabled")
        val IDLE_DIM_ENABLED = booleanPreferencesKey("idle_dim_enabled")
        val IDLE_DIM_STRENGTH = intPreferencesKey("idle_dim_strength")
        val IDLE_DIM_STRENGTH_OPTIONS = (5..100 step 5).toList()
        val IDLE_DIM_TIMEOUT_SECONDS = intPreferencesKey("idle_dim_timeout_seconds")
        // The discrete steps the timeout can be set to — 30s, then whole minutes up to 5. Not a
        // uniform step (30s -> 1min is a 30s jump, everything after is a full minute), so this is
        // walked as a list rather than treated as a plain numeric range.
        val IDLE_DIM_TIMEOUT_OPTIONS_SECONDS = listOf(30, 60, 120, 180, 240, 300)

        // Theme
        val THEME = stringPreferencesKey("theme")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val WALLPAPER_PATH = stringPreferencesKey("wallpaper_path")
        val GRADIENT_BACKGROUND = stringPreferencesKey("gradient_background")

        // Top Bar
        val SHOW_TOP_BAR = booleanPreferencesKey("show_top_bar")
        val SHOW_PAGE_TAB_LABELS = booleanPreferencesKey("show_page_tab_labels")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val SHOW_WIFI_SHORTCUT = booleanPreferencesKey("show_wifi_shortcut")
        val SHOW_BLUETOOTH_SHORTCUT = booleanPreferencesKey("show_bluetooth_shortcut")
        val SHOW_SEARCH_SHORTCUT = booleanPreferencesKey("show_search_shortcut")
        val SHOW_SETTINGS_SHORTCUT = booleanPreferencesKey("show_settings_shortcut")

        val SHOW_INPUT_SHORTCUT = booleanPreferencesKey("show_input_shortcut")

        // Recent Apps Customization
        val SHOW_RECENT_APPS = booleanPreferencesKey("show_recent_apps")
        val RECENT_APPS_SCALE = floatPreferencesKey("recent_apps_scale")
        val RECENT_APPS_SQUARE_ICONS = booleanPreferencesKey("recent_apps_square_icons")
        val RECENT_APPS_CORNER_RADIUS = floatPreferencesKey("recent_apps_corner_radius")
        val MAX_RECENT_APPS = intPreferencesKey("max_recent_apps")

        // App Management
        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        val SHOW_HIDDEN_APPS_IN_SEARCH = booleanPreferencesKey("show_hidden_apps_in_search")
        val APP_ORDER = stringPreferencesKey("app_order")
        val SHOW_GLOW_EFFECT = booleanPreferencesKey("show_glow_effect")
        val GLOW_RADIUS = floatPreferencesKey("glow_radius")
        val GLOW_POSITION = stringPreferencesKey("glow_position")
    }

    // Every Flow below ends in distinctUntilChanged(): dataStore.data re-emits its ENTIRE
    // Preferences snapshot whenever ANY key changes, so without this, changing one unrelated
    // preference would cause every single one of these ~25 flows to re-emit too — and since
    // Launcher.kt combine()s them all into one HomeScreenState, that meant one settings toggle
    // could cascade into up to ~25 redundant recompositions of the whole home screen instead of
    // exactly one. This makes each flow only actually emit when its own resolved value changed.

    // General Flows
    val iconCornerRadius: Flow<Float> = dataStore.data.map { it[ICON_CORNER_RADIUS] ?: LauncherDefaults.ICON_CORNER_RADIUS }.distinctUntilChanged()
    val showAppLabels: Flow<Boolean> = dataStore.data.map { it[SHOW_APP_LABELS] ?: true }.distinctUntilChanged()
    val appLabelScale: Flow<Float> = dataStore.data.map { it[APP_LABEL_SCALE] ?: 0.85f }.distinctUntilChanged()
    val gridSize: Flow<Int> = dataStore.data.map { it[GRID_SIZE] ?: 6 }.distinctUntilChanged()
    val folderGridSize: Flow<Int> = dataStore.data.map { it[FOLDER_GRID_SIZE] ?: 4 }.distinctUntilChanged()
    val gridSquareIcons: Flow<Boolean> = dataStore.data.map { it[GRID_SQUARE_ICONS] ?: false }.distinctUntilChanged()
    // Master switch: whether the Recommendations feature exists at all (the page/tab and the
    // Home row). showRecommendationsOnHome is a narrower suboption only meaningful while this is
    // on — it hides just the Home row for someone who still wants the dedicated page.
    val showFeaturedTile: Flow<Boolean> = dataStore.data.map { it[SHOW_FEATURED_TILE] ?: false }.distinctUntilChanged()
    val showRecommendationsOnHome: Flow<Boolean> = dataStore.data.map { it[SHOW_RECOMMENDATIONS_ON_HOME] ?: true }.distinctUntilChanged()
    // Same master/suboption shape as showFeaturedTile/showRecommendationsOnHome above.
    val showNotifications: Flow<Boolean> = dataStore.data.map { it[SHOW_NOTIFICATIONS] ?: false }.distinctUntilChanged()
    val showNotificationsOnHome: Flow<Boolean> = dataStore.data.map { it[SHOW_NOTIFICATIONS_ON_HOME] ?: true }.distinctUntilChanged()
    val contentCardScale: Flow<Float> = dataStore.data.map { it[CONTENT_CARD_SCALE] ?: 0.7f }.distinctUntilChanged()
    val excludedContentApps: Flow<Set<String>> = dataStore.data.map { it[EXCLUDED_CONTENT_APPS] ?: emptySet() }.distinctUntilChanged()
    val showSectionTitles: Flow<Boolean> = dataStore.data.map { it[SHOW_SECTION_TITLES] ?: LauncherDefaults.SHOW_SECTION_TITLES }.distinctUntilChanged()
    val showSectionIcons: Flow<Boolean> = dataStore.data.map { it[SHOW_SECTION_ICONS] ?: LauncherDefaults.SHOW_SECTION_ICONS }.distinctUntilChanged()
    val sectionTitleScale: Flow<Float> = dataStore.data.map { it[SECTION_TITLE_SCALE] ?: LauncherDefaults.SECTION_TITLE_SCALE }.distinctUntilChanged()
    val sectionOrder: Flow<List<String>> = dataStore.data.map { prefs ->
        val stored = prefs[SECTION_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val validStored = stored.filter { it in HomeSection.DEFAULT_ORDER }.distinct()
        if (validStored.isEmpty()) HomeSection.DEFAULT_ORDER
        else validStored + HomeSection.DEFAULT_ORDER.filter { it !in validStored }
    }.distinctUntilChanged()
    // Manual per-device compensation for TV boxes that misreport display density (rendering
    // identical dp values at visibly different physical sizes) — not something the app can detect
    // or correct automatically, so this is an escape hatch rather than a cosmetic preference.
    val uiScale: Flow<Float> = dataStore.data.map { it[UI_SCALE] ?: 1f }.distinctUntilChanged()

    // Theme Flow
    val theme: Flow<String> = dataStore.data.map { it[THEME] ?: "system" }.distinctUntilChanged()
    val accentColor: Flow<String> = dataStore.data.map { it[ACCENT_COLOR] ?: "default" }.distinctUntilChanged()
    val wallpaperPath: Flow<String?> = dataStore.data.map { it[WALLPAPER_PATH] }.distinctUntilChanged()
    val gradientBackground: Flow<String> = dataStore.data.map { it[GRADIENT_BACKGROUND] ?: "none" }.distinctUntilChanged()

    // Top Bar Flows
    val showTopBar: Flow<Boolean> = dataStore.data.map { it[SHOW_TOP_BAR] ?: true }.distinctUntilChanged()
    val showPageTabLabels: Flow<Boolean> = dataStore.data.map { it[SHOW_PAGE_TAB_LABELS] ?: LauncherDefaults.SHOW_PAGE_TAB_LABELS }.distinctUntilChanged()
    val showClock: Flow<Boolean> = dataStore.data.map { it[SHOW_CLOCK] ?: true }.distinctUntilChanged()
    val showWifiShortcut: Flow<Boolean> = dataStore.data.map { it[SHOW_WIFI_SHORTCUT] ?: true }.distinctUntilChanged()
    val showBluetoothShortcut: Flow<Boolean> = dataStore.data.map { it[SHOW_BLUETOOTH_SHORTCUT] ?: true }.distinctUntilChanged()
    val showInputShortcut: Flow<Boolean> = dataStore.data.map { it[SHOW_INPUT_SHORTCUT] ?: true }.distinctUntilChanged()
    val showSearchShortcut: Flow<Boolean> = dataStore.data.map { it[SHOW_SEARCH_SHORTCUT] ?: true }.distinctUntilChanged()
    val showSettingsShortcut: Flow<Boolean> = dataStore.data.map { it[SHOW_SETTINGS_SHORTCUT] ?: true }.distinctUntilChanged()

    // Recent Apps Flows
    val showRecentApps: Flow<Boolean> = dataStore.data.map { it[SHOW_RECENT_APPS] ?: true }.distinctUntilChanged()
    val recentAppsScale: Flow<Float> = dataStore.data.map { it[RECENT_APPS_SCALE] ?: LauncherDefaults.RECENT_APPS_SCALE }.distinctUntilChanged()
    val recentAppsSquareIcons: Flow<Boolean> = dataStore.data.map { it[RECENT_APPS_SQUARE_ICONS] ?: LauncherDefaults.RECENT_APPS_SQUARE_ICONS }.distinctUntilChanged()
    val recentAppsCornerRadius: Flow<Float> = dataStore.data.map { it[RECENT_APPS_CORNER_RADIUS] ?: LauncherDefaults.RECENT_APPS_CORNER_RADIUS }.distinctUntilChanged()
    val maxRecentApps: Flow<Int> = dataStore.data.map { it[MAX_RECENT_APPS] ?: LauncherDefaults.MAX_RECENT_APPS }.distinctUntilChanged()

    // App Management Flows
    val showHiddenAppsInSearch: Flow<Boolean> = dataStore.data.map { it[SHOW_HIDDEN_APPS_IN_SEARCH] ?: false }.distinctUntilChanged()
    val hiddenApps: Flow<Set<String>> = dataStore.data.map { it[HIDDEN_APPS] ?: emptySet() }.distinctUntilChanged()
    val gridOrder: Flow<List<HomeGridItem>> = dataStore.data.map { decodeGridOrder(it[APP_ORDER] ?: "") }.distinctUntilChanged()
    val showGlowEffect: Flow<Boolean> = dataStore.data.map { it[SHOW_GLOW_EFFECT] ?: true }.distinctUntilChanged()
    val glowRadius: Flow<Float> = dataStore.data.map { it[GLOW_RADIUS] ?: LauncherDefaults.GLOW_RADIUS }.distinctUntilChanged()
    val glowPosition: Flow<String> = dataStore.data.map { it[GLOW_POSITION] ?: "topright" }.distinctUntilChanged()
    val navigationSoundEnabled: Flow<Boolean> = dataStore.data.map { it[NAVIGATION_SOUND_ENABLED] ?: true }.distinctUntilChanged()
    val idleDimEnabled: Flow<Boolean> = dataStore.data.map { it[IDLE_DIM_ENABLED] ?: true }.distinctUntilChanged()
    // Coerced into range here (not just at the UI edge) so a value from an older/different build
    // can never end up outside the 30s-5min range this feature was designed for.
    val idleDimStrength: Flow<Int> = dataStore.data
        .map { normalizeDimStrength(it[IDLE_DIM_STRENGTH] ?: LauncherDefaults.IDLE_DIM_STRENGTH) }
        .distinctUntilChanged()
    val idleDimTimeoutSeconds: Flow<Int> = dataStore.data.map { (it[IDLE_DIM_TIMEOUT_SECONDS] ?: 30).coerceIn(30, 300) }.distinctUntilChanged()

    // Add these Setter functions
    suspend fun setShowGlowEffect(show: Boolean) { dataStore.edit { it[SHOW_GLOW_EFFECT] = show } }
    suspend fun setGlowRadius(radius: Float) { dataStore.edit { it[GLOW_RADIUS] = radius } }
    suspend fun setGlowPosition(id: String) { dataStore.edit { it[GLOW_POSITION] = id } }
    suspend fun setNavigationSoundEnabled(enabled: Boolean) { dataStore.edit { it[NAVIGATION_SOUND_ENABLED] = enabled } }
    suspend fun setIdleDimEnabled(enabled: Boolean) { dataStore.edit { it[IDLE_DIM_ENABLED] = enabled } }
    suspend fun setIdleDimStrength(percent: Int) {
        dataStore.edit { it[IDLE_DIM_STRENGTH] = normalizeDimStrength(percent) }
    }

    private fun normalizeDimStrength(percent: Int): Int =
        ((percent.coerceIn(5, 100) + 2) / 5) * 5

    suspend fun setIdleDimTimeoutSeconds(seconds: Int) { dataStore.edit { it[IDLE_DIM_TIMEOUT_SECONDS] = seconds.coerceIn(30, 300) } }
    // Setters
    suspend fun setIconCornerRadius(radius: Float) { dataStore.edit { it[ICON_CORNER_RADIUS] = radius } }
    suspend fun setShowAppLabels(show: Boolean) { dataStore.edit { it[SHOW_APP_LABELS] = show } }
    suspend fun setAppLabelScale(scale: Float) { dataStore.edit { it[APP_LABEL_SCALE] = scale } }
    suspend fun setGridSize(size: Int) { dataStore.edit { it[GRID_SIZE] = size } }
    suspend fun setFolderGridSize(size: Int) { dataStore.edit { it[FOLDER_GRID_SIZE] = size } }
    suspend fun setGridSquareIcons(square: Boolean) { dataStore.edit { it[GRID_SQUARE_ICONS] = square } }
    suspend fun setShowFeaturedTile(show: Boolean) { dataStore.edit { it[SHOW_FEATURED_TILE] = show } }
    suspend fun setShowRecommendationsOnHome(show: Boolean) { dataStore.edit { it[SHOW_RECOMMENDATIONS_ON_HOME] = show } }
    suspend fun setShowNotifications(show: Boolean) { dataStore.edit { it[SHOW_NOTIFICATIONS] = show } }
    suspend fun setShowNotificationsOnHome(show: Boolean) { dataStore.edit { it[SHOW_NOTIFICATIONS_ON_HOME] = show } }
    suspend fun setContentCardScale(scale: Float) { dataStore.edit { it[CONTENT_CARD_SCALE] = scale } }
    suspend fun setExcludedContentApps(excluded: Set<String>) { dataStore.edit { it[EXCLUDED_CONTENT_APPS] = excluded } }
    suspend fun setShowSectionTitles(show: Boolean) { dataStore.edit { it[SHOW_SECTION_TITLES] = show } }
    suspend fun setShowSectionIcons(show: Boolean) { dataStore.edit { it[SHOW_SECTION_ICONS] = show } }
    suspend fun setSectionTitleScale(scale: Float) { dataStore.edit { it[SECTION_TITLE_SCALE] = scale } }
    suspend fun setSectionOrder(order: List<String>) { dataStore.edit { it[SECTION_ORDER] = order.joinToString(",") } }
    suspend fun setUiScale(scale: Float) { dataStore.edit { it[UI_SCALE] = scale } }
    suspend fun toggleExcludedContentApp(packageName: String) {
        dataStore.edit {
            val excluded = it[EXCLUDED_CONTENT_APPS]?.toMutableSet() ?: mutableSetOf()
            if (!excluded.remove(packageName)) excluded.add(packageName)
            it[EXCLUDED_CONTENT_APPS] = excluded
        }
    }
    suspend fun setTheme(theme: String) { dataStore.edit { it[THEME] = theme } }
    suspend fun setAccentColor(color: String) { dataStore.edit { it[ACCENT_COLOR] = color } }
    suspend fun setWallpaperPath(path: String?) {
        dataStore.edit { preferences ->
            if (path == null) {
                preferences.remove(WALLPAPER_PATH)
            } else {
                // Wallpaper and gradient background are mutually exclusive — only one image
                // source is ever drawn behind the home screen, so picking one clears the other.
                preferences[WALLPAPER_PATH] = path
                preferences[GRADIENT_BACKGROUND] = "none"
            }
        }
    }
    suspend fun setGradientBackground(id: String) {
        dataStore.edit { preferences ->
            preferences[GRADIENT_BACKGROUND] = id
            if (id != "none") {
                preferences.remove(WALLPAPER_PATH)
            }
        }
    }
    suspend fun setShowTopBar(show: Boolean) { dataStore.edit { it[SHOW_TOP_BAR] = show } }
    suspend fun setShowPageTabLabels(show: Boolean) { dataStore.edit { it[SHOW_PAGE_TAB_LABELS] = show } }
    suspend fun setShowClock(show: Boolean) { dataStore.edit { it[SHOW_CLOCK] = show } }
    suspend fun setShowWifiShortcut(show: Boolean) { dataStore.edit { it[SHOW_WIFI_SHORTCUT] = show } }
    suspend fun setShowBluetoothShortcut(show: Boolean) { dataStore.edit { it[SHOW_BLUETOOTH_SHORTCUT] = show } }
    suspend fun setShowInputShortcut(show: Boolean) { dataStore.edit { it[SHOW_INPUT_SHORTCUT] = show } }
    suspend fun setShowSearchShortcut(show: Boolean) { dataStore.edit { it[SHOW_SEARCH_SHORTCUT] = show } }
    suspend fun setShowSettingsShortcut(show: Boolean) { dataStore.edit { it[SHOW_SETTINGS_SHORTCUT] = show } }
    suspend fun setShowRecentApps(show: Boolean) { dataStore.edit { it[SHOW_RECENT_APPS] = show } }
    suspend fun setRecentAppsScale(scale: Float) { dataStore.edit { it[RECENT_APPS_SCALE] = scale } }
    suspend fun setRecentAppsSquareIcons(square: Boolean) { dataStore.edit { it[RECENT_APPS_SQUARE_ICONS] = square } }
    suspend fun setRecentAppsCornerRadius(radius: Float) { dataStore.edit { it[RECENT_APPS_CORNER_RADIUS] = radius } }
    suspend fun setMaxRecentApps(max: Int) { dataStore.edit { it[MAX_RECENT_APPS] = max } }
    suspend fun setShowHiddenAppsInSearch(show: Boolean) { dataStore.edit { it[SHOW_HIDDEN_APPS_IN_SEARCH] = show } }
    suspend fun hideApp(packageName: String) {
        dataStore.edit {
            val hidden = it[HIDDEN_APPS]?.toMutableSet() ?: mutableSetOf()
            hidden.add(packageName)
            it[HIDDEN_APPS] = hidden
        }
    }
    suspend fun unhideApp(packageName: String) {
        dataStore.edit {
            val hidden = it[HIDDEN_APPS]?.toMutableSet() ?: mutableSetOf()
            hidden.remove(packageName)
            it[HIDDEN_APPS] = hidden
        }
    }
    suspend fun setHiddenApps(hidden: Set<String>) { dataStore.edit { it[HIDDEN_APPS] = hidden } }
    suspend fun toggleHiddenApp(packageName: String) {
        dataStore.edit {
            val hidden = it[HIDDEN_APPS]?.toMutableSet() ?: mutableSetOf()
            if (!hidden.remove(packageName)) hidden.add(packageName)
            it[HIDDEN_APPS] = hidden
        }
    }
    /** App discovery may be incomplete at boot; automatic reconciliation only appends. */
    suspend fun appendNewApps(packageNames: List<String>) {
        dataStore.edit { prefs ->
            val current = decodeGridOrder(prefs[APP_ORDER] ?: "")
            val updated = appendDiscoveredApps(current, packageNames)
            if (updated != current) prefs[APP_ORDER] = encodeGridOrder(updated)
        }
    }

    suspend fun saveGridOrder(items: List<HomeGridItem>) { dataStore.edit { it[APP_ORDER] = encodeGridOrder(items) } }

    // The only path that's allowed to shrink the saved grid order (see Launcher.kt's
    // orderedGridItems comment) — called from MainActivity's ACTION_PACKAGE_REMOVED /
    // ACTION_PACKAGE_FULLY_REMOVED handling, i.e. only once the system has confirmed a specific
    // package is actually gone, never inferred from an app-list scan that might just be
    // incomplete. Read-modify-write within the same edit{} transaction so a removal can't race
    // with a concurrent reorder/append and clobber it.
    suspend fun removePackageFromGridOrder(packageName: String) {
        dataStore.edit { prefs ->
            val current = decodeGridOrder(prefs[APP_ORDER] ?: "")
            val updated = current.mapNotNull { item ->
                when (item) {
                    is HomeGridItem.AppItem ->
                        if (item.packageName == packageName) null else item
                    is HomeGridItem.FolderItem -> {
                        if (packageName !in item.packageNames) {
                            item
                        } else {
                            val remaining = item.packageNames.filter { it != packageName }
                            if (remaining.isEmpty()) null else item.copy(packageNames = remaining)
                        }
                    }
                }
            }
            prefs[APP_ORDER] = encodeGridOrder(updated)
        }
    }
}