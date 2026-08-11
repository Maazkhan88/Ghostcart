package com.example.ghostcart.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.WalletDemoData
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.common.CircularGoalRing
import com.example.ghostcart.ui.common.PrimaryButton
import com.example.ghostcart.ui.common.RoundIconButton
import com.example.ghostcart.ui.common.materialIconFor

@Composable
fun TrendsScreen(onGhostAnotherCart: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 120.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            GhostMascotPose(poseName = "wave", modifier = Modifier.size(36.dp))
            Text(text = "Ghost Cart", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f).padding(start = 10.dp))
            RoundIconButton(icon = Icons.Filled.Notifications)
        }

        Column(modifier = Modifier.padding(top = 20.dp)) {
            Text(text = "Your salary doesn't disappear all at once.", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
            Text(text = "It disappears in small orders.", color = GhostGreen, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
            Text(text = "Good job ghosting. Your future self thanks you.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, FaintBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(materialIconFor("wallet"), contentDescription = null, tint = GhostGreen, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${Marketplace.currency} ${WalletDemoData.currentBalance}", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(16.dp).padding(start = 6.dp))
                    }
                    Text(text = "saved this month", color = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Today", color = MutedText, fontSize = 10.sp)
                    Text(text = "${Marketplace.currency} 320", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                    Text(text = "Saved today", color = MutedText, fontSize = 9.sp)
                }
                Column {
                    Text(text = "This Week", color = MutedText, fontSize = 10.sp)
                    Text(text = "${Marketplace.currency} 1,890", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                    Text(text = "Saved this week", color = MutedText, fontSize = 9.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Monthly Goal", color = MutedText, fontSize = 10.sp)
                    CircularGoalRing(percent = 84, modifier = Modifier.padding(top = 4.dp))
                    Text(text = "of ${Marketplace.currency} 10,000", color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        Text(text = "Most Ghosted Categories", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 22.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false,
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().height(246.dp)
        ) {
            items(WalletDemoData.mostGhostedCategories) { (label, amount, percent) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, FaintBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Icon(materialIconFor(categoryIconFor(label)), contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                    Text(text = label, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text(text = "${Marketplace.currency} $amount", color = GhostGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp))
                    Text(text = "$percent%", color = MutedText, fontSize = 10.sp)
                }
            }
        }

        Text(text = "Top Triggers", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false,
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().height(210.dp)
        ) {
            items(WalletDemoData.topTriggers) { (label, iconName, percent) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GreenTint)
                        .padding(14.dp)
                ) {
                    Icon(materialIconFor(iconName), contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                    Text(text = label, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text(text = "$percent%", color = GhostGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        PrimaryButton(text = "Ghost Another Cart", onClick = onGhostAnotherCart, trailingIcon = Icons.Filled.ArrowForward, modifier = Modifier.padding(top = 22.dp))
    }
}

private fun categoryIconFor(label: String): String = when (label) {
    "Food & Coffee" -> "coffee"
    "Beauty & Perfume" -> "spa"
    "Fashion & Shoes" -> "checkroom"
    "Gadgets & Tech" -> "devices"
    else -> "bag"
}
