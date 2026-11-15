package com.kvendy.chocolatetvlauncher.ui.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.HomeSection
import com.kvendy.chocolatetvlauncher.data.PreferencesData
import com.kvendy.chocolatetvlauncher.ui.components.SmallScaleButton
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import java.util.Collections
import kotlinx.coroutines.launch

@Composable
internal fun SectionOrderPreference(preferencesData: PreferencesData) {
    val coroutineScope = rememberCoroutineScope()
    val sectionOrder by preferencesData.sectionOrder.collectAsState(initial = HomeSection.DEFAULT_ORDER)

    fun moveSection(from: Int, to: Int) {
        val newOrder = sectionOrder.toMutableList()
        Collections.swap(newOrder, from, to)
        coroutineScope.launch { preferencesData.setSectionOrder(newOrder) }
    }

    sectionOrder.forEachIndexed { index, section ->
        key(section) {
            val label = when (section) {
                HomeSection.RECENT_APPS -> stringResource(R.string.preferences_recent_apps)
                HomeSection.FEATURED_CONTENT -> stringResource(R.string.preferences_recommendations)
                HomeSection.NOTIFICATIONS -> stringResource(R.string.preferences_notifications)
                HomeSection.APP_GRID -> stringResource(R.string.section_apps)
                else -> section
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    SmallScaleButton(enabled = index > 0, onClick = { moveSection(index, index - 1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.preferences_move_up, label))
                    }
                    SmallScaleButton(enabled = index < sectionOrder.lastIndex, onClick = { moveSection(index, index + 1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.preferences_move_down, label))
                    }
                }
            }
        }
    }
}
