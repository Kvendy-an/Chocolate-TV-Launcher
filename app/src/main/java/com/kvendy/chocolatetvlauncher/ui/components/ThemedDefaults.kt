package com.kvendy.chocolatetvlauncher.ui.components

import androidx.compose.runtime.Composable
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.ListItemColors
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme

/**
 * tv-material3's own focused-state defaults for [androidx.tv.material3.ListItem],
 * [androidx.tv.material3.Button], [androidx.tv.material3.IconButton] and a plain clickable
 * [androidx.tv.material3.Surface] all key off `colorScheme.inverseSurface`/`onSurface`, not
 * `colorScheme.primary` — so the accent color picker in Settings had no visible effect on most of
 * the app's focus highlighting (list rows, dialog buttons, status bar shortcuts) even though it
 * correctly changed [MaterialTheme.colorScheme.primary] itself. These give every one of those
 * components' focused state the current accent color instead, so it reads consistently everywhere.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun themedListItemColors(): ListItemColors = ListItemDefaults.colors(
    // The softer primaryContainer tone, not solid primary — PreferenceRow nests a Switch whose
    // own "checked" track color is also primary, so a focused row background using that same
    // solid primary made the switch blend invisibly into it whenever a checked toggle was focused.
    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    focusedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun themedButtonColors(): ButtonColors = ButtonDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.primary,
    focusedContentColor = MaterialTheme.colorScheme.onPrimary
)

/**
 * tv-material3's default focusedScale for [androidx.tv.material3.Button] is 1.1x — fine for a
 * compact, isolated button, but every Button in this app's dialogs (context menus, the input
 * picker) is full-width in a narrow ~300dp dialog, stacked with only [Spacing.Small] between them.
 * At 1.1x, a focused button's scaled-up bounds overflowed the dialog's own side padding and
 * overlapped its neighbors above/below. This keeps the focus pop visible without either.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun themedButtonScale(): ButtonScale = ButtonDefaults.scale(focusedScale = 1.02f)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun themedIconButtonColors(): ButtonColors = IconButtonDefaults.colors(
    // Unlike the others, this also tints the resting (unfocused) icon — status bar shortcuts are
    // small glyphs with no other visual tie to the theme, so leaving them plain onSurface at rest
    // made the accent color invisible until something happened to be focused.
    contentColor = MaterialTheme.colorScheme.primary,
    focusedContainerColor = MaterialTheme.colorScheme.primary,
    focusedContentColor = MaterialTheme.colorScheme.onPrimary
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun themedSurfaceColors(): ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.primary,
    focusedContentColor = MaterialTheme.colorScheme.onPrimary
)
