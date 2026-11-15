package com.kvendy.chocolatetvlauncher.ui.home.menus

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Input
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.ui.components.themedButtonScale
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvInputMenu(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val tvInputManager = remember { context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager }

    val inputs = remember {
        tvInputManager?.tvInputList?.filter { input ->
            input.isPassthroughInput
        } ?: emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(350.dp)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Text(
                    text = "Select Input",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = Spacing.Small)
                )

                if (inputs.isEmpty()) {
                    Text("No external inputs found.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        items(inputs) { input ->
                            // Get the display label (e.g., "HDMI 1")
                            val label = input.loadLabel(context).toString()

                            Button(
                                onClick = {
                                    try {
                                        // Standard way to switch input on Android TV
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.data = TvContract.buildChannelUriForPassthroughInput(input.id)
                                        context.startActivity(intent)
                                        onDismiss()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                scale = themedButtonScale(),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Input,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(Spacing.Small))
                                Text(label)
                            }
                        }
                    }
                }
            }
        }
    }
}