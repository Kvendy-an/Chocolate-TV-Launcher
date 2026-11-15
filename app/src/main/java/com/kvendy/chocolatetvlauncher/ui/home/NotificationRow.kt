package com.kvendy.chocolatetvlauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import com.kvendy.chocolatetvlauncher.AppImageModel
import com.kvendy.chocolatetvlauncher.data.AppInfo
import com.kvendy.chocolatetvlauncher.data.AppNotification
import com.kvendy.chocolatetvlauncher.ui.components.fullBleedHorizontal
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

private val CardWidth = 280.dp

/**
 * A titled, horizontally-scrolling row of notification cards for the Home page — mirrors
 * [ContentRow]'s structure. Tap opens the notification (its own contentIntent, falling back to
 * launching the source app); long-press dismisses it, the same "hold for secondary actions"
 * convention used everywhere else in this app (e.g. an app tile's context menu).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NotificationRow(
    title: String,
    notifications: List<AppNotification>,
    appsByPackage: Map<String, AppInfo>,
    titleScale: Float = 1f,
    showTitle: Boolean = true,
    showIcon: Boolean = true,
    onNotificationClicked: (AppNotification) -> Unit,
    onNotificationDismissed: (AppNotification) -> Unit
) {

    // Keyed by notification key rather than recreated with the list, so an entry stays valid
    // (and attached to its still-composed card) right up until the moment it's dismissed — needed
    // below to redirect focus to a neighbor before removal happens.
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun focusRequesterFor(key: String) = focusRequesters.getOrPut(key) { FocusRequester() }

    // Dismissing the only remaining notification has no neighbor to hand focus to — the "No
    // Notifications" placeholder doesn't exist yet at that instant (it only appears once the list
    // recomposes as empty), so its FocusRequester isn't attached to anything and can't take focus
    // synchronously. This flag defers that handoff to the LaunchedEffect below, which runs after
    // that recomposition has actually happened.
    val emptyStateFocusRequester = remember { FocusRequester() }
    var focusEmptyStateNext by remember { mutableStateOf(false) }
    LaunchedEffect(notifications.isEmpty()) {
        if (notifications.isEmpty() && focusEmptyStateNext) {
            emptyStateFocusRequester.requestFocus()
            focusEmptyStateNext = false
        }
    }

    fun dismiss(notification: AppNotification) {
        // Without this, removing the focused card leaves Compose's default focus-search to pick
        // whatever it finds next — which was landing all the way up on the StatusBar's Home tab,
        // one accidental press away from switching pages. Moving focus to a neighboring card
        // first keeps it exactly where the user still is.
        val index = notifications.indexOf(notification)
        val neighborKey = when {
            index < 0 -> null
            index < notifications.lastIndex -> notifications[index + 1].key
            index > 0 -> notifications[index - 1].key
            else -> null
        }
        if (neighborKey != null) {
            focusRequesters[neighborKey]?.requestFocus()
        } else {
            focusEmptyStateNext = true
        }
        onNotificationDismissed(notification)
        focusRequesters.remove(notification.key)
    }

    Column {
        SectionTitle(title, Icons.Default.Notifications, titleScale, showTitle, showIcon)

        if (notifications.isEmpty()) {
            // Kept visible with a hint instead of the whole row disappearing — losing the section
            // outright when the last notification is dismissed was itself part of what made focus
            // jump somewhere unexpected, on top of just being a jarring layout shift. No extra
            // horizontal padding here: the row below reads as starting at the same position via
            // fullBleedHorizontal (break out to the true edge) plus its own matching contentPadding,
            // but this static element just inherits the outer margin directly — adding padding on
            // top of that was the double-inset that shifted it visibly right of the cards.
            //
            // A real (if no-op) clickable Surface, not a plain Box, so it can actually take and
            // show focus when the last notification's dismiss hands off to it below.
            Surface(
                onClick = {},
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                colors = ClickableSurfaceDefaults.colors(
                    // Semi-transparent at rest (not the solid surfaceVariant) so the wallpaper/
                    // gradient behind the row still shows through, same as the notification cards
                    // below — fully opaque once focused so the focused state still stands out.
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    // Matches the LazyRow branch below: top is the title-to-content gap, bottom
                    // is the same small clearance that branch's contentPadding adds (there it's
                    // for focused-card scale-up headroom; here it's just for equal spacing to the
                    // next section either way — HomeScreen's header Column still owns the rest).
                    .padding(top = if (showTitle || showIcon) Spacing.Medium else Spacing.Small, bottom = Spacing.ExtraSmall)
                    .focusRequester(emptyStateFocusRequester)
            ) {
                Text(
                    text = "No Notifications",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.Medium)
                )
            }
        } else {
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
                items(notifications, key = { it.key }) { notification ->
                    val sourceApp = appsByPackage[notification.packageName]
                    Surface(
                        onClick = { onNotificationClicked(notification) },
                        onLongClick = { dismiss(notification) },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                        colors = ClickableSurfaceDefaults.colors(
                            // Semi-transparent at rest so the wallpaper/gradient shows through
                            // instead of a solid card; opaque once focused so it still stands out.
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.width(CardWidth).focusRequester(focusRequesterFor(notification.key))
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.Medium),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape),
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
                            Column {
                                Text(
                                    text = notification.title ?: sourceApp?.label ?: notification.packageName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                notification.text?.let { text ->
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
