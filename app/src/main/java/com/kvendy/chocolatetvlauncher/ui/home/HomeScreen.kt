package com.kvendy.chocolatetvlauncher.ui.home

import com.kvendy.chocolatetvlauncher.data.LauncherDefaults
import android.app.PendingIntent
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.AppNotification
import com.kvendy.chocolatetvlauncher.data.LauncherNotificationListenerService
import com.kvendy.chocolatetvlauncher.data.NotificationStore
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.data.HomeGridItem
import com.kvendy.chocolatetvlauncher.data.HomeScreenState
import com.kvendy.chocolatetvlauncher.data.HomeSection
import com.kvendy.chocolatetvlauncher.data.RecommendedContent
import com.kvendy.chocolatetvlauncher.ui.components.TvAppGrid
import com.kvendy.chocolatetvlauncher.ui.components.AmbientGlowColorSpec
import com.kvendy.chocolatetvlauncher.ui.components.GlowPosition
import com.kvendy.chocolatetvlauncher.ui.components.ambientGlow
import com.kvendy.chocolatetvlauncher.ui.home.menus.TvInputMenu
import com.kvendy.chocolatetvlauncher.ui.home.tiles.AppTile
import com.kvendy.chocolatetvlauncher.ui.home.tiles.FolderTile
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    preferencesData: PreferencesData,
    allApps: List<AppInfo>,
    gridItems: List<HomeGridItem>,
    recentApps: List<AppInfo>,
    state: HomeScreenState,
    movingKey: String?,
    isEditMode: Boolean,
    // See TvAppGrid's own doc comment — keeps focus pinned to the slot being edited instead of
    // letting it follow whichever item a swap/merge just moved out of it.
    focusIndexRequest: Int?,
    currentPage: HomePage,
    onPageSelected: (HomePage) -> Unit,
    // Loaded once, shared with the Recommendations page — see Launcher.kt for the refresh effect.
    continueWatching: List<RecommendedContent>,
    topPicks: List<RecommendedContent>,
    onAppClicked: (AppInfo) -> Unit,
    onAppLongClicked: (AppInfo) -> Unit,
    // Separate from onAppLongClicked: search results are a filtered/alphabetical subset of the
    // grid, not real grid positions, so this must not be able to enter move-mode the way the
    // grid's long-click does.
    onSearchAppLongClicked: (AppInfo) -> Unit,
    onFolderClicked: (HomeGridItem.FolderItem) -> Unit,
    onFolderLongClicked: (HomeGridItem.FolderItem) -> Unit,
    onPreferencesClicked: () -> Unit,
    onRemoveRecentApp: (AppInfo) -> Unit,
    onClearRecentApps: () -> Unit
) {
    var isSearching by remember { mutableStateOf(false) }
    val hiddenApps by preferencesData.hiddenApps.collectAsState(initial = emptySet())
    // Recursive: drops hidden top-level apps outright, and strips hidden members out of folders
    // (dropping a folder entirely if that empties it), so hiding an app works the same whether
    // it's loose on the grid or tucked inside a folder.
    val visibleGridItems = remember(gridItems, hiddenApps) {
        gridItems.mapNotNull { item ->
            when (item) {
                is HomeGridItem.AppItem ->
                    if (item.packageName !in hiddenApps) item else null
                is HomeGridItem.FolderItem -> {
                    val remaining = item.packageNames.filter { it !in hiddenApps }
                    if (remaining.isEmpty()) null else item.copy(packageNames = remaining)
                }
            }
        }
    }
    val appsByPackage = remember(allApps) { allApps.associateBy { it.packageName } }
    val showGlow by preferencesData.showGlowEffect.collectAsState(initial = true)
    val glowRadius by preferencesData.glowRadius.collectAsState(initial = LauncherDefaults.GLOW_RADIUS)
    val glowPosition by preferencesData.glowPosition.collectAsState(initial = "topright")

    var showInputMenu by remember { mutableStateOf(false) }
    val showInputShortcut by preferencesData.showInputShortcut.collectAsState(initial = true)
    val showSearchShortcut by preferencesData.showSearchShortcut.collectAsState(initial = true)
    val showSettingsShortcut by preferencesData.showSettingsShortcut.collectAsState(initial = true)

    val context = LocalContext.current

    BackHandler(enabled = isSearching || showInputMenu) {
        if(showInputMenu) showInputMenu = false else isSearching = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        var glowColor by remember { mutableStateOf(Color.Transparent) }
        val animatedGlowColor by animateColorAsState(glowColor, animationSpec = AmbientGlowColorSpec, label = "Ambient Glow")
        // The whole edit session, not just while something's actively picked up — isEditMode
        // stays true between individual moves too (see Launcher.kt), and StatusBar navigation
        // should stay disabled for the entire session, not just flicker on/off between moves.
        val isReordering = isEditMode

        // Wallpaper/gradient is rendered by HomeBackground in Launcher.kt, behind both this
        // screen and RecommendationsScreen — kept out of here so switching pages doesn't tear it
        // down and briefly expose the plain theme background (see HomeBackground's doc comment).

        if (!isSearching && showGlow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .ambientGlow(color = { animatedGlowColor }, position = GlowPosition.fromId(glowPosition))
            )
        }

        val onContentClicked: (RecommendedContent) -> Unit = { content ->
            val intentUri = content.intentUri
            if (intentUri != null) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, intentUri))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        // Continue Watching and Top Picks merge into one row here — the full per-app breakdown
        // lives on the dedicated Recommendations page now, so the home page just needs a single
        // "here's what's relevant" preview instead of two separate rows.
        val recommendedContent = remember(continueWatching, topPicks, state.excludedContentApps) {
            (continueWatching + topPicks).filter { it.sourcePackageName !in state.excludedContentApps }
        }
        val recommendationsShown = state.showFeaturedTile && state.showRecommendationsOnHome && recommendedContent.isNotEmpty()

        val notificationsShown = if (state.showNotifications) NotificationStore.notifications else emptyList()
        // Deliberately not gated on notificationsShown.isNotEmpty() — the row stays put and shows
        // its own "No Notifications" hint instead of the whole section disappearing, which was
        // both a jarring layout shift and part of what left focus stranded after a dismissal.
        val showNotificationsRow = state.showNotifications && state.showNotificationsOnHome

        fun openNotification(notification: AppNotification) {
            val sent = notification.contentIntent?.let {
                try {
                    it.send()
                    true
                } catch (e: PendingIntent.CanceledException) {
                    false
                }
            } ?: false
            if (!sent) {
                appsByPackage[notification.packageName]?.let { onAppClicked(it) }
            }
        }

        fun dismissNotification(notification: AppNotification) {
            LauncherNotificationListenerService.dismiss(notification.key)
            NotificationStore.notifications.remove(notification)
        }

        val visiblePages = remember(state.showFeaturedTile, state.showNotifications) {
            buildSet {
                add(HomePage.Home)
                if (state.showFeaturedTile) add(HomePage.Recommendations)
                if (state.showNotifications) add(HomePage.Notifications)
            }
        }

        val recentAppsSection: @Composable () -> Unit = {
            if (state.showRecentApps) {
                RecentRow(
                    apps = recentApps,
                    iconCornerRadius = state.recentAppsCornerRadius,
                    showAppLabels = state.showAppLabels,
                    onAppClicked = onAppClicked,
                    onAppFocused = { glowColor = it },
                    isVisible = state.showRecentApps,
                    scale = state.recentAppsScale,
                    titleScale = state.sectionTitleScale,
                    showTitle = state.showSectionTitles,
                    showIcon = state.showSectionIcons,
                    labelScale = state.appLabelScale,
                    useSquareIcons = state.recentAppsSquareIcons,
                    onClearClicked = onClearRecentApps,
                    onRemoveApp = onRemoveRecentApp,
                    showGlow = showGlow,
                    glowRadius = glowRadius
                )
            }
        }
        val featuredContentSection: @Composable () -> Unit = {
            if (recommendationsShown) {
                ContentRow(
                    title = stringResource(R.string.section_recommendations),
                    items = recommendedContent,
                    appsByPackage = appsByPackage,
                    iconCornerRadius = state.iconCornerRadius,
                    scale = state.contentCardScale,
                    titleScale = state.sectionTitleScale,
                    showTitle = state.showSectionTitles,
                    showIcon = state.showSectionIcons,
                    labelScale = state.appLabelScale,
                    onItemFocused = { glowColor = it },
                    showGlow = showGlow,
                    glowRadius = glowRadius,
                    onItemClicked = onContentClicked
                )
            }
        }
        val notificationsSection: @Composable () -> Unit = {
            if (showNotificationsRow) {
                NotificationRow(
                    title = stringResource(R.string.section_notifications),
                    notifications = notificationsShown,
                    appsByPackage = appsByPackage,
                    titleScale = state.sectionTitleScale,
                    showTitle = state.showSectionTitles,
                    showIcon = state.showSectionIcons,
                    onNotificationClicked = ::openNotification,
                    onNotificationDismissed = ::dismissNotification
                )
            }
        }
        val sectionRenderers = mapOf(
            HomeSection.RECENT_APPS to recentAppsSection,
            HomeSection.FEATURED_CONTENT to featuredContentSection,
            HomeSection.NOTIFICATIONS to notificationsSection
        )
        // The user-chosen order only needs to say where the app grid falls relative to the other
        // two sections: everything before it renders above the grid (as part of the header),
        // everything after renders below it (as the footer).
        val beforeGridSections = state.sectionOrder.takeWhile { it != HomeSection.APP_GRID }
        val afterGridSections = state.sectionOrder.dropWhile { it != HomeSection.APP_GRID }.drop(1)
        TvAppGrid(
            items = visibleGridItems,
            gridSize = state.gridSize,
            rowSpacing = if (state.showAppLabels) Spacing.Small else Spacing.Large,
            itemKey = { it.key },
            modifier = Modifier.fillMaxSize(),
            focusIndexRequest = focusIndexRequest,
            confineFocus = isEditMode,
            // Extra bottom clearance beyond the standard screen margin so the last row doesn't
            // feel cramped against the edge when scrolled all the way down.
            contentPadding = PaddingValues(
                start = Spacing.ExtraLarge,
                end = Spacing.ExtraLarge,
                top = Spacing.ExtraLarge,
                bottom = Spacing.ExtraLarge + 50.dp
            ),
            // Header renders as a single LazyColumn item so the gaps between StatusBar/Continue
            // Watching/Top Picks/Recent Apps can use their own (smaller) spacing, independent of
            // The grid's smaller row gap combines with explicit header padding below.
            headerItemCount = 1,
            header = {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                        modifier = Modifier.padding(bottom = Spacing.Small)
                    ) {
                        StatusBar(
                            currentPage = currentPage,
                            ownPage = HomePage.Home,
                            onPageSelected = onPageSelected,
                            visiblePages = visiblePages,
                            onPreferencesClicked = onPreferencesClicked,
                            onSearchClicked = { isSearching = true },
                            onInputsClicked = { showInputMenu = true },
                            onSettingsClicked = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            showOptionalItems = state.showTopBar,
                            showClock = state.showClock,
                            showPageTabLabels = state.showPageTabLabels,
                            showWifiShortcut = state.showWifiShortcut,
                            showBluetoothShortcut = state.showBluetoothShortcut,
                            showInputShortcut = showInputShortcut,
                            showSearchShortcut= showSearchShortcut,
                            showSettingsShortcut = showSettingsShortcut,
                            isReordering = isReordering
                        )
                        beforeGridSections.forEach { key -> sectionRenderers[key]?.invoke() }
                        SectionTitle(
                            text = stringResource(R.string.section_apps),
                            icon = Icons.Default.Apps,
                            scale = state.sectionTitleScale,
                            showLabel = state.showSectionTitles,
                            showIcon = state.showSectionIcons
                        )
                    }
                }
            },
            footer = {
                if (afterGridSections.isNotEmpty()) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                            modifier = Modifier.padding(top = Spacing.Small)
                        ) {
                            afterGridSections.forEach { key -> sectionRenderers[key]?.invoke() }
                        }
                    }
                }
            }
        ) { item, tileModifier ->
            when (item) {
                is HomeGridItem.AppItem -> {
                    val app = appsByPackage[item.packageName]
                    if (app != null) {
                        AppTile(
                            modifier = tileModifier,
                            app = app,
                            aspectRatio = if (state.gridSquareIcons) 1f else 16f / 9f,
                            renderAsSquare = state.gridSquareIcons,
                            iconCornerRadius = state.iconCornerRadius,
                            showAppLabels = state.showAppLabels,
                            onAppClicked = onAppClicked,
                            onAppLongClicked = onAppLongClicked,
                            isMoving = item.key == movingKey,
                            onFocused = { glowColor = it },
                            showGlow = showGlow,
                            glowRadius = glowRadius,
                            labelScale = state.appLabelScale
                        )
                    }
                }
                is HomeGridItem.FolderItem -> {
                    FolderTile(
                        modifier = tileModifier,
                        folder = item,
                        apps = item.packageNames.mapNotNull { appsByPackage[it] },
                        iconCornerRadius = state.iconCornerRadius,
                        showAppLabels = state.showAppLabels,
                        onClick = { onFolderClicked(item) },
                        onLongClick = { onFolderLongClicked(item) },
                        isMoving = item.key == movingKey,
                        onFocused = { glowColor = it },
                        showGlow = showGlow,
                        glowRadius = glowRadius,
                        labelScale = state.appLabelScale,
                        renderAsSquare = state.gridSquareIcons
                    )
                }
            }
        }

        // Pinned as a fixed overlay (not scrolled inline with the grid's own header) so it stays
        // visible regardless of scroll position for the whole edit session, not just while the
        // header happens to be on screen.
        if (isEditMode && !isSearching) {
            Text(
                text = "Editing — tap to swap, hold to merge into a folder, Back to finish",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.Large)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            )
        }

        if (isSearching) {
            SearchScreen(
                allApps = allApps,
                hiddenApps = if (state.showHiddenAppsInSearch) emptySet() else state.hiddenApps,
                iconCornerRadius = state.iconCornerRadius,
                onAppClicked = onAppClicked,
                onAppLongClicked = onSearchAppLongClicked,
                onBackPressed = { isSearching = false },
                showGlow = showGlow,
                glowRadius = glowRadius,
                glowPosition = glowPosition,
                labelScale = state.appLabelScale,
                gridSize = state.gridSize,
                renderAsSquare = state.gridSquareIcons,
                showAppLabels = state.showAppLabels
            )
        }

        if (showInputMenu) {
            TvInputMenu(
                onDismiss = { showInputMenu = false }
            )
        }
    }
}
