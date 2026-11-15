package com.kvendy.chocolatetvlauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [LazyColumn]-of-[Row]s grid tuned for fast, smooth D-pad navigation on weak Android TV
 * hardware. Reused by the home screen and search so this navigation logic only lives in one
 * place. Generic over the item type so the home grid can mix apps and folders while search stays
 * flat apps only.
 *
 * Why not [androidx.compose.foundation.lazy.grid.LazyVerticalGrid]: its 2D focus-search/layout
 * topology measurably stutters D-pad vertical navigation on low-end devices compared to a plain
 * [LazyColumn] of [Row]s, which is why this exists instead. On top of that:
 * - Each tile gets an explicit up/down [FocusRequester] wired to the tile directly above/below it
 *   (O(1)), bypassing Compose's generic 2D focus-search candidate scan.
 * - [LazyLayoutCacheWindow] keeps rows composed ahead of AND behind the viewport (symmetric —
 *   "ahead"/"behind" track current scroll direction, not list position, so an asymmetric window
 *   starves whichever direction isn't currently being scrolled) so those focus targets always
 *   already exist.
 * - Scroll-into-view is driven manually through a queued [MutableStateFlow] instead of Compose's
 *   default focus-triggered bring-into-view: [androidx.compose.foundation.lazy.LazyListState]
 *   .animateScrollToItem builds a fresh zero-velocity animation on every call, so cancelling one
 *   mid-flight (the default behavior under rapid repeated presses) restarts from a dead stop and
 *   visibly snaps. Queuing lets each animation finish before the next (possibly newer) target is
 *   picked up.
 * - Captions reserve their measured height, so focus labels never overlap adjacent rows.
 *
 * @param items The full flat list of items to lay out, in order.
 * @param gridSize Number of columns per row.
 * @param itemKey Stable identity per item, used for the row's LazyColumn key and per-tile
 *   `key {}` scoping. Renamed from the more obvious `key` to avoid shadowing
 *   [androidx.compose.runtime.key].
 * @param header Optional content placed above the grid, scrolling together with it. Must emit
 *   exactly [headerItemCount] items.
 * @param headerItemCount How many items [header] emits, used to translate a grid row index into
 *   the LazyColumn's absolute item index for scrolling.
 * @param footer Optional content placed below the grid, scrolling together with it. Doesn't
 *   affect scroll-to-row math since it comes after the app rows.
 * @param focusIndexRequest When non-null, (re-)focuses whichever tile currently sits at this flat
 *   index, every time [items] reorders — overriding Compose's own `key()`-based identity tracking
 *   below, which otherwise carries focus along with the moved item itself (e.g. after an edit-mode
 *   swap, the previously-focused item ends up at the *other* slot, so focus would silently follow
 *   it there instead of staying on the slot the user was actually looking at). Callers that swap
 *   two items in place should set this to the target slot's own flat index — which a plain 2-item
 *   swap never changes — and leave it set until the next edit so this keeps re-asserting focus
 *   there across every recomposition the swap's own (async) persistence triggers.
 * @param confineFocus When true, blocks focus from leaving the grid vertically — up from the
 *   first row and down from the last row both cancel instead of falling through to Compose's
 *   default search, which would otherwise land on whatever [header]/[footer] content (or a
 *   neighboring section) happens to be nearest. Used while editing so a stray press up/down can't
 *   strand the user's held tile outside the grid it can actually act on.
 * @param tile Renders one item. Apply the given [Modifier] to the focusable root of your tile —
 *   it carries the focus-routing and scroll-tracking wiring described above.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : Any> TvAppGrid(
    items: List<T>,
    gridSize: Int,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.ExtraLarge),
    rowSpacing: Dp = Spacing.Small,
    headerItemCount: Int = 0,
    header: LazyListScope.() -> Unit = {},
    footer: LazyListScope.() -> Unit = {},
    focusIndexRequest: Int? = null,
    confineFocus: Boolean = false,
    tile: @Composable (item: T, tileModifier: Modifier) -> Unit
) {
    val itemRows = remember(items, gridSize) { items.chunked(gridSize) }
    val rowFocusRequesters = remember(itemRows) {
        itemRows.map { row -> row.map { FocusRequester() } }
    }
    // Re-runs on every itemRows change (not just when focusIndexRequest itself changes) since the
    // reorder this is meant to counteract usually lands a frame or more after the caller sets the
    // request (it round-trips through DataStore) — rowFocusRequesters is a fresh set of objects
    // each time, so only a request that keeps re-asserting itself across that whole window
    // reliably wins against the identity-follow behavior described above.
    LaunchedEffect(focusIndexRequest, itemRows) {
        val index = focusIndexRequest ?: return@LaunchedEffect
        val row = index / gridSize
        val col = index % gridSize
        try {
            rowFocusRequesters.getOrNull(row)?.getOrNull(col)?.requestFocus()
        } catch (e: IllegalStateException) {
            // Target not currently composed (e.g. scrolled out of the cache window) — nothing to
            // do; if it comes back into range, itemRows changing again will retry.
        }
    }
    val gridScrollState = rememberLazyListState(
        cacheWindow = LazyLayoutCacheWindow(aheadFraction = 0.5f, behindFraction = 0.5f)
    )
    var lastScrolledRowIndex by remember { mutableStateOf(-1) }
    var focusedRowIndex by remember { mutableStateOf(-1) }
    val desiredRowIndex = remember { MutableStateFlow(-1) }
    LaunchedEffect(gridScrollState, headerItemCount) {
        desiredRowIndex.collect { rowIndex ->
            if (rowIndex >= 0) {
                gridScrollState.animateScrollToItem(headerItemCount + rowIndex)
            }
        }
    }

    LazyColumn(
        state = gridScrollState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(rowSpacing)
    ) {
        header()
        itemsIndexed(itemRows, key = { _, row -> itemKey(row.first()) }) { rowIndex, rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (rowIndex == focusedRowIndex) 1f else 0f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Large)
            ) {
                rowItems.forEachIndexed { colIndex, item ->
                    key(itemKey(item)) {
                        val upRequester = rowFocusRequesters.getOrNull(rowIndex - 1)?.getOrNull(colIndex)
                        val downRequester = rowFocusRequesters.getOrNull(rowIndex + 1)?.getOrNull(colIndex)
                        Box(modifier = Modifier.weight(1f)) {
                            tile(
                                item,
                                Modifier
                                    .fillMaxWidth()
                                    .focusRequester(rowFocusRequesters[rowIndex][colIndex])
                                    .focusProperties {
                                        // Rows in the middle always have a real neighbor to wire
                                        // up; only the true first/last row ever needs a boundary
                                        // decision — fall through to Compose's own default search
                                        // normally, or explicitly cancel it while confineFocus
                                        // keeps the grid from handing focus off to whatever's
                                        // nearest outside it (a header/footer section).
                                        up = upRequester ?: if (confineFocus) FocusRequester.Cancel else FocusRequester.Default
                                        down = downRequester ?: if (confineFocus) FocusRequester.Cancel else FocusRequester.Default
                                    }
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            focusedRowIndex = rowIndex
                                            if (lastScrolledRowIndex != rowIndex) {
                                                lastScrolledRowIndex = rowIndex
                                                desiredRowIndex.value = rowIndex
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
                val emptySlots = gridSize - rowItems.size
                if (emptySlots > 0) {
                    repeat(emptySlots) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        footer()
    }
}
