package com.kvendy.chocolatetvlauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing

/**
 * Shown while the initial app scan (PackageManager queries + banner/icon resolution across every
 * installed app — the one genuinely slow step at startup) is still running, instead of the grid
 * briefly rendering empty and then suddenly populating once it finishes. Reuses HomeBackground so
 * whatever wallpaper/gradient the user has already looks correct as soon as the real Home appears.
 */
@Composable
fun LoadingScreen(preferencesData: PreferencesData) {
    Box(modifier = Modifier.fillMaxSize()) {
        HomeBackground(preferencesData = preferencesData)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                // R.mipmap.ic_launcher resolves to an <adaptive-icon> XML on API 26+, which
                // painterResource() can't load directly (vector drawables and plain rasterized
                // assets only) — ic_banner is a plain PNG, safe to load this way.
                painter = painterResource(R.mipmap.ic_banner),
                contentDescription = null,
                modifier = Modifier.width(240.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(Spacing.Large))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
