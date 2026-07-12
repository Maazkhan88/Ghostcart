package com.example.ghostcart.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.common.PrimaryButton
import com.example.ghostcart.ui.common.SimulationBadge
import com.example.ghostcart.ui.common.materialIconFor

@Composable
fun SplashScreen(onStart: () -> Unit, onGuest: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GhostMascotPose(poseName = "wave", modifier = Modifier.size(96.dp))
            Text(
                text = "Ghost Cart",
                color = Ink,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = "Add to cart. Checkout.\nKeep your money.",
                color = Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "Browse, checkout, and track fake orders without spending a dirham.",
                color = MutedText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, start = 12.dp, end = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF6F6F6))
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OnboardingFeature("Shop UAE Stores", "Explore top brands\nin the UAE", Icons.Filled.LocationOn)
                OnboardingFeature("Stay in Control", "Track impulses and\nbuild better habits", materialIconFor("target"))
                OnboardingFeature("Protect Your Money", "Save more. Spend\nsmarter.", Icons.Filled.Shield)
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PrimaryButton(text = "Start Ghosting", onClick = onStart)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Continue as Guest",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onGuest)
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "✦ Made for the UAE. Built for better habits.",
                color = GhostGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            SimulationBadge(text = "Simulation only. No real payment. No real delivery.", modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun OnboardingFeature(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Icon(icon, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(22.dp))
        Text(
            text = title,
            color = Ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = subtitle,
            color = MutedText,
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun ProfileSelectScreen(
    selectedProfile: String,
    onSelectProfile: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 24.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GhostMascotPose(poseName = "wave", modifier = Modifier.size(40.dp))
            Text(
                text = "Ghost Cart",
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        Text(
            text = "Select your profile",
            color = Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 28.dp)
        )
        Text(
            text = "Choose the ghost that matches your profile.\nYou can change this later.",
            color = MutedText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.padding(top = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            listOf("Male", "Female").forEach { profile ->
                val selected = selectedProfile == profile
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) GhostGreen else FaintBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelectProfile(profile) }
                        .padding(20.dp)
                ) {
                    GhostMascotPose(poseName = if (profile == "Male") "cart" else "thumbsup", modifier = Modifier.size(96.dp))
                    Text(text = profile, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (selected) GhostGreen else Color.Transparent)
                            .border(1.dp, if (selected) GhostGreen else FaintBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MutedText, modifier = Modifier.size(14.dp))
            Text(
                text = "This only personalizes your Ghost Cart experience.",
                color = MutedText,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(text = "Continue", onClick = onContinue)
        Text(
            text = "Skip for now",
            color = MutedText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 14.dp)
                .clickable(onClick = onSkip)
        )
    }
}

@Composable
fun PersonalizationScreen(
    selectedCategoryIds: Set<String>,
    onToggleCategory: (String) -> Unit,
    selectedSavingsGoal: Int,
    onSelectSavingsGoal: (Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Paper)) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(36.dp))
                Text(
                    text = "Ghost Cart",
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = "What do you usually\noverspend on?",
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = "Choose a few categories so we can personalize your ghost marketplace.",
                color = MutedText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(Marketplace.overspendCategories) { category ->
                val selected = selectedCategoryIds.contains(category.id)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, if (selected) GhostGreen else FaintBorder, RoundedCornerShape(14.dp))
                        .clickable { onToggleCategory(category.id) }
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Icon(materialIconFor(category.iconName), contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                    Text(
                        text = category.label,
                        color = Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (selected) GhostGreen else Color.Transparent)
                            .border(1.dp, if (selected) GhostGreen else FaintBorder, CircleShape)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "What's your monthly savings goal?",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "You can change this anytime.",
                color = MutedText,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 2.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (Marketplace.savingsGoalPresets + 0).forEachIndexed { index, amount ->
                    val isCustom = index == Marketplace.savingsGoalPresets.size
                    val label = if (isCustom) "Custom" else "AED $amount"
                    val selected = !isCustom && selectedSavingsGoal == amount
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Ink else Paper)
                            .border(1.dp, if (selected) Ink else FaintBorder, RoundedCornerShape(14.dp))
                            .clickable { if (!isCustom) onSelectSavingsGoal(amount) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Paper else Ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            PrimaryButton(text = "Continue", onClick = onContinue, modifier = Modifier.padding(top = 20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(13.dp))
                Text(
                    text = "Your data stays private and secure.",
                    color = MutedText,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
