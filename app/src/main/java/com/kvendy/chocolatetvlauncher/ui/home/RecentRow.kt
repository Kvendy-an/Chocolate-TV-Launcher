package com.kvendy.chocolatetvlauncher.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.ui.components.fullBleedHorizontal
import com.kvendy.chocolatetvlauncher.ui.home.tiles.AppTile
import com.kvendy.chocolatetvlauncher.ui.home.tiles.SpecialTile
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun RecentRow(
    apps: List<AppInfo>,
    iconCornerRadius: Float,
    showAppLabels: Boolean,
    onAppClicked: (AppInfo) -> Unit,
    onAppFocused: (Color) -> Unit,
    onClearClicked: () -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    isVisible: Boolean = true,
    scale: Float = 1.0f,
    titleScale: Float = 1f,
    showTitle: Boolean = true,
    showIcon: Boolean = true,
    labelScale: Float = 1f,
    useSquareIcons: Boolean = false,
    showGlow: Boolean,
    glowRadius: Float
) {
    if (!isVisible) return

    val baseWidth = if (useSquareIcons) 120.dp else 214.dp
    val tileWidth = baseWidth * scale
    val tileAspectRatio = if (useSquareIcons) 1f else 16f/9f
    val gapWidth = 24.dp
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor = MaterialTheme.colorScheme.error

    var isClearing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focusRequesters = remember(apps) { List(apps.size + 1) { FocusRequester() } }
    var pendingRemoval by remember { mutableStateOf<Pair<String, Int>?>(null) }

    LaunchedEffect(apps, pendingRemoval) {
        val (packageName, index) = pendingRemoval ?: return@LaunchedEffect
        if (apps.any { it.packageName == packageName }) return@LaunchedEffect
        val nextIndex = index.coerceAtMost(apps.lastIndex).coerceAtLeast(0)
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == nextIndex }) {
            listState.scrollToItem(nextIndex)
        }
        focusRequesters[nextIndex].requestFocus()
        pendingRemoval = null
    }

    LaunchedEffect(isClearing) {
        if (isClearing) {
            // If the row was scrolled (enough recent apps to need it), the persistent tile's
            // trailing edge could sit right at the LazyRow's own clip boundary — as every app
            // tile ahead of it shrinks toward 0 width, the row's content shrinks under a scroll
            // offset that doesn't automatically track that, and the persistent tile could end up
            // partially clipped for the handful of frames until it settles. Snapping the scroll to
            // it right as the shrink starts keeps it anchored in view for the whole animation
            // instead of relying on that settling.
            listState.animateScrollToItem(apps.size)
            delay(350)
            onClearClicked()
            delay(100)
            isClearing = false
        }
    }

    val fullItemWidth = tileWidth + gapWidth

    val animatedWidth by animateDpAsState(
        targetValue = if (isClearing) 0.dp else fullItemWidth,
        label = "ClearAllWidth"
    )

    Column {
        SectionTitle(stringResource(R.string.section_recent_apps), Icons.Default.History, titleScale, showTitle, showIcon)

        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(0.dp), // Manual spacing
                // Top padding is the title-to-tile gap; bottom is just enough clearance for the
                // focused-tile scale-up, not a section separator — HomeScreen's header Column
                // owns the gap between this row and its neighbors.
                //
                // fullBleedHorizontal breaks this row out of HomeScreen's outer screen margin so
                // it clips exactly at the display's true edges instead of ~32dp short of them —
                // see ContentRow's identical fix for why. contentPadding matches that same margin
                // so tiles still sit at the usual inset when the row isn't scrolled to an edge.
                contentPadding = PaddingValues(
                    start = Spacing.ExtraLarge,
                    end = Spacing.ExtraLarge,
                    top = if (showTitle || showIcon) Spacing.Medium else Spacing.Small,
                    bottom = Spacing.ExtraSmall
                ),
                modifier = Modifier.fullBleedHorizontal(Spacing.ExtraLarge)
            ) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    var isCreated by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { isCreated = true }

                    val itemAlpha by animateFloatAsState(if (isCreated && !isClearing) 1f else 0f, label = "Alpha")

                    Box(
                        modifier = Modifier
                            .animateItem()
                            .width(if (isClearing) animatedWidth else fullItemWidth),
                        contentAlignment = Alignment.TopStart
                    ) {
                        AppTile(
                            modifier = Modifier
                                // Cap at the shrinking box's own width once it drops below
                                // tileWidth during Clear All — otherwise this stays fixed at
                                // tileWidth while its container shrinks toward 0, overflowing the
                                // box (which doesn't clip by default) and getting drawn over by
                                // the next tile sliding into that space, which looked like the
                                // tile itself getting clipped on the right mid-animation.
                                .width(if (isClearing) minOf(tileWidth, animatedWidth) else tileWidth)
                                .graphicsLayer { alpha = itemAlpha }
                                .focusRequester(focusRequesters[index]),
                            app = app,
                            aspectRatio = tileAspectRatio,
                            renderAsSquare = useSquareIcons,
                            iconCornerRadius = iconCornerRadius,
                            showAppLabels = showAppLabels,
                            onAppClicked = { if (!isClearing) onAppClicked(app) },
                            onAppLongClicked = { removed ->
                                if (!isClearing && pendingRemoval == null) {
                                    pendingRemoval = removed.packageName to index
                                    onRemoveApp(removed)
                                }
                            },
                            isMoving = false,
                            onFocused = onAppFocused,
                            showGlow = showGlow,
                            glowRadius = glowRadius,
                            labelScale = labelScale
                        )
                    }
                }
                item(key = "persistent_action_tile") {
                    val isRecentEmpty = apps.isEmpty()

                    val tileText = if (isRecentEmpty) "No Recent" else "Clear All"
                    val tileIcon = if (isRecentEmpty) Icons.Default.Apps else Icons.Default.Delete
                    val tileOnClick = if (isRecentEmpty) {{}} else {{ if (!isClearing) isClearing = true }}
                    val tileFocusColor = if (isRecentEmpty) onSurfaceVariant else errorColor

                    SpecialTile(
                        showLabel = showAppLabels,
                        // Deliberately no animateItem() here: the app tiles ahead of this one
                        // already shrink smoothly every frame via their own shared animatedWidth,
                        // which continuously moves this tile's correct position left throughout
                        // the whole Clear All sequence. Layering animateItem()'s own separate
                        // reposition spring on top of that constantly-moving target let it lag
                        // behind where the row's actual (already-shrunk) content ended, rendering
                        // this tile's box partially past that boundary and getting clipped —
                        // happened even with just a couple of recent apps, no scrolling involved.
                        // Tracking the exact position every frame (which is already animated,
                        // just by the tiles ahead of it, not this modifier) avoids that entirely.
                        modifier = Modifier.focusRequester(focusRequesters[apps.size]),
                        text = tileText,
                        icon = tileIcon,
                        width = tileWidth,
                        aspectRatio = tileAspectRatio,
                        cornerRadius = iconCornerRadius,
                        onClick = tileOnClick,
                        isSquare = useSquareIcons,
                        onFocused = { onAppFocused(tileFocusColor) },
                        labelScale = labelScale
                    )
                }
            }
        }
    }
}