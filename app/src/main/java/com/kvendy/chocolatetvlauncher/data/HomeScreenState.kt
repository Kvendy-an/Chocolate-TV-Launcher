package com.kvendy.chocolatetvlauncher.data

/** Keys identifying the reorderable home screen sections, plus their default order. */
object HomeSection {
    const val RECENT_APPS = "recent_apps"
    const val FEATURED_CONTENT = "featured_content"
    const val NOTIFICATIONS = "notifications"
    const val APP_GRID = "app_grid"
    val DEFAULT_ORDER = listOf(RECENT_APPS, FEATURED_CONTENT, NOTIFICATIONS, APP_GRID)
}

data class HomeScreenState(
    val iconCornerRadius: Float = LauncherDefaults.ICON_CORNER_RADIUS,
    val showAppLabels: Boolean = true,
    val appLabelScale: Float = 0.85f,
    val gridSize: Int = 6,
    val folderGridSize: Int = 4,
    val gridSquareIcons: Boolean = false,
    val showFeaturedTile: Boolean = false,
    val showRecommendationsOnHome: Boolean = true,
    val showNotifications: Boolean = false,
    val showNotificationsOnHome: Boolean = true,
    val contentCardScale: Float = 0.7f,
    val excludedContentApps: Set<String> = emptySet(),
    val sectionTitleScale: Float = LauncherDefaults.SECTION_TITLE_SCALE,
    val showSectionTitles: Boolean = LauncherDefaults.SHOW_SECTION_TITLES,
    val showSectionIcons: Boolean = LauncherDefaults.SHOW_SECTION_ICONS,
    val sectionOrder: List<String> = HomeSection.DEFAULT_ORDER,
    val showTopBar: Boolean = true,
    val showClock: Boolean = true,
    val showPageTabLabels: Boolean = LauncherDefaults.SHOW_PAGE_TAB_LABELS,
    val showWifiShortcut: Boolean = true,
    val showBluetoothShortcut: Boolean = true,
    // Recent Apps
    val showRecentApps: Boolean = true,
    val recentAppsScale: Float = LauncherDefaults.RECENT_APPS_SCALE,
    val recentAppsSquareIcons: Boolean = LauncherDefaults.RECENT_APPS_SQUARE_ICONS,
    val recentAppsCornerRadius: Float = LauncherDefaults.RECENT_APPS_CORNER_RADIUS,
    val maxRecentApps: Int = LauncherDefaults.MAX_RECENT_APPS,
    // Lists
    val hiddenApps: Set<String> = emptySet(),
    val showHiddenAppsInSearch: Boolean = false,
    val gridOrder: List<HomeGridItem> = emptyList(),
    val recentApps: List<String> = emptyList()
)