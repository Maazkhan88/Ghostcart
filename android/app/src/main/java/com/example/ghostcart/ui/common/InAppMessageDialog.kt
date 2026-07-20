package com.example.ghostcart.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.ghostcart.data.InAppMessage
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText

/** Admin-composed ad-hoc message shown on launch. Dismissal is remembered locally
 * per-install (see AppViewModel.dismissInAppMessage) so it isn't shown again once seen. */
@Composable
fun InAppMessageDialog(message: InAppMessage, onDismiss: () -> Unit, onOpenLink: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(message.title, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!message.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp))
                    )
                }
                Text(message.body, color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
            }
        },
        confirmButton = {
            if (!message.linkUrl.isNullOrBlank()) {
                TextButton(onClick = { onOpenLink(message.linkUrl); onDismiss() }) {
                    Text("View", color = GhostGreen, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("OK", color = GhostGreen, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            if (!message.linkUrl.isNullOrBlank()) {
                TextButton(onClick = onDismiss) { Text("Dismiss", color = Ink) }
            }
        }
    )
}
