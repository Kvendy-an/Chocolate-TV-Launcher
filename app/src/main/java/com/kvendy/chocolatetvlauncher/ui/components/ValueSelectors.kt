package com.kvendy.chocolatetvlauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kvendy.chocolatetvlauncher.ui.theme.Spacing
import java.util.Locale

@Composable
fun ValueSelector(text: String, value: Int, onValueChange: (Int) -> Unit, range: IntRange) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            SmallScaleButton(onClick = { if (value > range.first) onValueChange(value - 1) }) { Text("-") }
            Text(value.toString(), modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            SmallScaleButton(onClick = { if (value < range.last) onValueChange(value + 1) }) { Text("+") }
        }
    }
}

/**
 * Like [ValueSelector], but steps through an explicit list of allowed values instead of a plain
 * ±1 range — for settings like the idle-dim timeout where the steps aren't uniform (30s, then
 * whole minutes).
 */
@Composable
fun SteppedValueSelector(text: String, value: Int, options: List<Int>, label: (Int) -> String, onValueChange: (Int) -> Unit) {
    val index = options.indexOf(value).coerceAtLeast(0)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            SmallScaleButton(onClick = { if (index > 0) onValueChange(options[index - 1]) }) { Text("-") }
            Text(label(value), modifier = Modifier.width(90.dp), textAlign = TextAlign.Center, maxLines = 1)
            SmallScaleButton(onClick = { if (index < options.lastIndex) onValueChange(options[index + 1]) }) { Text("+") }
        }
    }
}

@Composable
fun FloatValueSelector(text: String, value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, step: Float = 0.1f) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            SmallScaleButton(onClick = { onValueChange((value - step).coerceIn(range).run { String.format(Locale.US, "%.1f", this).toFloat() }) }) { Text("-") }
            Text(String.format(Locale.US, "%.1f", value), modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            SmallScaleButton(onClick = { onValueChange((value + step).coerceIn(range).run { String.format(Locale.US, "%.1f", this).toFloat() }) }) { Text("+") }
        }
    }
}
