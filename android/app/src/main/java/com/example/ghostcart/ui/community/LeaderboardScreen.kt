package com.example.ghostcart.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.ghostcart.data.LeaderboardEntry
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.ui.common.GhostTopBar
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper

private fun formatKeptAmount(cents: Long): String = if (cents > 0) {
    "${Marketplace.currency} " + "%,.2f".format(java.util.Locale.US, cents / 100.0)
} else {
    "${Marketplace.currency} 0.00"
}

@Composable
private fun LeaderboardStat(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = MutedText, fontSize = 9.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/**
 * Standalone page (not a bottom-nav tab) reached via a Home banner - the
 * user asked for this to stay simple, not a whole new nav destination.
 * Only opted-in users (username + communityConsent) ever appear here; the
 * anonymous community-products feed is a completely separate, unaffected
 * surface.
 */
@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    loading: Boolean,
    currentUsername: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(Paper).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GhostTopBar(title = "Community Leaderboard", onBack = onBack)
        Text(
            text = "Ranked by money cooled & saved. Ghosted (checked-out) purchases are shown too, but don't affect rank. Only members who opted in from Profile show up here.",
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(entries) { entry ->
                    val isYou = currentUsername != null && entry.username == currentUsername
                    val rank = entries.indexOf(entry) + 1
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(if (isYou) GreenTint else Paper)
                            .border(1.dp, if (isYou) GhostGreen else FaintBorder, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#$rank",
                                color = if (rank <= 3) GhostGreen else MutedText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(FaintBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                if (entry.avatarUrl != null) {
                                    AsyncImage(
                                        model = entry.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Text(entry.username.take(1).uppercase(), color = Ink, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Text(
                                text = entry.username + if (isYou) " (you)" else "",
                                color = Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LeaderboardStat(
                                label = "Cooled & saved",
                                value = "${entry.savedCount} · ${formatKeptAmount(entry.moneyKeptCents)}",
                                valueColor = GhostGreen,
                                modifier = Modifier.weight(1f)
                            )
                            LeaderboardStat(
                                label = "Ghosted",
                                value = "${entry.ghostedCount} · ${formatKeptAmount(entry.ghostedAmountCents)}",
                                valueColor = Ink,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
