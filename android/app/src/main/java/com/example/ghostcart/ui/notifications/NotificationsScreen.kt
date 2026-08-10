package com.example.ghostcart.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.NotificationItem
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.common.GhostTopBar

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    loading: Boolean,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { onOpen() }

    Column(modifier = modifier.fillMaxSize().background(Paper)) {
        GhostTopBar(title = "Notifications", onBack = onBack)
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GhostGreen)
            }
            notifications.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = MutedText, modifier = Modifier.size(40.dp))
                    Text(
                        "Nothing yet",
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "Order updates, gifts and announcements will show up here.",
                        color = MutedText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(0.dp, 16.dp, 0.dp, 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationRow(notification)
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SoftGray)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FaintBorder),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (notification.type) {
                "delivery" -> Icons.Filled.LocalShipping
                "gift" -> Icons.Filled.CardGiftcard
                else -> Icons.Filled.Campaign
            }
            Icon(icon, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(notification.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(notification.body, color = MutedText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
