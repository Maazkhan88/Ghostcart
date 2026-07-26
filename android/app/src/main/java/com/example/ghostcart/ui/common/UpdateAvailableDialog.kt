package com.example.ghostcart.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.AppUpdateInfo
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText

/**
 * Not on the Play Store yet, so there's no system in-app-update prompt to
 * rely on - this is the whole mechanism. A mandatory update (this device's
 * versionCode below the server's configured minimum) has no dismiss path;
 * an optional one can be pushed to next launch.
 */
@Composable
fun UpdateAvailableDialog(
    update: AppUpdateInfo,
    downloading: Boolean,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!update.mandatory) onDismiss() },
        title = {
            Text(
                if (update.mandatory) "Update required" else "Update available",
                color = Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Ghost Cart ${update.latestVersionName} is ready to install" +
                        if (update.mandatory) ". This version is required to keep using the app." else ".",
                    color = MutedText,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                if (update.releaseNotes.isNotBlank()) {
                    Text(update.releaseNotes, color = MutedText, fontSize = 12.sp, lineHeight = 17.sp)
                }
                if (downloading) {
                    CircularProgressIndicator(color = GhostGreen, modifier = Modifier)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate, enabled = !downloading) {
                Text(if (downloading) "Downloading…" else "Update now", color = GhostGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!update.mandatory) {
                TextButton(onClick = onDismiss, enabled = !downloading) { Text("Later", color = Ink) }
            }
        }
    )
}
