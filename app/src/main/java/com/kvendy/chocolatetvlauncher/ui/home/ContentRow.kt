package com.kvendy.chocolatetvlauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.RecommendedContent
import com.kvendy.chocolatetvlauncher.ui.components.fullBleedHorizontal
import com.kvendy.chocolatetvlauncher.ui.home.tiles.ContentCard
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

private val BaseCardWidth = 320.dp

/** A titled, horizontally-scrolling row of [ContentCard]s — used for both "Continue Watching" and "Top Picks for You". */
@Composable
fun ContentRow(
    title: String,
    items: List<RecommendedContent>,
    appsByPackage: Map<String, AppInfo>,
    iconCornerRadius: Float,
    scale: Float = 1f,
    titleScale: Float = 1f,
    showTitle: Boolean = true,
    showIcon: Boolean = true,
    labelScale: Float = 1f,
    onItemFocused: (Color) -> Unit = {},
    showGlow: Boolean = true,
    glowRadius: Float = 40f,
    onItemClicked: (RecommendedContent) -> Unit
) {
    val cardWidth = BaseCardWidth * scale


    Column {
        SectionTitle(title, Icons.Default.Movie, titleScale, showTitle, showIcon)

        // Top padding is the title-to-card gap; bottom is just enough clearance for the
        // focused-card scale-up, not a section separator — HomeScreen's header Column owns
        // the gap between this row and its neighbors, so a large bottom value here would
        // double up that spacing.
        //
        // fullBleedHorizontal breaks this row out of HomeScreen's outer screen margin so it
        // clips exactly at the display's true edges instead of ~32dp short of them — that
        // premature inset clip boundary (with visible empty margin beyond it) was what made
        // scrolled-off cards look cut off, not anything about the cards themselves. contentPadding
        // below is bumped to match that same margin so cards still sit at the usual inset when
        // the row isn't scrolled to an edge.
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
            contentPadding = PaddingValues(
                start = Spacing.ExtraLarge,
                end = Spacing.ExtraLarge,
                top = if (showTitle || showIcon) Spacing.Medium else Spacing.Small,
                bottom = Spacing.ExtraSmall
            ),
            modifier = Modifier.fullBleedHorizontal(Spacing.ExtraLarge)
        ) {
            items(items, key = { it.id }) { item ->
                ContentCard(
                    content = item,
                    sourceApp = appsByPackage[item.sourcePackageName],
                    cornerRadius = iconCornerRadius,
                    onClick = onItemClicked,
                    modifier = Modifier.width(cardWidth),
                    onFocused = onItemFocused,
                    showGlow = showGlow,
                    glowRadius = glowRadius,
                    labelScale = labelScale
                )
            }
        }
    }
}
