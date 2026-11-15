package com.kvendy.chocolatetvlauncher.ui.home

import com.kvendy.chocolatetvlauncher.data.LauncherDefaults
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.data.RecentAppsTracker
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.HomeScreenState
import com.kvendy.chocolatetvlauncher.data.HomeGridItem
import com.kvendy.chocolatetvlauncher.data.RecommendedContent
import com.kvendy.chocolatetvlauncher.data.loadTopPicks
import com.kvendy.chocolatetvlauncher.data.loadWatchNextPrograms
import com.kvendy.chocolatetvlauncher.data.newFolderId
import com.kvendy.chocolatetvlauncher.ui.home.menus.AppContextMenu
import com.kvendy.chocolatetvlauncher.ui.home.menus.FolderContentsDialog
import com.kvendy.chocolatetvlauncher.ui.home.menus.FolderContextMenu
import com.kvendy.chocolatetvlauncher.ui.preferences.PreferencesScreen
import com.kvendy.chocolatetvlauncher.ui.util.openAppInfo
import com.kvendy.chocolatetvlauncher.ui.util.requestUninstall
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Collections

/** What the currently open [AppContextMenu] is for: a plain grid app, or one living inside a folder. */
private data class AppMenuTarget(
    val app: AppInfo,
    val folder: HomeGridItem.FolderItem?,
    // False for search results: reordering a filtered/alphabetical subset doesn't map onto real
    // grid positions and was producing all kinds of bogus swaps/merges when used from there.
    val reorderEnabled: Boolean = true
)

