package com.example.ghostcart.ui.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.ghostcart.data.LeaderboardEntry
import com.example.ghostcart.data.avatarPresetById
import com.example.ghostcart.ui.DirhamAmount
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.common.GhostTopBar
import com.example.ghostcart.ui.formatDirhamAmount
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper

private val BronzeColor = Color(0xFFCD7F32)

@Composable
private fun AvatarCircle(avatarUrl: String?, avatarPresetId: String?, username: String, size: Dp = 40.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Ink),
        contentAlignment = Alignment.Center
    ) {
        val preset = avatarPresetById(avatarPresetId)
        when {
            preset != null -> Image(
                painter = painterResource(preset.drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(size / 10)
            )
            avatarUrl != null -> AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
            else -> Text(username.take(1).uppercase(), color = Paper, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PodiumCard(
    entry: LeaderboardEntry,
    rank: Int,
    isYou: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (rank) {
        1 -> GhostGreen
        3 -> BronzeColor
        else -> FaintBorder
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Paper)
            .border(if (rank == 1) 2.dp else 1.dp, accent, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = if (rank == 1) 18.dp else 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "#$rank",
            color = accent,
            fontSize = if (rank == 1) 15.sp else 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (rank == 1) GreenTint else FaintBorder.copy(alpha = 0.4f))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        )
        AvatarCircle(
            avatarUrl = entry.avatarUrl,
            avatarPresetId = entry.avatarPresetId,
            username = entry.username,
            size = if (rank == 1) 64.dp else 48.dp
        )
        Text(
            text = entry.username,
            color = if (rank == 1) GhostGreen else Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (isYou) {
            Text(text = "(you)", color = GhostGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(FaintBorder))
        PodiumStatLine(label = "Ghosted", value = entry.ghostedCount.toString())
        PodiumStatLine(label = "Cooled", value = entry.coolingCount.toString())
        PodiumStatLine(label = "Saved", amountCents = entry.moneyKeptCents)
    }
}

@Composable
private fun PodiumStatLine(label: String, value: String? = null, amountCents: Long? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
        Text(label, color = MutedText, fontSize = 9.sp)
        if (amountCents != null) {
            DirhamAmount(formatDirhamAmount(amountCents), tint = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, glyphSize = 9.dp)
        } else if (value != null) {
            Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    rank: Int,
    isYou: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isYou) GreenTint else Paper)
            .border(1.dp, if (isYou) GhostGreen else FaintBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$rank",
            color = MutedText,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(end = 12.dp).size(24.dp)
        )
        AvatarCircle(entry.avatarUrl, entry.avatarPresetId, entry.username)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = entry.username + if (isYou) " (you)" else "",
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                GhostMascotPose(poseName = "peek", modifier = Modifier.size(12.dp))
                Text(" Ghosted", color = MutedText, fontSize = 9.sp)
                Text(" ${entry.ghostedCount}", color = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                Icon(Icons.Filled.AcUnit, contentDescription = null, tint = MutedText, modifier = Modifier.size(11.dp))
                Text(" Cooled", color = MutedText, fontSize = 9.sp)
                Text(" ${entry.coolingCount}", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                Icon(Icons.Filled.Sell, contentDescription = null, tint = MutedText, modifier = Modifier.size(11.dp))
                Text(" Saved ", color = MutedText, fontSize = 9.sp)
                DirhamAmount(formatDirhamAmount(entry.moneyKeptCents), tint = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, glyphSize = 9.dp)
            }
        }
    }
}

/**
 * Standalone page (not a bottom-nav tab) reached via a Home banner - the
 * user asked for this to stay simple, not a whole new nav destination.
 * Only opted-in users (username + communityConsent) ever appear here; the
 * anonymous community-products feed is a completely separate, unaffected
 * surface. Tapping any entry (podium or list) opens that member's
 * leaderboard detail view.
 */
@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    loading: Boolean,
    currentUsername: String?,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(Paper).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GhostTopBar(title = "Community Leaderboard", onBack = onBack)
        Text(
            text = "Ranked by items ghosted (checked out). Money cooled & saved is shown too, but doesn't affect rank. Only members who opted in from Profile show up here.",
            color = MutedText,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
        )

        if (loading && entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GhostGreen)
            }
        } else if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No one's on the board yet", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Opt in from your Profile to be the first.",
                        color = MutedText,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            val top3 = entries.take(3)
            val rest = entries.drop(3)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (top3.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // #2 left, #1 center (emphasized), #3 right - matches the
                            // reference podium layout, not simple rank order.
                            val second = top3.getOrNull(1)
                            val first = top3.getOrNull(0)
                            val third = top3.getOrNull(2)
                            if (second != null) {
                                PodiumCard(
                                    entry = second, rank = 2,
                                    isYou = currentUsername == second.username,
                                    onClick = { onOpenDetail(second.username) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (first != null) {
                                PodiumCard(
                                    entry = first, rank = 1,
                                    isYou = currentUsername == first.username,
                                    onClick = { onOpenDetail(first.username) },
                                    modifier = Modifier.weight(1.15f)
                                )
                            }
                            if (third != null) {
                                PodiumCard(
                                    entry = third, rank = 3,
                                    isYou = currentUsername == third.username,
                                    onClick = { onOpenDetail(third.username) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                items(rest) { entry ->
                    val rank = entries.indexOf(entry) + 1
                    LeaderboardRow(
                        entry = entry,
                        rank = rank,
                        isYou = currentUsername != null && entry.username == currentUsername,
                        onClick = { onOpenDetail(entry.username) }
                    )
                }
            }
        }
    }
}
