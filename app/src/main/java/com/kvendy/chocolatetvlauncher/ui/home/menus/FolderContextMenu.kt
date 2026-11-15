package com.kvendy.chocolatetvlauncher.ui.home.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.data.HomeGridItem
import com.kvendy.chocolatetvlauncher.ui.components.themedButtonColors
import com.kvendy.chocolatetvlauncher.ui.components.themedButtonScale
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderContextMenu(
    folder: HomeGridItem.FolderItem,
    onDismiss: () -> Unit,
    onReorder: () -> Unit,
    onRename: (String) -> Unit,
    onRemoveFolder: () -> Unit
) {
    var isRenaming by remember(folder.id) { mutableStateOf(false) }
    var nameInput by remember(folder.id) { mutableStateOf(folder.name ?: "") }
    val focusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                if (isRenaming) {
                    Text(
                        text = "Rename Folder",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = Spacing.Small)
                    )

                    BasicTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .padding(horizontal = Spacing.Medium),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (nameInput.isEmpty()) {
                                    Text("Leave blank for unnamed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    Button(
                        onClick = {
                            onRename(nameInput)
                            onDismiss()
                        },
                        colors = themedButtonColors(),
                        scale = themedButtonScale(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save") }

                    Button(
                        onClick = { isRenaming = false },
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        scale = themedButtonScale(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cancel") }
                } else {
                    Text(
                        text = folder.name?.takeIf { it.isNotBlank() } ?: "Folder",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = Spacing.Small)
                    )

                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .alpha(0f)
                            .focusRequester(focusRequester)
                            .focusable()
                    )

                    Button(
                        onClick = onReorder,
                        colors = themedButtonColors(),
                        scale = themedButtonScale(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Edit Mode") }

                    Button(
                        onClick = { nameInput = folder.name ?: ""; isRenaming = true },
                        colors = themedButtonColors(),
                        scale = themedButtonScale(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Rename") }

                    Button(
                        onClick = onRemoveFolder,
                        colors = themedButtonColors(),
                        scale = themedButtonScale(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Remove Folder") }
                }
            }
        }
    }

    LaunchedEffect(isRenaming) {
        focusRequester.requestFocus()
    }
}
