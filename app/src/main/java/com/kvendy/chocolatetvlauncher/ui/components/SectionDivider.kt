package com.kvendy.chocolatetvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

@Composable
fun SectionDivider() {
    Spacer(modifier = Modifier.height(16.dp))
    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
    Spacer(modifier = Modifier.height(16.dp))
}
