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
import com.example.ghostcart.ui.tutorial.FloatingTutorialMascot
import java.util.concurrent.TimeUnit

data class CoolingOption(val label: String, val durationMillis: Long)

/** The single default behind every primary "Ghost it" action. */
const val DEFAULT_GHOST_COOLDOWN_MILLIS: Long = 24L * 60L * 60L * 1000L

val coolingOptions = listOf(
    CoolingOption("15 min", TimeUnit.MINUTES.toMillis(15)),
    CoolingOption("24 hours", TimeUnit.HOURS.toMillis(24)),
    CoolingOption("3 days", TimeUnit.DAYS.toMillis(3)),
    CoolingOption("7 days", TimeUnit.DAYS.toMillis(7))
)

/**
 * Secondary duration picker used when someone deliberately changes or restarts a cooldown.
 * Primary Ghost actions always use the predictable 24-hour default above.
 */
@Composable
fun CoolingDurationDialog(
    onConfirm: (CoolingOption) -> Unit,
    onDismiss: () -> Unit,
    initialSelection: CoolingOption? = coolingOptions[1],
    options: List<CoolingOption> = coolingOptions,
    title: String = "Restart the cooldown",
    description: String = "Choose when Ghost Cart should remind you to decide again.",
    confirmLabel: String = "Restart",
    showTutorialGuide: Boolean = false
) {
    var selected by remember { mutableStateOf(initialSelection) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (showTutorialGuide) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        FloatingTutorialMascot(Modifier.padding(end = 10.dp))
                        Text(
                            "Cooling gives the impulse time to fade. Choose the 10-second practice option.",
                            color = Ink,
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    description,
                    color = MutedText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options) { option ->
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
            TextButton(
                enabled = selected != null,
                onClick = { selected?.let(onConfirm) }
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