@Composable
fun Launcher(
    preferencesData: PreferencesData,
    recentAppsTracker: RecentAppsTracker,
    allInstalledApps: List<AppInfo>,
    showSettings: Boolean,
    onShowSettingsChange: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val loadedHomeScreenState by remember(preferencesData, recentAppsTracker) {
        combine(
            preferencesData.iconCornerRadius,
            preferencesData.showAppLabels,
            preferencesData.appLabelScale,
            preferencesData.gridSize,
            preferencesData.folderGridSize,
            preferencesData.gridSquareIcons,
            preferencesData.showFeaturedTile,
            preferencesData.showRecommendationsOnHome,
            preferencesData.showNotifications,
            preferencesData.showNotificationsOnHome,
            preferencesData.contentCardScale,
            preferencesData.excludedContentApps,
            preferencesData.sectionTitleScale,
            preferencesData.sectionOrder,
            preferencesData.showTopBar,
            preferencesData.showClock,
            preferencesData.showWifiShortcut,
            preferencesData.showBluetoothShortcut,
            preferencesData.showRecentApps,
            preferencesData.recentAppsScale,
            preferencesData.recentAppsSquareIcons,
            preferencesData.recentAppsCornerRadius,
            preferencesData.maxRecentApps,
            preferencesData.hiddenApps,
            preferencesData.gridOrder,
            recentAppsTracker.recentApps,
            preferencesData.showPageTabLabels,
            preferencesData.showSectionTitles,
            preferencesData.showSectionIcons,
            preferencesData.showHiddenAppsInSearch
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            (HomeScreenState(
                iconCornerRadius = values[0] as Float,
                showAppLabels = values[1] as Boolean,
                appLabelScale = values[2] as Float,
                gridSize = values[3] as Int,
                folderGridSize = values[4] as Int,
                gridSquareIcons = values[5] as Boolean,
                showFeaturedTile = values[6] as Boolean,
                showRecommendationsOnHome = values[7] as Boolean,
                showNotifications = values[8] as Boolean,
                showNotificationsOnHome = values[9] as Boolean,
                contentCardScale = values[10] as Float,
                excludedContentApps = values[11] as Set<String>,
                sectionTitleScale = values[12] as Float,
                sectionOrder = values[13] as List<String>,
                showTopBar = values[14] as Boolean,
                showClock = values[15] as Boolean,
                showWifiShortcut = values[16] as Boolean,
                showBluetoothShortcut = values[17] as Boolean,
                showRecentApps = values[18] as Boolean,
                recentAppsScale = values[19] as Float,
                recentAppsSquareIcons = values[20] as Boolean,
                recentAppsCornerRadius = values[21] as Float,
                maxRecentApps = values[22] as Int,
                hiddenApps = values[23] as Set<String>,
                gridOrder = values[24] as List<HomeGridItem>,
                recentApps = values[25] as List<String>,
                showPageTabLabels = values[26] as Boolean,
                showSectionTitles = values[27] as Boolean,
                showSectionIcons = values[28] as Boolean,
                showHiddenAppsInSearch = values[29] as Boolean
            ))
        }
    }.collectAsState(initial = null)
    val homeScreenState = loadedHomeScreenState ?: HomeScreenState()
    // Readiness belongs to the same snapshot as the order used for reconciliation.
    val gridOrderLoaded = loadedHomeScreenState != null

    val showGlow by preferencesData.showGlowEffect.collectAsState(initial = true)
    val glowRadius by preferencesData.glowRadius.collectAsState(initial = LauncherDefaults.GLOW_RADIUS)

    var currentPage by remember { mutableStateOf(HomePage.Home) }
    // If a page's feature gets turned off (e.g. from the Preferences dialog open on top of that
    // very page) while it's the active page, its tab disappears from the status bar — snap back
    // to Home rather than stranding the user on a now-tabless page with only the remote's Back
    // button to escape it.
    LaunchedEffect(homeScreenState.showFeaturedTile, homeScreenState.showNotifications) {
        if (currentPage == HomePage.Recommendations && !homeScreenState.showFeaturedTile) currentPage = HomePage.Home
        if (currentPage == HomePage.Notifications && !homeScreenState.showNotifications) currentPage = HomePage.Home
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var continueWatching by remember { mutableStateOf<List<RecommendedContent>>(emptyList()) }
    var topPicks by remember { mutableStateOf<List<RecommendedContent>>(emptyList()) }

    // Refresh once per resumed session, only while the feature is enabled.
    // repeatOnLifecycle cancels an old refresh before starting another on return Home.
    LaunchedEffect(lifecycleOwner, homeScreenState.showFeaturedTile) {
        if (!homeScreenState.showFeaturedTile) {
            continueWatching = emptyList()
            topPicks = emptyList()
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            continueWatching = loadWatchNextPrograms(context)
            topPicks = loadTopPicks(context)
        }
    }

    // Entered via a tile's context menu ("Edit Mode") and left only by pressing Back — while
    // active, tapping/holding tiles edits the grid (swap/merge) instead of launching/opening them.
    // Kept separate from movingKey (below) so the session survives between individual moves: after
    // one swap/merge completes, movingKey clears back to null but isEditMode stays true, letting
    // the very next tap pick up a different tile without backing out and re-entering the menu.
    var isEditMode by remember { mutableStateOf(false) }
    // Identity of the app or folder currently picked up within an edit session, in
    // HomeGridItem.key format ("app:<pkg>" / "folder:<id>"). Null while isEditMode is true but
    // nothing's currently picked up (right after entering, or between moves).
    var movingKey by remember { mutableStateOf<String?>(null) }
    // Non-null only when movingKey was picked up from inside that folder's contents dialog —
    // scopes the drop target to reordering within that same folder instead of the top-level grid.
    var movingFromFolderId by remember { mutableStateOf<String?>(null) }
    var appMenuTarget by remember { mutableStateOf<AppMenuTarget?>(null) }
    var folderContextMenuFor by remember { mutableStateOf<HomeGridItem.FolderItem?>(null) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    // Flat index (top-level grid) or in-folder index TvAppGrid should keep focus pinned to after a
    // swap/merge — see TvAppGrid's own doc comment for why this is needed at all. Cleared on the
    // next pick-up so a later, unrelated grid change (e.g. installing an app) can't yank focus
    // back to a stale slot from a previous edit.
    var focusIndexRequest by remember { mutableStateOf<Int?>(null) }
    // Which grid focusIndexRequest belongs to: null for the top-level grid, a folder id for that
    // folder's own dialog grid. The top-level grid stays composed (just visually covered) the
    // whole time a folder dialog is open, so without this both grids would try to act on the same
    // index at once — the hidden top-level grid silently stealing focus back from the dialog.
    var focusIndexRequestFolderId by remember { mutableStateOf<String?>(null) }

    // Puts down whatever's currently picked up without leaving edit mode — used after a
    // swap/merge completes, or when the currently-held tile is tapped again to just drop it in
    // place, either way leaving the session open for the next pick-up.
    fun releaseHeld() {
        movingKey = null
        movingFromFolderId = null
    }

    fun exitEditMode() {
        releaseHeld()
        isEditMode = false
        focusIndexRequest = null
        focusIndexRequestFolderId = null
    }

    BackHandler {
        when {
            isEditMode -> exitEditMode()
            showSettings -> onShowSettingsChange(false)
            currentPage != HomePage.Home -> currentPage = HomePage.Home
            // Nothing to back out of once already at the true root — this app is registered as a
            // Home/Leanback launcher, and finishing that Activity on Back used to make the system
            // immediately relaunch it fresh (a full re-scan of every installed app, visible as an
            // unwanted "reload"), since a Home app always needs something to show. A no-op here
            // matches how real launchers behave: Back simply does nothing on the home screen.
            else -> {}
        }
    }

    val appsByPackage = remember(allInstalledApps) { allInstalledApps.associateBy { it.packageName } }

    // Reconciles the saved grid order against what's actually installed, for DISPLAY only:
    // drops references to currently-not-installed apps (recursively — including inside
    // folders, dropping a folder entirely if it empties out) so they don't show as broken
    // tiles, then appends newly-installed apps that aren't in the saved order yet.
    val orderedGridItems = remember(homeScreenState.gridOrder, allInstalledApps, gridOrderLoaded) {
        if (allInstalledApps.isEmpty() || !gridOrderLoaded) {
            // Apps haven't finished loading yet — e.g. right after an Activity recreation,
            // where allInstalledApps briefly starts empty before the async package query
            // returns. Treating that as "everything was uninstalled" would filter out every
            // saved item below. Show/keep the raw saved order as-is until we actually know
            // what's installed; this block re-runs once allInstalledApps populates. Same
            // reasoning for !gridOrderLoaded — see that flag's comment above.
            return@remember homeScreenState.gridOrder
        }
        val installedPackages = allInstalledApps.map { it.packageName }.toSet()
        val cleaned = homeScreenState.gridOrder.mapNotNull { item ->
            when (item) {
                is HomeGridItem.AppItem ->
                    if (item.packageName in installedPackages) item else null
                is HomeGridItem.FolderItem -> {
                    val remaining = item.packageNames.filter { it in installedPackages }
                    if (remaining.isEmpty()) null else item.copy(packageNames = remaining)
                }
            }
        }
        val knownPackages = cleaned.flatMap { item ->
            when (item) {
                is HomeGridItem.AppItem -> listOf(item.packageName)
                is HomeGridItem.FolderItem -> item.packageNames
            }
        }.toSet()
        val newItems = installedPackages.filter { it !in knownPackages }.map { HomeGridItem.AppItem(it) }

        cleaned + newItems
    }

    // Persistence uses the latest saved order, never the filtered display projection.
    LaunchedEffect(allInstalledApps, gridOrderLoaded) {
        if (gridOrderLoaded) {
            preferencesData.appendNewApps(allInstalledApps.map { it.packageName })
        }
    }

    fun saveOrder(newOrder: List<HomeGridItem>) {
        coroutineScope.launch { preferencesData.saveGridOrder(newOrder) }
    }

    // Exchange the moving item and target's positions — the same predictable behavior
    // reordering always had, before folders existed. Triggered directly by a short tap on
    // the target in edit mode, no confirmation menu in between.
    fun swapPositions(targetKey: String) {
        val current = orderedGridItems.toMutableList()
        val fromIndex = current.indexOfFirst { it.key == movingKey }
        val toIndex = current.indexOfFirst { it.key == targetKey }
        if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
            Collections.swap(current, fromIndex, toIndex)
            saveOrder(current)
            // A plain swap never changes the target's own index — pin focus there so it
            // stays on the slot the user was looking at, not on the item that used to be
            // there (which just moved to fromIndex instead).
            focusIndexRequest = toIndex
            focusIndexRequestFolderId = null
        }
        releaseHeld()
    }

    // Same swap, scoped to one folder's internal package list. There's no folder-vs-app
    // choice to make in here (no nested folders), so this is the sole action.
    fun swapWithinFolder(folderId: String, movingPackage: String, targetPackage: String) {
        val current = orderedGridItems.toMutableList()
        val folderIndex = current.indexOfFirst { it.key == "folder:$folderId" }
        val folder = current.getOrNull(folderIndex) as? HomeGridItem.FolderItem
        if (folder != null) {
            val pkgs = folder.packageNames.toMutableList()
            val fromIndex = pkgs.indexOf(movingPackage)
            val toIndex = pkgs.indexOf(targetPackage)
            if (fromIndex != -1 && toIndex != -1) {
                Collections.swap(pkgs, fromIndex, toIndex)
                current[folderIndex] = folder.copy(packageNames = pkgs)
                saveOrder(current)
                // Same reasoning as swapPositions, scoped to this folder's own grid instead.
                focusIndexRequest = toIndex
                focusIndexRequestFolderId = folderId
            }
        }
        releaseHeld()
    }

    // Merges the moving app into the target, creating a new folder (target was a plain app)
    // or joining an existing one (target was already a folder). Only single apps can be
    // merged this way — moving a folder onto something only ever swaps, it never nests
    // folders. Triggered directly by a hold-press on the target in edit mode.
    fun mergeInto(targetKey: String) {
        val current = orderedGridItems.toMutableList()
        val fromIndex = current.indexOfFirst { it.key == movingKey }
        val toIndex = current.indexOfFirst { it.key == targetKey }
        val movingItem = current.getOrNull(fromIndex)
        if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex || movingItem !is HomeGridItem.AppItem) {
            releaseHeld()
            return
        }
        current.removeAt(fromIndex)
        val adjustedToIndex = if (fromIndex < toIndex) toIndex - 1 else toIndex
        when (val target = current[adjustedToIndex]) {
            is HomeGridItem.FolderItem ->
                current[adjustedToIndex] = target.copy(packageNames = target.packageNames + movingItem.packageName)
            is HomeGridItem.AppItem ->
                current[adjustedToIndex] = HomeGridItem.FolderItem(
                    id = newFolderId(),
                    name = null,
                    packageNames = listOf(target.packageName, movingItem.packageName)
                )
        }
        saveOrder(current)
        // The merged-into slot (now a folder, or a folder with one more member) ends up at
        // adjustedToIndex — same "pin focus to the slot, not the item" reasoning as swaps.
        // Merging only ever happens at the top level (folders never nest).
        focusIndexRequest = adjustedToIndex
        focusIndexRequestFolderId = null
        releaseHeld()
    }

    // Single entry point for every tap/hold on a top-level grid tile while isEditMode is
    // active. Nothing held yet -> pick this tile up. Same tile tapped again -> put it down in
    // place. Otherwise -> hold-press merges (apps only; folders never nest, so a held folder
    // always falls back to swap), short tap swaps.
    fun handleEditTap(key: String, longPress: Boolean) {
        val heldKey = movingKey
        when {
            heldKey == null -> {
                movingKey = key
                focusIndexRequest = null
                focusIndexRequestFolderId = null
            }
            heldKey == key -> releaseHeld()
            else -> {
                val heldItem = orderedGridItems.find { it.key == heldKey }
                if (longPress && heldItem is HomeGridItem.AppItem) mergeInto(key) else swapPositions(key)
            }
        }
    }

    // Same idea as handleEditTap, scoped to one folder's own contents — every tap/hold
    // inside FolderContentsDialog while isEditMode is active goes through here instead of the
    // dialog's own onAppClicked/onAppLongClicked checking movingPackage directly, which broke
    // after the first swap: swapWithinFolder already clears movingKey to keep the edit session
    // open for the next pick-up (same as the top-level grid), but "nothing held" was being
    // read as "not editing" there, so the very next tap launched the app instead, and a
    // hold-press opened its context menu instead of picking it up. No long-press-vs-short
    // distinction here (unlike the top-level grid) — folders never nest, so there's nothing
    // for a hold-press to merge into; it's always a swap.
    fun handleFolderEditTap(folder: HomeGridItem.FolderItem, packageName: String) {
        val heldPackage = if (movingKey != null && movingFromFolderId == folder.id) {
            movingKey?.removePrefix("app:")
        } else null
        when {
            heldPackage == null -> {
                movingKey = HomeGridItem.AppItem(packageName).key
                movingFromFolderId = folder.id
                focusIndexRequest = null
                focusIndexRequestFolderId = null
            }
            heldPackage == packageName -> releaseHeld()
            else -> swapWithinFolder(folder.id, heldPackage, packageName)
        }
    }

    fun launchApp(app: AppInfo) {
        coroutineScope.launch {
            recentAppsTracker.addRecentApp(app.packageName, homeScreenState.maxRecentApps)
        }
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.component = app.componentName
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Only fallback to package launch if specific component fails
            context.packageManager.getLaunchIntentForPackage(app.packageName)
                ?.let { context.startActivity(it) }
        }
    }

    // Shared by both pages: search results are a filtered/alphabetical subset of the grid,
    // not positions in it, so long-clicking one never enters edit mode the way the grid's
    // own handler below does.
    val onSearchAppLongClicked: (AppInfo) -> Unit = { app ->
        appMenuTarget = AppMenuTarget(app, folder = null, reorderEnabled = false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Rendered once here rather than inside each page, so switching pages doesn't tear
        // down and remount it — see HomeBackground's doc comment for the white-flash bug that
        // caused.
        HomeBackground(preferencesData = preferencesData)

        // Home stays permanently composed instead of living inside the Crossfade below — it's
        // the one expensive page (TvAppGrid's FocusRequesters + layout scale with app count),
        // so tearing it down and rebuilding it on every trip back from Recommendations/
        // Notifications was heavy enough to blow through the fade's own frame budget: the
        // animation got skipped rather than played. Only alpha (and, once fully faded out, an
        // off-screen offset — see homeOffscreen below) changes now; the composition, its scroll
        // position, and its in-flight Coil requests never restart.
        var homeOffscreen by remember { mutableStateOf(currentPage != HomePage.Home) }
        val homeAlpha by animateFloatAsState(
            targetValue = if (currentPage == HomePage.Home) 1f else 0f,
            animationSpec = tween(),
            label = "HomeFade",
            finishedListener = { value -> if (value == 0f) homeOffscreen = true }
        )
        LaunchedEffect(currentPage) {
            if (currentPage == HomePage.Home) homeOffscreen = false
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = homeAlpha }
                // Compose's spatial focus search is bounds-based and ignores alpha entirely —
                // a plain alpha fade alone would still let a D-pad press land on an invisible
                // grid tile. canFocus=false on a wrapping node doesn't stop search from
                // reaching focusable descendants either (verified against Compose UI's own
                // TwoDimensionalFocusSearch source: a deactivated node's children are still
                // collected as candidates). Moving the whole thing far outside the viewport
                // once it's fully invisible is what actually keeps it out of the candidate
                // pool, so it's only applied after the fade-out finishes — while the fade is
                // still playing, this needs to stay in place for the dissolve to be visible.
                .then(if (homeOffscreen) Modifier.offset(x = 20000.dp) else Modifier)
        ) {
            HomeScreen(
                preferencesData = preferencesData,
                allApps = allInstalledApps,
                gridItems = orderedGridItems,
                recentApps = homeScreenState.recentApps.mapNotNull { appsByPackage[it] },
                state = homeScreenState,
                movingKey = movingKey,
                isEditMode = isEditMode,
                focusIndexRequest = if (focusIndexRequestFolderId == null) focusIndexRequest else null,
                currentPage = currentPage,
                onPageSelected = { currentPage = it },
                continueWatching = continueWatching,
                topPicks = topPicks,
                onAppClicked = { app ->
                    if (isEditMode) {
                        handleEditTap(HomeGridItem.AppItem(app.packageName).key, longPress = false)
                    } else {
                        launchApp(app)
                    }
                },
                onAppLongClicked = { app ->
                    if (isEditMode) {
                        handleEditTap(HomeGridItem.AppItem(app.packageName).key, longPress = true)
                    } else {
                        appMenuTarget = AppMenuTarget(app, folder = null)
                    }
                },
                onSearchAppLongClicked = onSearchAppLongClicked,
                onFolderClicked = { folder ->
                    if (isEditMode) {
                        handleEditTap(folder.key, longPress = false)
                    } else {
                        openFolderId = folder.id
                    }
                },
                onFolderLongClicked = { folder ->
                    if (isEditMode) {
                        handleEditTap(folder.key, longPress = true)
                    } else {
                        folderContextMenuFor = folder
                    }
                },
                onPreferencesClicked = { onShowSettingsChange(true) },
                onRemoveRecentApp = { app ->
                    coroutineScope.launch { recentAppsTracker.removeRecentApp(app.packageName) }
                },
                onClearRecentApps = {
                    coroutineScope.launch { recentAppsTracker.clearHistory() }
                }
            )
        }

        // A slight cross-fade rather than an instant swap: each page loads its own content
        // (grid images, recommendations, notifications) on its own schedule, so switching
        // straight to a still-half-loaded page read as an abrupt, unfinished-looking cut. Home
        // renders in its own persistent layer above instead of through here, so this only ever
        // needs to animate between Recommendations/Notifications and nothing.
        Crossfade(targetState = currentPage, label = "PageSwitch") { page ->
            when (page) {
                HomePage.Home -> {}
                HomePage.Recommendations -> RecommendationsScreen(
                    preferencesData = preferencesData,
                    allApps = allInstalledApps,
                    state = homeScreenState,
                    currentPage = currentPage,
                    onPageSelected = { currentPage = it },
                    continueWatching = continueWatching,
                    topPicks = topPicks,
                    onAppClicked = { app -> launchApp(app) },
                    onSearchAppLongClicked = onSearchAppLongClicked,
                    onPreferencesClicked = { onShowSettingsChange(true) }
                )
                HomePage.Notifications -> NotificationsScreen(
                    preferencesData = preferencesData,
                    allApps = allInstalledApps,
                    state = homeScreenState,
                    currentPage = currentPage,
                    onPageSelected = { currentPage = it },
                    onAppClicked = { app -> launchApp(app) },
                    onSearchAppLongClicked = onSearchAppLongClicked,
                    onPreferencesClicked = { onShowSettingsChange(true) }
                )
            }
        }
    }

    appMenuTarget?.let { target ->
        val inFolder = target.folder
        AppContextMenu(
            app = target.app,
            onDismiss = { appMenuTarget = null },
            reorderEnabled = target.reorderEnabled,
            onReorder = {
                movingKey = HomeGridItem.AppItem(target.app.packageName).key
                movingFromFolderId = inFolder?.id
                isEditMode = true
                focusIndexRequest = null
                focusIndexRequestFolderId = null
                appMenuTarget = null
            },
            onHide = {
                coroutineScope.launch { preferencesData.hideApp(target.app.packageName) }
                appMenuTarget = null
            },
            onAppInfo = {
                context.openAppInfo(target.app.packageName)
                appMenuTarget = null
            },
            onUninstall = {
                context.requestUninstall(target.app.packageName)
                appMenuTarget = null
            },
            onRemoveFromFolder = inFolder?.let { folder ->
                {
                    val current = orderedGridItems.toMutableList()
                    val idx = current.indexOfFirst { it.key == folder.key }
                    if (idx != -1) {
                        val remaining = folder.packageNames.filter { it != target.app.packageName }
                        if (remaining.isEmpty()) {
                            current.removeAt(idx)
                        } else {
                            current[idx] = folder.copy(packageNames = remaining)
                        }
                        current.add(idx.coerceAtMost(current.size), HomeGridItem.AppItem(target.app.packageName))
                        saveOrder(current)
                    }
                    appMenuTarget = null
                }
            }
        )
    }

    folderContextMenuFor?.let { folder ->
        FolderContextMenu(
            folder = folder,
            onDismiss = { folderContextMenuFor = null },
            onReorder = {
                movingKey = folder.key
                isEditMode = true
                focusIndexRequest = null
                focusIndexRequestFolderId = null
                folderContextMenuFor = null
            },
            onRename = { newName ->
                val current = orderedGridItems.toMutableList()
                val idx = current.indexOfFirst { it.key == folder.key }
                if (idx != -1) {
                    current[idx] = folder.copy(name = newName.trim().ifBlank { null })
                    saveOrder(current)
                }
            },
            onRemoveFolder = {
                val current = orderedGridItems.toMutableList()
                val idx = current.indexOfFirst { it.key == folder.key }
                if (idx != -1) {
                    current.removeAt(idx)
                    current.addAll(idx, folder.packageNames.map { HomeGridItem.AppItem(it) })
                    saveOrder(current)
                }
                folderContextMenuFor = null
                if (openFolderId == folder.id) openFolderId = null
            }
        )
    }

    val openFolder = orderedGridItems.filterIsInstance<HomeGridItem.FolderItem>().find { it.id == openFolderId }
    openFolder?.let { folder ->
        // Only meaningful while a move was picked up from within this same folder — see
        // movingFromFolderId's doc comment for why cross-folder moves aren't reachable.
        val movingPackage = if (movingKey != null && movingFromFolderId == folder.id) {
            movingKey?.removePrefix("app:")
        } else null

        FolderContentsDialog(
            folder = folder,
            // Same hidden-apps filtering HomeScreen applies to the top-level grid — this
            // dialog reads straight from orderedGridItems, which doesn't go through that
            // filtering, so a hidden app sitting inside a folder would otherwise still show
            // up here even though it's gone everywhere else.
            apps = folder.packageNames.filter { it !in homeScreenState.hiddenApps }.mapNotNull { appsByPackage[it] },
            gridSize = homeScreenState.folderGridSize,
            iconCornerRadius = homeScreenState.iconCornerRadius,
            showAppLabels = homeScreenState.showAppLabels,
            showGlow = showGlow,
            glowRadius = glowRadius,
            labelScale = homeScreenState.appLabelScale,
            renderAsSquare = homeScreenState.gridSquareIcons,
            movingPackage = movingPackage,
            focusIndexRequest = if (focusIndexRequestFolderId == folder.id) focusIndexRequest else null,
            onDismiss = { openFolderId = null },
            onAppClicked = { app ->
                if (isEditMode) {
                    handleFolderEditTap(folder, app.packageName)
                } else {
                    openFolderId = null
                    launchApp(app)
                }
            },
            onAppLongClicked = { app ->
                if (isEditMode) {
                    handleFolderEditTap(folder, app.packageName)
                } else {
                    appMenuTarget = AppMenuTarget(app, folder = folder)
                }
            }
        )
    }

    if (showSettings) {
        PreferencesScreen(
            preferencesData = preferencesData,
            apps = allInstalledApps,
            onDismiss = { onShowSettingsChange(false) }
        )
    }
}
