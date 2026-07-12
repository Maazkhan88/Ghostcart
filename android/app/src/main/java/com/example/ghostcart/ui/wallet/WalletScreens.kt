package com.example.ghostcart.ui.wallet

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.SavingsGoal
import com.example.ghostcart.data.WalletConfig
import com.example.ghostcart.data.WalletDemoData
import com.example.ghostcart.data.WalletTransaction
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.ProductIcon
import com.example.ghostcart.ui.common.BackButton
import com.example.ghostcart.ui.common.ForwardChevron
import com.example.ghostcart.ui.common.GhostHeroCard
import com.example.ghostcart.ui.common.GhostTopBar
import com.example.ghostcart.ui.common.PrimaryButton
import com.example.ghostcart.ui.common.RoundIconButton
import com.example.ghostcart.ui.common.SecondaryButton
import com.example.ghostcart.ui.common.SimulationBadge
import com.example.ghostcart.ui.common.StatColumn
import com.example.ghostcart.ui.common.ThinProgressBar
import com.example.ghostcart.ui.common.materialIconFor

@Composable
fun WalletHomeScreen(
    onBack: (() -> Unit)? = null,
    onGhostPay: () -> Unit,
    onViewWallet: () -> Unit,
    onViewActivity: () -> Unit,
    onViewGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            GhostMascotPose(poseName = "wave", modifier = Modifier.size(36.dp))
            Text(text = "Ghost Wallet", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f).padding(start = 10.dp))
            RoundIconButton(icon = Icons.Filled.Notifications)
        }

        GhostHeroCard(modifier = Modifier.padding(top = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(72.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Ghost Card", color = Paper, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        SimulationBadge(text = "Demo wallet", dark = true)
                    }
                    Text(text = WalletDemoData.cardHolderName, color = Paper, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
                    Text(text = "GC•••• ${WalletDemoData.cardLastFour}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(text = "Balance", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp))
                    Text(text = "${Marketplace.currency} ${WalletDemoData.currentBalance}", color = Paper, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WalletStatCard(icon = Icons.Filled.Shield, label = "Protected\nthis month", value = "${Marketplace.currency} ${WalletDemoData.protectedThisMonth}", modifier = Modifier.weight(1f), onClick = onViewActivity)
            WalletStatCard(icon = materialIconFor("target"), label = "Ghosted\norders", value = "${WalletDemoData.ghostedOrdersThisMonth}", modifier = Modifier.weight(1f), onClick = onViewActivity)
            WalletStatCard(icon = materialIconFor("target"), label = "Savings\ngoal", value = "${WalletDemoData.savingsGoalPercent}%", modifier = Modifier.weight(1f), onClick = onViewGoals)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Recent Ghost Saves", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "View all", color = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onViewActivity))
        }
        Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            WalletDemoData.recentSaves.take(3).forEach { tx -> TransactionRow(tx) }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ink)
                    .clickable(onClick = onGhostPay)
                    .padding(14.dp)
            ) {
                Column {
                    GhostMascotPose(poseName = "wave", modifier = Modifier.size(24.dp))
                    Text(text = "Ghost Pay", color = Paper, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
                    Text(text = "Pay privately", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onViewWallet)
                    .padding(14.dp)
            ) {
                Column {
                    Icon(materialIconFor("wallet"), contentDescription = null, tint = Ink, modifier = Modifier.size(24.dp))
                    Text(text = "View Wallet", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
                    Text(text = "View balance & history", color = MutedText, fontSize = 10.sp)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GreenTint)
                .clickable(onClick = onViewWallet)
                .padding(14.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(text = "Wallet controls", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "Review goals, Salary Shield, and Ghost Card.", color = MutedText, fontSize = 10.sp)
            }
            ForwardChevron()
        }
    }
}

@Composable
private fun WalletStatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(16.dp))
        Text(text = label, color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 6.dp))
        Text(text = value, color = GhostGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun TransactionRow(tx: WalletTransaction) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(SoftGray), contentAlignment = Alignment.Center) {
            ProductIcon(name = tx.iconName, modifier = Modifier.size(22.dp), color = Ink)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(text = tx.name, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "${tx.whenLabel} • ${tx.category}", color = MutedText, fontSize = 10.sp)
        }
        Text(text = "${Marketplace.currency} ${tx.amount}", color = GhostGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        ForwardChevron(modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
fun WalletSetupScreen(
    config: WalletConfig,
    onBack: () -> Unit,
    onToggleSalaryShield: () -> Unit,
    onOpenSalaryShield: () -> Unit,
    onCreateWallet: () -> Unit,
    onContinueWithDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GhostTopBar(title = "Ghost Wallet", onBack = onBack)
        Text(text = "Set up your Ghost Wallet", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp))
        Text(text = "Choose the amount you want to protect and track inside Ghost Cart.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        Column(modifier = Modifier.padding(top = 16.dp)) {
            SetupRow(materialIconFor("work"), "Monthly salary", "${Marketplace.currency} ${config.monthlySalary}")
            SetupRow(materialIconFor("target"), "Monthly savings goal", "${Marketplace.currency} ${config.monthlySavingsGoal}")
            SetupRow(materialIconFor("bag"), "Temptation budget", "${Marketplace.currency} ${config.temptationBudget}")
            SetupRow(materialIconFor("wallet"), "Starting wallet balance", "${Marketplace.currency} ${config.startingBalance}")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, FaintBorder, RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenSalaryShield)
                    .padding(14.dp)
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(text = "Salary Shield", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Automatically protect your salary every month.", color = MutedText, fontSize = 10.sp)
                }
                Switch(checked = config.salaryShieldEnabled, onCheckedChange = { onToggleSalaryShield() }, colors = SwitchDefaults.colors(checkedTrackColor = GhostGreen))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GreenTint)
                    .padding(14.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(text = "Ready when you are", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "You can change these settings anytime.", color = MutedText, fontSize = 9.sp)
                }
                ForwardChevron()
            }

            PrimaryButton(text = "Create Ghost Wallet", onClick = onCreateWallet, leadingIcon = null, modifier = Modifier.padding(top = 20.dp))
            Text(
                text = "Continue with defaults",
                color = GhostGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).clickable(onClick = onContinueWithDefaults),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SetupRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, FaintBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(SoftGray), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
        }
        Text(text = label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text(text = value, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SalaryShieldScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GhostTopBar(
            title = "Salary Shield",
            onBack = onBack,
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, FaintBorder, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "How it works", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MutedText, modifier = Modifier.size(12.dp).padding(start = 4.dp))
                }
            }
        )

        GhostHeroCard(modifier = Modifier.padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = Paper, modifier = Modifier.size(26.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(text = "Connected to Ghost Wallet", color = Paper, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = "The first 72 hours after salary drops are the most dangerous.", color = Paper, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Paper)
                    .padding(14.dp)
            ) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(GreenTint), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(text = "Salary Shield Active", color = GhostGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Your money is protected from impulse spending.", color = MutedText, fontSize = 10.sp)
                    Text(text = "${Marketplace.currency} ${WalletDemoData.protectedThisMonth}", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                    Text(text = "protected this cycle", color = MutedText, fontSize = 9.sp)
                }
                Icon(Icons.Filled.Lock, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(18.dp))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, FaintBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Icon(Icons.Filled.AcUnit, contentDescription = null, tint = MutedText, modifier = Modifier.size(16.dp))
            Text(text = "Cooling down", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
            Text(text = "23:59:21", color = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Midnight Burger Meal", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = "${Marketplace.currency} 75", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            ThinProgressBar(progress = 0.35f, modifier = Modifier.padding(top = 6.dp))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeedsCravingsColumn(
                title = "Needs first",
                subtitle = "Secure your essentials.",
                items = listOf("Rent" to 1500, "DEWA" to 280, "Groceries" to 420, "Savings goal" to 300),
                totalLabel = "Total planned",
                highlighted = true,
                modifier = Modifier.weight(1f)
            )
            NeedsCravingsColumn(
                title = "Cravings later",
                subtitle = "Wait. You won't miss out.",
                items = listOf("Food delivery" to 420, "Gadgets" to 950, "Shopping" to 680, "Impulse buys" to 300),
                totalLabel = "Total cravings",
                highlighted = false,
                modifier = Modifier.weight(1f)
            )
        }

        PrimaryButton(text = "Keep Salary Shield On", onClick = {}, leadingIcon = Icons.Filled.Shield, modifier = Modifier.padding(top = 18.dp))
    }
}

