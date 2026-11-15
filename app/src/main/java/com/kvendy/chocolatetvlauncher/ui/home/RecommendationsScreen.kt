package com.kvendy.chocolatetvlauncher.ui.home

import com.kvendy.chocolatetvlauncher.data.LauncherDefaults
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.HomeScreenState
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.data.RecommendedContent
import com.kvendy.chocolatetvlauncher.ui.components.AmbientGlowColorSpec
import com.kvendy.chocolatetvlauncher.ui.components.GlowPosition
import com.kvendy.chocolatetvlauncher.ui.components.ambientGlow
import com.kvendy.chocolatetvlauncher.ui.home.menus.TvInputMenu
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

/** One source app's contribution to the combined Continue Watching + Top Picks pool. */
private data class AppRecommendations(val app: AppInfo?, val packageName: String, val items: List<RecommendedContent>)

/**
 * The dedicated Recommendations page: every app that published Continue Watching or Top Picks
 * content gets its own row, instead of the Home page's single merged preview row. Shares its
 * loaded content with [HomeScreen] (see Launcher.kt) rather than querying the provider again.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    preferencesData: PreferencesData,
    allApps: List<AppInfo>,
    state: HomeScreenState,
    currentPage: HomePage,
    onPageSelected: (HomePage) -> Unit,
    continueWatching: List<RecommendedContent>,
    topPicks: List<RecommendedContent>,
    onAppClicked: (AppInfo) -> Unit,
    onSearchAppLongClicked: (AppInfo) -> Unit,
    onPreferencesClicked: () -> Unit
) {
    var isSearching by remember { mutableStateOf(false) }
    var showInputMenu by remember { mutableStateOf(false) }
    val appsByPackage = remember(allApps) { allApps.associateBy { it.packageName } }
    val showGlow by preferencesData.showGlowEffect.collectAsState(initial = true)
    val glowRadius by preferencesData.glowRadius.collectAsState(initial = LauncherDefaults.GLOW_RADIUS)
    val glowPosition by preferencesData.glowPosition.collectAsState(initial = "topright")
    val showInputShortcut by preferencesData.showInputShortcut.collectAsState(initial = true)
    val showSearchShortcut by preferencesData.showSearchShortcut.collectAsState(initial = true)
    val showSettingsShortcut by preferencesData.showSettingsShortcut.collectAsState(initial = true)
    val context = LocalContext.current

    BackHandler(enabled = isSearching || showInputMenu) {
        if (showInputMenu) showInputMenu = false else isSearching = false
    }

    // Same grouping/sourcing rules as the Home page's merged row: excluded apps drop out, and the
    // "Enable Recommendations" toggle gates the whole page the same way it gates that row, not
    // just the grid title.
    val groups = remember(continueWatching, topPicks, state.excludedContentApps, appsByPackage) {
        (continueWatching + topPicks)
            .filter { it.sourcePackageName != null && it.sourcePackageName !in state.excludedContentApps }
            .groupBy { it.sourcePackageName!! }
            .map { (packageName, items) -> AppRecommendations(appsByPackage[packageName], packageName, items) }
            .sortedBy { (it.app?.label ?: it.packageName).lowercase() }
    }
    val groupsShown = if (state.showFeaturedTile) groups else emptyList()
    val visiblePages = remember(state.showFeaturedTile, state.showNotifications) {
        buildSet {
            add(HomePage.Home)
            if (state.showFeaturedTile) add(HomePage.Recommendations)
            if (state.showNotifications) add(HomePage.Notifications)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        var glowColor by remember { mutableStateOf(Color.Transparent) }
        val animatedGlowColor by animateColorAsState(glowColor, animationSpec = AmbientGlowColorSpec, label = "Ambient Glow")

        // Wallpaper/gradient is rendered by HomeBackground in Launcher.kt, behind both this
        // screen and HomeScreen — see its doc comment for why it isn't rendered per-page here.

        if (!isSearching && showGlow) {
            Box(modifier = Modifier.fillMaxSize().ambientGlow(color = { animatedGlowColor }, position = GlowPosition.fromId(glowPosition)))
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.ExtraLarge,
                end = Spacing.ExtraLarge,
                top = Spacing.ExtraLarge,
                bottom = Spacing.ExtraLarge + 50.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            item {
                StatusBar(
                    currentPage = currentPage,
                    ownPage = HomePage.Recommendations,
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
                    showSearchShortcut = showSearchShortcut,
                    showSettingsShortcut = showSettingsShortcut,
                    isReordering = false
                )
            }

            if (groupsShown.isEmpty()) {
                item {
                    Text(
                        text = if (!state.showFeaturedTile) {
                            "Recommendations are turned off — enable \"Enable Recommendations\" in Preferences to see them here."
                        } else {
                            "No recommendations available yet. Watch something in one of your apps, or check that it's included in Preferences."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.ExtraLarge)
                    )
                }
            } else {
                items(groupsShown, key = { it.packageName }) { group ->
                    ContentRow(
                        title = group.app?.label ?: group.packageName,
                        items = group.items,
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
            TvInputMenu(onDismiss = { showInputMenu = false })
        }
    }
}
