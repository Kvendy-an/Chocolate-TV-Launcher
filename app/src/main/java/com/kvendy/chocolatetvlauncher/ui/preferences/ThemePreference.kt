package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.SectionDivider
import com.kvendy.chocolatetvlauncher.ui.components.SmallScaleButton
import com.kvendy.chocolatetvlauncher.ui.components.themedListItemColors
import com.kvendy.chocolatetvlauncher.ui.theme.AccentColor
import com.kvendy.chocolatetvlauncher.ui.theme.AutoAccentColor
import com.kvendy.chocolatetvlauncher.ui.theme.GradientBackground
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import com.kvendy.chocolatetvlauncher.ui.theme.id
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ThemePreference(preferencesData: PreferencesData) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wallpaperPath by preferencesData.wallpaperPath.collectAsState(initial = null)

    val theme by preferencesData.theme.collectAsState(initial = "system")
    val themes = listOf("system", "light", "dark")
    val selectedIndex = themes.indexOf(theme)
    val accentColorId by preferencesData.accentColor.collectAsState(initial = "default")
    val gradientBackgroundId by preferencesData.gradientBackground.collectAsState(initial = "none")

    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(selectedUri)
                    val file = File(context.filesDir, "custom_wallpaper.jpg")

                    inputStream?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    preferencesData.setWallpaperPath(file.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            SmallScaleButton(onClick = { coroutineScope.launch { preferencesData.setTheme(themes[ (selectedIndex - 1 + themes.size) % themes.size]) } }) {
                Text("<")
            }
            Text(
                text = theme.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                modifier = Modifier.width(100.dp),
                textAlign = TextAlign.Center
            )
            SmallScaleButton(onClick = { coroutineScope.launch { preferencesData.setTheme(themes[(selectedIndex + 1) % themes.size]) } }) {
                Text(">")
            }
        }
    }

    Text("Accent Color", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.Small))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        items(AccentColor.entries.toList()) { accent ->
            val isSelected = accent.id == accentColorId
            // Auto has no fixed seed of its own — reflects whatever LauncherTheme most recently
            // resolved from the current wallpaper/gradient (see AutoAccentColor), instead of the
            // generic placeholder grey every other unset case falls back to.
            val swatchColor = if (accent == AccentColor.Auto) {
                AutoAccentColor.seed ?: Color(0xFF9E9E9E)
            } else {
                accent.seed ?: Color(0xFF9E9E9E)
            }
            var isFocused by remember { mutableStateOf(false) }
            Surface(
                onClick = { coroutineScope.launch { preferencesData.setAccentColor(accent.id) } },
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                modifier = Modifier.size(44.dp).onFocusChanged { isFocused = it.isFocused }
            ) {
                Row(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(4.dp)
                        .background(swatchColor, CircleShape)
                        // Drawn on the visible circle itself, not the outer Surface — that circle
                        // sits inset within the Surface's own (larger, invisible) click target, so
                        // a border on the Surface didn't hug the swatch you actually see.
                        .border(
                            width = if (isFocused) 3.dp else 2.dp,
                            color = if (isFocused || isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = accent.label,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (accent == AccentColor.Auto) {
                        // Marks this swatch as the dynamic "derive from background" option even
                        // before it's picked — otherwise, whatever color it currently happens to
                        // resolve to could look identical to one of the fixed named colors next
                        // to it, with no way to tell them apart at a glance.
                        Text("A", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    SectionDivider()

    Text("Background", style = MaterialTheme.typography.headlineSmall)

    ListItem(
        selected = false,
        onClick = { wallpaperLauncher.launch("image/*") },
        scale = ListItemDefaults.scale(focusedScale = 1.01f),
        colors = themedListItemColors(),
        headlineContent = { Text("Set Wallpaper") },
        supportingContent = { Text("Choose an image from your gallery") },
        trailingContent = { Icon(Icons.Default.Image, "Image") }
    )

    if (wallpaperPath != null) {
        ListItem(
            selected = false,
            onClick = {
                val pathToDelete = wallpaperPath

                coroutineScope.launch(Dispatchers.IO) {
                    preferencesData.setWallpaperPath(null)

                    if (pathToDelete != null) {
                        try {
                            File(pathToDelete).delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            },
            scale = ListItemDefaults.scale(focusedScale = 1.01f),
        colors = themedListItemColors(),
            headlineContent = { Text("Remove Wallpaper") },
            trailingContent = { Icon(Icons.Default.Delete, "Delete") }
        )
    }

    Text(
        text = "Gradient",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = Spacing.Small)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        items(GradientBackground.entries.toList()) { gradient ->
            val isSelected = gradient.id == gradientBackgroundId
            var isFocused by remember { mutableStateOf(false) }
            Surface(
                onClick = {
                    val oldWallpaperPath = wallpaperPath
                    coroutineScope.launch(Dispatchers.IO) {
                        preferencesData.setGradientBackground(gradient.id)
                        if (gradient != GradientBackground.None && oldWallpaperPath != null) {
                            try { File(oldWallpaperPath).delete() } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                },
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                modifier = Modifier.size(width = 64.dp, height = 44.dp).onFocusChanged { isFocused = it.isFocused }
            ) {
                // Border drawn on the visible swatch itself (see the accent swatch's identical
                // comment) rather than the outer Surface, whose bounds don't match it exactly.
                val swatchModifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .border(
                        width = if (isFocused) 3.dp else 2.dp,
                        color = if (isFocused || isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                if (gradient == GradientBackground.None) {
                    Row(
                        modifier = swatchModifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("None", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Row(
                        modifier = swatchModifier.background(Brush.verticalGradient(gradient.colors), RoundedCornerShape(8.dp)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = gradient.label,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

}
