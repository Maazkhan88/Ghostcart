package com.example.ghostcart.ui.gifts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.ghostcart.data.GiftListItem
import com.example.ghostcart.data.GiftLists
import com.example.ghostcart.data.GhostGiftRepository
import com.example.ghostcart.theme.DangerRed
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.DirhamAmount
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.common.GhostTopBar
import java.util.Locale

@Composable
fun GiftsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("Received") }
    var lists by remember { mutableStateOf(GiftLists()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        GhostGiftRepository.list(context)
            .onSuccess { lists = it }
            .onFailure { error = it.message ?: "Unable to load gifts" }
        loading = false
    }

    val visible = if (selectedTab == "Received") lists.received else lists.sent
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Paper),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 16.dp, 20.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GhostTopBar(title = "Gifts", onBack = onBack)
            Text(
                "Private gifts you received and simulated gifts you sent.",
                color = MutedText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Received", "Sent").forEach { tab ->
                    val selected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) GhostGreen else SoftGray)
                            .border(1.dp, if (selected) GhostGreen else FaintBorder, RoundedCornerShape(999.dp))
                            .clickable { selectedTab = tab }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$tab (${if (tab == "Received") lists.received.size else lists.sent.size})",
                            color = Ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
        if (loading) {
            item {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GhostGreen)
                }
            }
        } else if (error != null) {
            item { Text(requireNotNull(error), color = DangerRed, fontSize = 12.sp) }
        } else if (visible.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SoftGray).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GhostMascotPose("cart", Modifier.size(100.dp))
                    Text(
                        if (selectedTab == "Received") "No revealed gifts yet" else "No sent gifts yet",
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        if (selectedTab == "Received") "Open a gift from its private email link to add it here."
                        else "Choose Send as a gift during Fake Checkout.",
                        color = MutedText,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else {
            items(visible, key = { it.id }) { item -> GiftCard(item, selectedTab == "Received") }
        }
        item {
            Text(
                "Simulation only. Gifts shown here are ideas, not purchases. No real payment or delivery occurs.",
                color = MutedText,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun GiftCard(item: GiftListItem, received: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SoftGray)
            .border(1.dp, FaintBorder, RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(92.dp).clip(RoundedCornerShape(16.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.itemTitle,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            } else {
                GhostMascotPose("cart", Modifier.size(64.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (received) "From ${item.senderName ?: "a Ghost Cart member"}" else item.status.replaceFirstChar { it.uppercase() },
                color = GhostGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(item.itemTitle, color = Ink, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Text(item.category, color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
            if (item.amountCents > 0) {
                DirhamAmount(
                    amount = String.format(Locale.US, "%.2f", item.amountCents / 100.0),
                    tint = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    glyphSize = 11.dp,
                    modifier = Modifier.padding(top = 7.dp)
                )
            }
        }
    }
}
