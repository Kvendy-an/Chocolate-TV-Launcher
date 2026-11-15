package com.kvendy.chocolatetvlauncher.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.*
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.ui.components.*
import com.kvendy.chocolatetvlauncher.ui.home.tiles.AppTile
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

@Composable
fun SearchScreen(
    allApps: List<AppInfo>,
    iconCornerRadius: Float,
    onAppClicked: (AppInfo) -> Unit,
    onAppLongClicked: (AppInfo) -> Unit,
    onBackPressed: () -> Unit,
    showGlow: Boolean,
    glowRadius: Float,
    glowPosition: String = "topright",
    labelScale: Float = 1f,
    gridSize: Int = 6,
    renderAsSquare: Boolean = false,
    showAppLabels: Boolean = true,
    hiddenApps: Set<String> = emptySet()
) {
    var query by remember { mutableStateOf("") }
    val visibleApps = remember(allApps, hiddenApps) {
        if (hiddenApps.isEmpty()) allApps else allApps.filterNot { it.packageName in hiddenApps }
    }
    val filteredApps = remember(query, visibleApps) {
        val term = query.trim()
        if (term.isEmpty()) visibleApps else visibleApps.filter { it.label.contains(term, ignoreCase = true) }
    }
    val searchBarFocusRequester = remember { FocusRequester() }
    var isSearchBarFocused by remember { mutableStateOf(false) }
    var requestedResultIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var glowColor by remember { mutableStateOf(Color.Transparent) }
    val animatedGlowColor by animateColorAsState(glowColor, animationSpec = AmbientGlowColorSpec, label = "Search Glow")

    Dialog(onDismissRequest = onBackPressed, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            searchBarFocusRequester.requestFocus()
        }
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                .then(if (showGlow) Modifier.ambientGlow(color = { animatedGlowColor }, position = GlowPosition.fromId(glowPosition)) else Modifier)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(top = Spacing.ExtraLarge)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.ExtraLarge)
                ) {
                    IconButton(onClick = onBackPressed, colors = themedIconButtonColors()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                    val borderColor by animateColorAsState(if (isSearchBarFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    val backgroundColor by animateColorAsState(if (isSearchBarFocused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                                    .clip(RoundedCornerShape(50)).background(backgroundColor)
                                    .border(2.dp, borderColor, RoundedCornerShape(50))
                                    .padding(horizontal = Spacing.Medium)
                            ) {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Box(Modifier.weight(1f)) {
                                    if (query.isEmpty()) Text(stringResource(R.string.search_apps), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    innerTextField()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).focusRequester(searchBarFocusRequester)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && filteredApps.isNotEmpty()) {
                                    keyboardController?.hide()
                                    requestedResultIndex = 0
                                    true
                                } else false
                            }.onFocusChanged {
                            isSearchBarFocused = it.isFocused
                            if (it.isFocused) {
                                requestedResultIndex = null
                                keyboardController?.show()
                            }
                        }
                    )
                }
                Text(
                    pluralStringResource(R.plurals.search_result_count, filteredApps.size, filteredApps.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Medium)
                )
                BoxWithConstraints(Modifier.weight(1f)) {
                    val columns = gridSize.coerceAtLeast(1)
                    val tileWidth = (maxWidth - Spacing.ExtraLarge * 2 - Spacing.Large * (columns - 1)) / columns
                    val tileHeight = tileWidth / (if (renderAsSquare) 1f else 16f / 9f)
                    // LazyColumn clips at its viewport. Keep both the 1.1x focus expansion
                    // and the blur kernel inside that viewport, including after row scrolling.
                    // Glow radius is stored in physical pixels; layout padding is in dp.
                    val blurClearance = if (showGlow) with(density) { (glowRadius * 2f).toDp() } else 0.dp
                    val verticalClearance = maxOf(Spacing.Medium, blurClearance + tileHeight * 0.05f + Spacing.Small)
                    if (filteredApps.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(Spacing.ExtraLarge),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.search_no_results), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.search_try_again), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.Small))
                        }
                    } else {
                        TvAppGrid(
                            items = filteredApps,
                            gridSize = columns,
                            focusIndexRequest = requestedResultIndex,
                            rowSpacing = if (showAppLabels) Spacing.Small else Spacing.Large,
                            itemKey = { it.packageName },
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = Spacing.ExtraLarge, end = Spacing.ExtraLarge, top = verticalClearance, bottom = verticalClearance)
                        ) { app, tileModifier ->
                            AppTile(
                                modifier = tileModifier,
                                app = app,
                                aspectRatio = if (renderAsSquare) 1f else 16f / 9f,
                                renderAsSquare = renderAsSquare,
                                iconCornerRadius = iconCornerRadius,
                                showAppLabels = showAppLabels,
                                onAppClicked = onAppClicked,
                                onAppLongClicked = onAppLongClicked,
                                isMoving = false,
                                onFocused = { glowColor = it },
                                showGlow = showGlow,
                                glowRadius = glowRadius,
                                labelScale = labelScale
                            )
                        }
                    }
                }
            }
        }
    }
}
