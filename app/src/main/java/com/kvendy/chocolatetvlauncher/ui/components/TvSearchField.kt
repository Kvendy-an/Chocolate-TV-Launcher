package com.kvendy.chocolatetvlauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

/**
 * A search box for D-pad navigation: displays as plain, non-editable text until explicitly
 * pressed, only then becoming focusable and showing the keyboard.
 *
 * The [BasicTextField] stays mounted the whole time (just invisible and excluded from focus via
 * [Modifier.focusProperties] until activated) rather than being added/removed from composition on
 * press — an earlier version did the latter, and requesting focus onto a text field in the very
 * frame it's newly composed turned out to be unreliable. A field that's already been laid out from
 * the start has nothing to race.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search apps...",
    onSearch: (() -> Unit)? = null
) {
    var isEditing by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitSearch() {
        keyboardController?.hide()
        onSearch?.invoke()
    }

    val borderColor by animateColorAsState(if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    val backgroundColor by animateColorAsState(if (isFocused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(50))
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = if (onSearch != null) KeyboardOptions(imeAction = ImeAction.Search)
            else KeyboardOptions.Default,
            keyboardActions = KeyboardActions(onSearch = if (onSearch != null) { { submitSearch() } } else null),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.Medium)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        innerTextField()
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                // Invisible and out of the focus chain until activated — the overlay below is
                // what D-pad navigation actually lands on before that.
                .alpha(if (isEditing) 1f else 0f)
                .focusProperties { canFocus = isEditing }
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (onSearch != null && event.key == Key.DirectionDown) {
                        if (event.type == KeyEventType.KeyDown) submitSearch()
                        true
                    } else {
                        false
                    }
                }
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (it.isFocused) {
                        keyboardController?.show()
                    } else {
                        // Losing focus (D-pad moved elsewhere, or Back dismissed the keyboard)
                        // drops back to plain-text display mode.
                        isEditing = false
                    }
                }
        )

        if (!isEditing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.Medium)
                    .onFocusChanged { isFocused = it.isFocused }
                    .tvClickable(onClick = { isEditing = true })
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(Spacing.Small))
                Text(
                    text = query.ifEmpty { placeholder },
                    color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
