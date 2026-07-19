package com.example.ghostcart.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import java.util.concurrent.TimeUnit

data class CoolingOption(val label: String, val durationMillis: Long)

val coolingOptions = listOf(
    CoolingOption("15 min", TimeUnit.MINUTES.toMillis(15)),
    CoolingOption("24 hours", TimeUnit.HOURS.toMillis(24)),
    CoolingOption("3 days", TimeUnit.DAYS.toMillis(3)),
    CoolingOption("7 days", TimeUnit.DAYS.toMillis(7))
)

/**
 * Shared duration picker for every "Cool it" / "Start cooling" entry point (product cards,
 * product detail, cart list) - the cooling period must always be a user choice, never a silent
 * fixed default.
 */
@Composable
fun CoolingDurationDialog(
    onConfirm: (CoolingOption) -> Unit,
    onDismiss: () -> Unit,
    initialSelection: CoolingOption = coolingOptions[1]
) {
    var selected by remember { mutableStateOf(initialSelection) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How long do you want to cool this?") },
        text = {
            Column {
                Text(
                    "Pick a cooldown - you'll get a reminder when it's ready for a decision.",
                    color = MutedText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(coolingOptions) { option ->
                        FilterChip(
                            selected = option == selected,
                            onClick = { selected = option },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenTint, selectedLabelColor = Ink)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("Start cooling") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
