package com.kvendy.chocolatetvlauncher.ui.preferences

import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Title
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.TvSearchField
import com.kvendy.chocolatetvlauncher.ui.components.themedListItemColors
import com.kvendy.chocolatetvlauncher.ui.components.themedSurfaceColors
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

internal enum class PreferencePage(
    @StringRes val title: Int,
    val icon: ImageVector,
    val parent: PreferencePage? = null,
    @StringRes val summary: Int = 0
) {
    Root(R.string.preferences_title, Icons.Default.Settings),
    Home(R.string.preferences_home, Icons.Default.Home, Root, R.string.preferences_home_summary),
    Appearance(R.string.preferences_appearance, Icons.Default.Palette, Root, R.string.preferences_appearance_summary),
    Apps(R.string.preferences_apps, Icons.Default.Apps, Root, R.string.preferences_apps_summary),
    Recommendations(R.string.preferences_recommendations, Icons.Default.Movie, Root, R.string.preferences_recommendations_summary),
    Notifications(R.string.preferences_notifications, Icons.Default.Notifications, Root, R.string.preferences_notifications_summary),
    RemoteButtons(R.string.preferences_remote_buttons, Icons.Default.SettingsRemote, Root, R.string.preferences_remote_buttons_summary),
    System(R.string.preferences_system, Icons.Default.Settings, Root, R.string.preferences_system_summary),
    Backup(R.string.preferences_backup, Icons.Default.Backup, Root, R.string.preferences_backup_summary),
    SectionOrder(R.string.preferences_section_order, Icons.AutoMirrored.Filled.Sort, Home),
    SectionHeaders(R.string.preferences_section_headers, Icons.Default.Title, Home),
    NavigationBars(R.string.preferences_navigation_bars, Icons.AutoMirrored.Filled.ViewList, Home),
    RecentApps(R.string.preferences_recent_apps, Icons.Default.History, Home),
    Theme(R.string.preferences_theme, Icons.Default.Image, Appearance),
    TileStyle(R.string.preferences_tile_style, Icons.Default.RoundedCorner, Appearance),
    FocusGlow(R.string.preferences_focus_glow, Icons.Default.Contrast, Appearance),
    AppGrid(R.string.preferences_app_grid, Icons.Default.GridView, Apps),
    Search(R.string.preferences_search_page, Icons.Default.Search, Apps),
    ManageApps(R.string.preferences_manage_apps, Icons.Default.Apps, Apps);

    companion object {
        val childrenByPage = entries.groupBy { it.parent }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PreferencesScreen(
    preferencesData: PreferencesData,
    apps: List<AppInfo>,
    onDismiss: () -> Unit
) {
    var page by rememberSaveable { mutableStateOf(PreferencePage.Root) }
    var returningFrom by rememberSaveable { mutableStateOf<PreferencePage?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var openedResult by rememberSaveable { mutableStateOf<String?>(null) }
    var searchDestination by rememberSaveable { mutableStateOf<PreferencePage?>(null) }
    var focusFirstResult by remember { mutableStateOf(false) }
    val searchFieldFocus = remember { FocusRequester() }
    val clearSearchFocus = remember { FocusRequester() }
    val searching = page == PreferencePage.Root && query.isNotBlank()

    fun clearSearch() {
        searchFieldFocus.requestFocus()
        openedResult = null
        focusFirstResult = false
        query = ""
    }

    fun navigateBack() {
        when {
            page == searchDestination -> {
                searchDestination = null
                page = PreferencePage.Root
            }
            page == PreferencePage.Root && query.isNotEmpty() -> clearSearch()
            page == PreferencePage.Root -> onDismiss()
            else -> {
                returningFrom = page
                page = page.parent ?: PreferencePage.Root
            }
        }
    }

    // The dialog scopes D-pad focus while the launcher stays visible as a live preview.
    Dialog(
        onDismissRequest = ::navigateBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setDimAmount(0f)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        val children = PreferencePage.childrenByPage[page].orEmpty()
        val menuFocus = returningFrom?.takeIf { it in children } ?: children.firstOrNull()
        val pageFocus = remember(page) { FocusRequester() }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(minOf(maxWidth * 0.58f, 550.dp))
                    .fillMaxHeight()
                    .background(lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primary, 0.06f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (page != PreferencePage.Root) {
                        Surface(
                            onClick = ::navigateBack,
                            modifier = if (menuFocus == null) Modifier.focusRequester(pageFocus) else Modifier,
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                            shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
                            colors = themedSurfaceColors()
                        ) {
                            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                            }
                        }
                    }
                    Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall)
                }

                Box(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.Large).height(1.dp)
                        .background(MaterialTheme.colorScheme.border)
                )

                val results = if (searching) rememberPreferenceSearchResults(query) else emptyList()
                if (page == PreferencePage.Root) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = Spacing.Large).padding(top = Spacing.Medium),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvSearchField(
                            query = query,
                            onQueryChange = {
                                openedResult = null
                                focusFirstResult = false
                                query = it
                            },
                            modifier = Modifier.weight(1f).focusRequester(searchFieldFocus),
                            placeholder = stringResource(R.string.preferences_search),
                            onSearch = {
                                when {
                                    results.isNotEmpty() -> {
                                        openedResult = null
                                        focusFirstResult = true
                                    }
                                    query.isNotEmpty() -> clearSearchFocus.requestFocus()
                                    else -> pageFocus.requestFocus()
                                }
                            }
                        )
                        if (query.isNotEmpty()) {
                            Surface(
                                onClick = ::clearSearch,
                                modifier = Modifier.focusRequester(clearSearchFocus),
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                                shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
                                colors = themedSurfaceColors()
                            ) {
                                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Close, stringResource(R.string.preferences_search_clear))
                                }
                            }
                        }
                    }
                }

                key(page) {
                    when {
                        searching -> key(query) {
                            PreferenceSearchResults(
                                results = results,
                                returningTo = openedResult,
                                focusFirst = focusFirstResult,
                                onFirstFocused = { focusFirstResult = false }
                            ) { result ->
                                returningFrom = null
                                focusFirstResult = false
                                openedResult = result.key
                                searchDestination = result.page
                                page = result.page
                            }
                        }
                        children.isNotEmpty() -> PreferenceRows {
                            children.forEach { child ->
                                ListItem(
                                    modifier = if (child == menuFocus) Modifier.focusRequester(pageFocus) else Modifier,
                                    selected = false,
                                    onClick = {
                                        returningFrom = null
                                        page = child
                                    },
                                    leadingContent = { Icon(child.icon, null, Modifier.size(22.dp)) },
                                    headlineContent = { Text(stringResource(child.title)) },
                                    supportingContent = if (child.summary == 0) null else {
                                        { Text(stringResource(child.summary)) }
                                    },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                    scale = ListItemDefaults.scale(focusedScale = 1f),
                                    colors = themedListItemColors()
                                )
                            }
                        }
                        page == PreferencePage.RemoteButtons -> RemoteButtonsPreference(apps)
                        page == PreferencePage.ManageApps -> ManageAppsPreference(preferencesData, apps)
                        page == PreferencePage.Recommendations -> FeaturedAppPreference(preferencesData, apps)
                        else -> PreferenceRows {
                            when (page) {
                                PreferencePage.SectionOrder -> SectionOrderPreference(preferencesData)
                                PreferencePage.SectionHeaders -> SectionHeadersPreference(preferencesData)
                                PreferencePage.NavigationBars -> StatusBarPreference(preferencesData)
                                PreferencePage.RecentApps -> RecentAppsPreference(preferencesData)
                                PreferencePage.Theme -> ThemePreference(preferencesData)
                                PreferencePage.TileStyle -> TileStylePreference(preferencesData)
                                PreferencePage.FocusGlow -> FocusGlowPreference(preferencesData)
                                PreferencePage.AppGrid -> GridAppearancePreference(preferencesData)
                                PreferencePage.Notifications -> NotificationsPreference(preferencesData)
                                PreferencePage.System -> SystemPreference(preferencesData)
                                PreferencePage.Search -> SearchPreference(preferencesData)
                                PreferencePage.Backup -> BackupPreference()
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(page) {
            if (!searching) pageFocus.requestFocus()
        }
    }
}

private val preferencesPadding = PaddingValues(horizontal = Spacing.Large, vertical = Spacing.Medium)

@Composable
private fun PreferenceRows(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(preferencesPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
    ) {
        content()
    }
}

@Composable
internal fun PreferenceList(
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = preferencesPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        content = content
    )
}
