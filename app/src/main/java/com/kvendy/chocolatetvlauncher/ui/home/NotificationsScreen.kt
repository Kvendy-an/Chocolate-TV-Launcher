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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import com.kvendy.chocolatetvlauncher.AppImageModel
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.AppNotification
import com.kvendy.chocolatetvlauncher.data.HomeScreenState
import com.kvendy.chocolatetvlauncher.data.LauncherNotificationListenerService
import com.kvendy.chocolatetvlauncher.data.NotificationStore
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.AmbientGlowColorSpec
import com.kvendy.chocolatetvlauncher.ui.components.GlowPosition
import com.kvendy.chocolatetvlauncher.ui.components.SmallScaleButton
import com.kvendy.chocolatetvlauncher.ui.components.ambientGlow
import com.kvendy.chocolatetvlauncher.ui.home.menus.TvInputMenu
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

/**
 * The dedicated Notifications page: a flat, reverse-chronological list (NotificationStore already
 * keeps newest-first) rather than grouped by app like Recommendations — that's how a notification
 * list conventionally reads, and there's no natural "per-app row" grouping for it the way media
 * content has.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NotificationsScreen(
    preferencesData: PreferencesData,
    allApps: List<AppInfo>,
    state: HomeScreenState,
    currentPage: HomePage,
    onPageSelected: (HomePage) -> Unit,
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

    val notificationsShown = if (state.showNotifications) NotificationStore.notifications else emptyList()
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

        if (!isSearching && showGlow) {
            Box(modifier = Modifier.fillMaxSize().ambientGlow(color = { animatedGlowColor }, position = GlowPosition.fromId(glowPosition)))
        }

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

        // Keyed by notification key rather than recreated with the list, so an entry stays valid
        // (and attached to its still-composed row) right up until the moment it's dismissed —
        // needed below to redirect focus to a neighbor before removal happens.
        val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
        fun focusRequesterFor(key: String) = focusRequesters.getOrPut(key) { FocusRequester() }

        // Dismissing the only remaining notification has no neighbor to hand focus to — the empty
        // state text doesn't exist yet at that instant (it only appears once the list recomposes
        // as empty), so its FocusRequester isn't attached to anything and can't take focus
        // synchronously. This flag defers that handoff to the LaunchedEffect below, which runs
        // after that recomposition has actually happened.
        val emptyStateFocusRequester = remember { FocusRequester() }
        var focusEmptyStateNext by remember { mutableStateOf(false) }
        LaunchedEffect(notificationsShown.isEmpty()) {
            if (notificationsShown.isEmpty() && focusEmptyStateNext) {
                emptyStateFocusRequester.requestFocus()
                focusEmptyStateNext = false
            }
        }

        fun dismissNotification(notification: AppNotification) {
            // Without this, removing the focused row leaves Compose's default focus-search to
            // pick whatever it finds next — which was landing all the way up on the StatusBar's
            // Home tab, one accidental press away from switching pages. Moving focus to a
            // neighboring row first keeps it exactly where the user still is.
            val index = notificationsShown.indexOf(notification)
            val neighborKey = when {
                index < 0 -> null
                index < notificationsShown.lastIndex -> notificationsShown[index + 1].key
                index > 0 -> notificationsShown[index - 1].key
                else -> null
            }
            if (neighborKey != null) {
                focusRequesters[neighborKey]?.requestFocus()
            } else {
                focusEmptyStateNext = true
            }
            LauncherNotificationListenerService.dismiss(notification.key)
            NotificationStore.notifications.remove(notification)
            focusRequesters.remove(notification.key)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.ExtraLarge,
                end = Spacing.ExtraLarge,
                top = Spacing.ExtraLarge,
                bottom = Spacing.ExtraLarge + 50.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            item {
                StatusBar(
                    currentPage = currentPage,
                    ownPage = HomePage.Notifications,
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

            if (notificationsShown.isNotEmpty()) {
                item {
                    val showHeader = state.showSectionTitles || state.showSectionIcons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small),
                        horizontalArrangement = if (showHeader) Arrangement.SpaceBetween else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(
                            text = stringResource(R.string.section_notifications),
                            icon = Icons.Default.Notifications,
                            scale = state.sectionTitleScale,
                            showLabel = state.showSectionTitles,
                            showIcon = state.showSectionIcons
                        )
                        SmallScaleButton(onClick = {
                            NotificationStore.clear(context)
                        }) { Text("Clear All") }
                    }
                }
            }

            if (notificationsShown.isEmpty()) {
                item {
                    // A real (if no-op) clickable Surface, not a plain Text, so it can actually
                    // take and show focus when the last notification's dismiss hands off to it —
                    // see focusEmptyStateNext above.
                    Surface(
                        onClick = {},
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .padding(top = Spacing.ExtraLarge)
                            .focusRequester(emptyStateFocusRequester)
                    ) {
                        Text(
                            text = if (!state.showNotifications) {
                                "Notifications are turned off — enable them in Preferences to see them here."
                            } else if (!LauncherNotificationListenerService.isEnabled(context)) {
                                "Notification access hasn't been granted yet — enable it in Preferences to see notifications here."
                            } else {
                                "No notifications right now."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.Small)
                        )
                    }
                }
            } else {
                items(notificationsShown, key = { it.key }) { notification ->
                    val sourceApp = appsByPackage[notification.packageName]
                    ListItem(
                        selected = false,
                        onClick = { openNotification(notification) },
                        // A nested IconButton in trailingContent isn't independently focusable —
                        // the whole ListItem is one D-pad focus stop, so that button could never
                        // actually be reached/pressed. Long-press for dismiss instead, the same
                        // "hold for secondary actions" convention used everywhere else (app tiles'
                        // context menu, NotificationRow's cards).
                        onLongClick = { dismissNotification(notification) },
                        scale = ListItemDefaults.scale(focusedScale = 1.01f),
                        // Not themedListItemColors() here: its default containerColor is fully
                        // transparent (fine for a Preferences list over a plain surface, but this
                        // page sits over the wallpaper/gradient, and each row was invisible until
                        // focused). Semi-transparent at rest so the background still shows
                        // through; fully opaque once focused so the focused row still stands out.
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            focusedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.focusRequester(focusRequesterFor(notification.key)),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sourceApp != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(AppImageModel(appInfo = sourceApp, isBanner = false)),
                                        contentDescription = sourceApp.label,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null)
                                }
                            }
                        },
                        headlineContent = { Text(notification.title ?: sourceApp?.label ?: notification.packageName) },
                        supportingContent = notification.text?.let { text -> { Text(text) } },
                        // Purely a visual hint that holding this row dismisses it — not its own
                        // clickable target (see onLongClick above for why).
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hold to dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
