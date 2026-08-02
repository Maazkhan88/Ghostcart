package com.example.ghostcart.ui.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.ghostcart.data.LeaderboardActivityEntry
import com.example.ghostcart.data.LeaderboardDetail
import com.example.ghostcart.data.LeaderboardRecentItem
import com.example.ghostcart.data.avatarPresetById
import com.example.ghostcart.ui.DirhamAmount
import com.example.ghostcart.ui.ProductPhoto
import com.example.ghostcart.ui.common.GhostTopBar
import com.example.ghostcart.ui.formatDirhamAmount
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private fun relativeTime(iso: String): String {
    val millis = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX")
        .firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }.parse(iso)?.time
            }.getOrNull()
        } ?: return ""
    val diff = System.currentTimeMillis() - millis
    if (diff < 0) return "just now"
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        hours < 1 -> "just now"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

@Composable
private fun DetailStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun MonthStatColumn(label: String, value: String, changePercent: Int?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, color = GhostGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        if (changePercent != null) {
            Text(
                text = "${if (changePercent >= 0) "+" else ""}$changePercent% vs last month",
                color = MutedText,
                fontSize = 8.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(FaintBorder)
        ) {
            val fraction = changePercent?.let { (50 + it).coerceIn(5, 100) / 100f } ?: 0.4f
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(GhostGreen)
            )
        }
    }
}

@Composable
private fun RecentItemRow(item: LeaderboardRecentItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(SoftGray)) {
            ProductPhoto(productName = item.name, fallbackIconName = "sell", modifier = Modifier.fillMaxSize())
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().background(Color.White)
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(item.category, color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
        DirhamAmount(formatDirhamAmount(item.amountCents), tint = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, glyphSize = 10.dp)
    }
}

@Composable
private fun ActivityRow(entry: LeaderboardActivityEntry) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(GreenTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.AcUnit, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(14.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(entry.label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(relativeTime(entry.createdAt), color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 1.dp))
        }
        if (entry.amountCents > 0) {
            DirhamAmount(formatDirhamAmount(entry.amountCents), tint = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, glyphSize = 9.dp)
        }
    }
}

/**
 * Reached by tapping any leaderboard entry (podium or list row). Aggregate
 * stats (rank/Ghosted/Cooled/Saved) are always public - that's unchanged
 * from the existing leaderboard opt-in. Recent ghosted items and recent
 * activity are additionally gated on that member's own
 * showRecentActivityPublicly toggle (Profile screen); when they've kept it
 * off, this shows a private-state message instead of fabricating anything.
 */
@Composable
fun LeaderboardDetailScreen(
    detail: LeaderboardDetail?,
    loading: Boolean,
    isYou: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Paper).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GhostTopBar(title = "Leaderboard details", onBack = onBack)

        if (loading && detail == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GhostGreen)
            }
            return
        }
        if (detail == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This member isn't available right now", color = MutedText, fontSize = 12.sp)
            }
            return
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GreenTint)
                    .border(1.dp, GhostGreen, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Ink),
                        contentAlignment = Alignment.Center
                    ) {
                        val preset = avatarPresetById(detail.avatarPresetId)
                        when {
                            preset != null -> Image(
                                painter = painterResource(preset.drawableRes),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(6.dp)
                            )
                            detail.avatarUrl != null -> AsyncImage(
                                model = detail.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                            else -> Text(detail.username.take(1).uppercase(), color = Paper, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            text = detail.username + if (isYou) " (you)" else "",
                            color = Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if ("Top 1" in detail.achievements) {
                            Text(
                                text = "👑 Top 1",
                                color = Ink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFFFFD54F))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("#${detail.rank}", color = GhostGreen, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Rank", color = MutedText, fontSize = 9.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailStat("Ghosted", "${detail.ghostedCount} items", modifier = Modifier.weight(1f))
                    DetailStat("Cooled", "${detail.coolingCount} items", modifier = Modifier.weight(1f))
                    DetailStat("Money saved", formatDirhamAmount(detail.moneyKeptCents), modifier = Modifier.weight(1f))
                    DetailStat("Current streak", "${detail.currentStreakDays} days", modifier = Modifier.weight(1f))
                }
            }

            if (!detail.activityPrivate) {
                if (detail.recentGhostedItems.isNotEmpty()) {
                    Text(
                        "Recent ghosted items",
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp)
                    ) {
                        detail.recentGhostedItems.forEach { RecentItemRow(it) }
                    }
                }

                if (detail.achievements.isNotEmpty()) {
                    Text(
                        "Achievements",
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        detail.achievements.forEach { badge ->
                            Text(
                                text = badge,
                                color = Ink,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .border(1.dp, GhostGreen, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                detail.thisMonth?.let { month ->
                    Text(
                        "This month",
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MonthStatColumn("Ghosted", "${month.ghostedCount}", month.ghostedCountChangePercent, modifier = Modifier.weight(1f))
                        MonthStatColumn("Saved", formatDirhamAmount(month.savedCents), month.savedCentsChangePercent, modifier = Modifier.weight(1f))
                        MonthStatColumn("Cooled", "${month.coolingCount}", null, modifier = Modifier.weight(1f))
                    }
                }

                if (detail.recentActivity.isNotEmpty()) {
                    Text(
                        "Recent activity",
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp)
                    ) {
                        detail.recentActivity.forEach { ActivityRow(it) }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoftGray)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MutedText, modifier = Modifier.size(24.dp))
                    Text(
                        "This member keeps their recent activity private",
                        color = MutedText,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            Box(modifier = Modifier.height(24.dp))
        }
    }
}
