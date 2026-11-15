package com.kvendy.chocolatetvlauncher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.DisposableEffect
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import java.util.Date

private val BarIconSize = 18.dp
private val BarButtonPadding = 9.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatusBar(
    currentPage: HomePage,
    // Which page this particular StatusBar instance belongs to (Home/Recommendations/
    // Notifications) — see PageSwitcher's doc comment for why this needs to be explicit now that
    // Home stays permanently composed instead of remounting on every switch.
    ownPage: HomePage,
    onPageSelected: (HomePage) -> Unit,
    // Home is always implicitly visible — this only lists the extra pages currently reachable
    // (Recommendations/Notifications), each independently toggleable in Preferences.
    visiblePages: Set<HomePage>,
    onPreferencesClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onInputsClicked: () -> Unit,
    showOptionalItems: Boolean,
    showClock: Boolean,
    showPageTabLabels: Boolean,
    showWifiShortcut: Boolean,
    showBluetoothShortcut: Boolean,
    showInputShortcut: Boolean,
    showSearchShortcut: Boolean,
    showSettingsShortcut: Boolean,
    isReordering: Boolean
) {
    val showTime = showOptionalItems && showClock
    val showNavigation = showOptionalItems || visiblePages.size > 1
    val groupStyle = Modifier
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), RoundedCornerShape(50))
        .padding(Spacing.ExtraSmall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showNavigation) {
            Row(
                modifier = Modifier.weight(1f, fill = false).then(groupStyle),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTime) {
                    Clock(modifier = Modifier.padding(horizontal = Spacing.ExtraSmall))
                    Spacer(
                        Modifier.width(1.dp).height(20.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f))
                    )
                }
                PageSwitcher(
                    currentPage = currentPage,
                    ownPage = ownPage,
                    visiblePages = visiblePages,
                    onPageSelected = onPageSelected,
                    enabled = !isReordering,
                    labelsEnabled = showPageTabLabels,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.padding(start = if (showNavigation) Spacing.Medium else 0.dp).then(groupStyle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
        ) {
            if (showOptionalItems) {
                val context = LocalContext.current
                if (showWifiShortcut) {
                    StatusBarButton(Icons.Filled.Wifi, "Wi-Fi", enabled = !isReordering) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                        } catch (e: Exception) {
                            // Fallback to general settings or ignore if unavailable
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }
                        }
                    }
                }
                if (showBluetoothShortcut) {
                    StatusBarButton(Icons.Filled.Bluetooth, "Bluetooth", enabled = !isReordering) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        } catch (e: Exception) {
                            // Fallback to general settings or ignore if unavailable
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }
                        }
                    }
                }
                if (showInputShortcut) {
                    StatusBarButton(
                        icon = Icons.AutoMirrored.Filled.Input,
                        label = "TV Inputs",
                        enabled = !isReordering,
                        onClick = onInputsClicked
                    )
                }
                if (showSearchShortcut) {
                    StatusBarButton(
                        icon = Icons.Default.Search,
                        label = "Search",
                        enabled = !isReordering,
                        onClick = onSearchClicked
                    )
                }
                if (showSettingsShortcut) {
                    StatusBarButton(
                        icon = Icons.Default.Settings,
                        label = "System Settings",
                        enabled = !isReordering,
                        onClick = onSettingsClicked
                    )
                }
            }
            StatusBarButton(Icons.Default.Palette, "Preferences", enabled = !isReordering, onClick = onPreferencesClicked)
        }
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    var time by remember { mutableStateOf(DateFormat.getTimeFormat(context).format(Date())) }
    DisposableEffect(context, lifecycleOwner, configuration) {
        var registered = false
        fun update() { time = DateFormat.getTimeFormat(context).format(Date()) }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = update()
        }
        fun stop() {
            if (registered) {
                context.unregisterReceiver(receiver)
                registered = false
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    update()
                    if (!registered) {
                        ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
                            addAction(Intent.ACTION_TIME_TICK)
                            addAction(Intent.ACTION_TIME_CHANGED)
                            addAction(Intent.ACTION_TIMEZONE_CHANGED)
                        }, ContextCompat.RECEIVER_NOT_EXPORTED)
                        registered = true
                    }
                }
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
        }
    }
    Text(time, modifier = modifier, style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatusBarButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) colors.primaryContainer else Color.Transparent,
            focusedContainerColor = colors.primary,
            contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            focusedContentColor = colors.onPrimary
        )
    ) {
        Row(
            modifier = Modifier.heightIn(min = 36.dp).padding(horizontal = BarButtonPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = if (showLabel) null else label, modifier = Modifier.size(BarIconSize))
            if (showLabel) Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PageSwitcher(
    currentPage: HomePage,
    ownPage: HomePage,
    visiblePages: Set<HomePage>,
    onPageSelected: (HomePage) -> Unit,
    enabled: Boolean,
    labelsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val pages = remember(visiblePages) {
        HomePage.entries.filter { it == HomePage.Home || it in visiblePages }
    }
    val selectedFocusRequester = remember { FocusRequester() }
    LaunchedEffect(currentPage, pages.size) {
        // The single Home tab should leave initial focus to the existing Home content.
        if (currentPage == ownPage && pages.size > 1) selectedFocusRequester.requestFocus()
    }

    val tabGap = Spacing.ExtraSmall
    val labeledWidth = if (labelsEnabled) {
        val labelStyle = MaterialTheme.typography.titleSmall
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        remember(pages, labelStyle, textMeasurer, density) {
            val textWidth = pages.sumOf {
                textMeasurer.measure(it.label, style = labelStyle, maxLines = 1, softWrap = false).size.width
            }
            with(density) { textWidth.toDp() } +
                (BarIconSize + Spacing.Small + BarButtonPadding * 2) * pages.size + tabGap * (pages.size - 1)
        }
    } else {
        0.dp
    }

    BoxWithConstraints(modifier) {
        val showLabels = labelsEnabled && maxWidth >= labeledWidth + Spacing.Small
        Row(
            horizontalArrangement = Arrangement.spacedBy(tabGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.forEach { page ->
                val isSelected = page == currentPage
                val icon = when (page) {
                    HomePage.Home -> Icons.Default.Home
                    HomePage.Recommendations -> Icons.Default.Movie
                    HomePage.Notifications -> Icons.Default.Notifications
                }
                StatusBarButton(
                    icon = icon,
                    label = page.label,
                    enabled = enabled,
                    showLabel = showLabels,
                    isSelected = isSelected,
                    modifier = Modifier.semantics { selected = isSelected }
                        .then(if (isSelected) Modifier.focusRequester(selectedFocusRequester) else Modifier),
                    onClick = { onPageSelected(page) }
                )
            }
        }
    }
}