@Composable
private fun NeedsCravingsColumn(title: String, subtitle: String, items: List<Pair<String, Int>>, totalLabel: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(text = subtitle, color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(bottom = 8.dp))
        items.forEach { (label, amount) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(text = label, color = Ink, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text(text = "${Marketplace.currency} $amount", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (highlighted) GreenTint else SoftGray)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(text = totalLabel, color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(text = "${Marketplace.currency} ${items.sumOf { it.second }}", color = if (highlighted) GhostGreen else Ink, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun GoalsScreen(goals: List<SavingsGoal> = WalletDemoData.goals, onAllocateSavings: () -> Unit, onCreateGoal: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            GhostMascotPose(poseName = "wave", modifier = Modifier.size(36.dp))
            Text(text = "Ghost Wallet", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f).padding(start = 10.dp))
            RoundIconButton(icon = Icons.Filled.Notifications)
        }
        Text(text = "Your Goals", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp))
        Text(text = "Save smarter with Ghost Wallet pocket goals.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        Column(modifier = Modifier.padding(top = 16.dp)) {
            goals.forEach { goal ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, FaintBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(SoftGray), contentAlignment = Alignment.Center) {
                            Icon(materialIconFor(goal.iconName), contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(text = goal.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = goal.tagline, color = MutedText, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "${Marketplace.currency} ${goal.current}", color = GhostGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = "of ${Marketplace.currency} ${goal.target}", color = MutedText, fontSize = 10.sp)
                        }
                    }
                    ThinProgressBar(progress = goal.percent / 100f, modifier = Modifier.padding(top = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(text = "${goal.percent}% complete", color = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(text = "${Marketplace.currency} ${goal.target - goal.current} left", color = MutedText, fontSize = 11.sp)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GreenTint)
                .padding(14.dp)
        ) {
            GhostMascotPose(poseName = "wave", modifier = Modifier.size(28.dp))
            Text(
                text = "Latest ghosted savings went to ${goals.firstOrNull()?.name ?: "Travel Fund"}",
                color = Ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
            )
            Text(text = "+${Marketplace.currency} 420", color = GhostGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }

        PrimaryButton(text = "Allocate Savings", onClick = onAllocateSavings, leadingIcon = null, trailingIcon = Icons.Filled.ChevronRight)
        SecondaryButton(text = "Create New Goal", onClick = onCreateGoal, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
fun WalletActivityScreen(onBack: () -> Unit, onOpenStatement: () -> Unit, modifier: Modifier = Modifier) {
    val filters = listOf("All", "Today", "This Week", "Ghost Pays", "Goals")
    val selectedFilter = "All"

    Column(modifier = modifier.fillMaxSize().background(Paper).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onBack = onBack)
            Row(modifier = Modifier.weight(1f).padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(28.dp))
                Text(text = "Ghost Wallet Activity", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 8.dp))
            }
            RoundIconButton(icon = Icons.Filled.Notifications)
        }

        GhostHeroCard(modifier = Modifier.padding(top = 16.dp).clickable(onClick = onOpenStatement)) {
            Text(text = "This week protected", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Text(text = "${Marketplace.currency} ${WalletDemoData.weeklyTotalProtected}", color = Paper, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
            Text(text = "Across ${WalletDemoData.weeklyGhostedOrders} ghosted orders", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 14.dp)) {
            items(filters) { filter ->
                val selected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) Ink else Paper)
                        .border(1.dp, if (selected) Ink else FaintBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(text = filter, color = if (selected) Paper else Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            WalletDemoData.recentSaves.forEach { tx -> TransactionRow(tx) }
        }
    }
}

@Composable
fun WeeklyStatementScreen(onBack: () -> Unit, onDownload: () -> Unit, onShare: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GhostTopBar(title = "Ghost Cart", onBack = onBack, trailing = { RoundIconButton(icon = Icons.Filled.Notifications) })
        Text(text = "Weekly Ghost Statement", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp))
        Text(text = "📅 26 May – 1 Jun 2025", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        GhostHeroCard(modifier = Modifier.padding(top = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatColumn(label = "Total protected\nthis week", value = "${Marketplace.currency} ${WalletDemoData.weeklyTotalProtected}", valueColor = Paper)
                StatColumn(label = "Ghosted\norders", value = "${WalletDemoData.weeklyGhostedOrders}", valueColor = Paper)
                StatColumn(label = "Average protected\norder", value = "${Marketplace.currency} ${WalletDemoData.weeklyAverageOrder}", valueColor = Paper)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Savings over the week", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "↗ ${WalletDemoData.weeklyChangePercent}% vs last week", color = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val maxValue = WalletDemoData.weeklyChart.max()
            WalletDemoData.weeklyChart.forEachIndexed { index, value ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${Marketplace.currency} $value", color = Ink, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(24.dp)
                            .height((value.toFloat() / maxValue * 90).dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(GreenTint)
                    )
                    Text(text = WalletDemoData.weeklyChartDays[index], color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Text(text = "Top ghosted items", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 20.dp))
        Column(modifier = Modifier.padding(top = 6.dp)) {
            WalletDemoData.recentSaves.take(3).forEach { tx -> TransactionRow(tx) }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SoftGray)
                .padding(14.dp)
        ) {
            Icon(materialIconFor("target"), contentDescription = null, tint = GhostGreen, modifier = Modifier.size(16.dp))
            Text(text = "Top trigger: Late-night scrolling", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(text = "Download Statement", onClick = onDownload, leadingIcon = Icons.Filled.Download, modifier = Modifier.weight(1f))
            SecondaryButton(text = "Share Ghost Receipt", onClick = onShare, leadingIcon = Icons.Filled.Share, modifier = Modifier.weight(1f))
        }
        Text(
            text = "Generated from demo activity.",
            color = MutedText,
            fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun GhostCardSettingsScreen(
    config: WalletConfig,
    onBack: () -> Unit,
    onToggleNotifications: () -> Unit,
    onToggleAutoAllocate: () -> Unit,
    onToggleFreeze: () -> Unit,
    onDeleteWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GhostTopBar(title = "Ghost Card Settings", onBack = onBack)

        GhostHeroCard(modifier = Modifier.padding(top = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(64.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Ghost Card", color = Paper, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        SimulationBadge(text = "Demo card", dark = true)
                    }
                    Text(text = WalletDemoData.cardHolderName, color = Paper, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
                    Text(text = "GC•••• ${WalletDemoData.cardLastFour}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    Text(text = "Balance", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, modifier = Modifier.padding(top = 10.dp))
                    Text(text = "${Marketplace.currency} ${WalletDemoData.currentBalance}", color = Paper, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Column(modifier = Modifier.padding(top = 12.dp)) {
            SettingsChevronRow(Icons.Filled.Edit, "Rename card", null)
            SettingsChevronRow(Icons.Filled.Palette, "Theme", config.cardTheme)
            SettingsToggleRow(Icons.Filled.Notifications, "Wallet notifications", "Get alerts for orders, saves and goal progress", config.walletNotificationsEnabled, onToggleNotifications)
            SettingsToggleRow(materialIconFor("target"), "Auto-allocate to goals", "Automatically move savings to your goals", config.autoAllocateToGoals, onToggleAutoAllocate)
            SettingsChevronRow(Icons.Filled.Shield, "Salary Shield preference", "Set how much to protect from salary")
            SettingsToggleRow(Icons.Filled.AcUnit, "Freeze internal card", "Temporarily pause all Ghost Card activity", config.cardFrozen, onToggleFreeze)
            SettingsChevronRow(Icons.Filled.Delete, "Delete wallet", "Permanently delete this wallet and all data", danger = true, onClick = onDeleteWallet)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GreenTint)
                .padding(14.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(text = "Ghost Card works only inside Ghost Cart", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "and cannot be used for real-world purchases.", color = MutedText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SettingsChevronRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?, danger: Boolean = false, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SoftGray), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (danger) androidx.compose.ui.graphics.Color(0xFFE0453C) else Ink, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(text = title, color = if (danger) androidx.compose.ui.graphics.Color(0xFFE0453C) else Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(text = subtitle, color = MutedText, fontSize = 10.sp)
        }
        ForwardChevron()
    }
}

@Composable
private fun SettingsToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SoftGray), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(text = title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = MutedText, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = GhostGreen))
    }
}
