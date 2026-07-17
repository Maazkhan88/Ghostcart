package com.example.ghostcart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.ghostcart.data.AlmostBuyResolution
import com.ghostcart.app.R
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.app.AppViewModel
import com.example.ghostcart.ui.onboarding.AuthScreen
import com.example.ghostcart.ui.onboarding.PersonalizationScreen
import com.example.ghostcart.ui.onboarding.ProfileSelectScreen
import com.example.ghostcart.ui.v2.CaptureAlmostBuyScreen
import com.example.ghostcart.ui.v2.CooldownsScreen
import com.example.ghostcart.ui.v2.GhostHomeScreen
import com.example.ghostcart.ui.v2.ProfileScreen
import com.example.ghostcart.ui.v2.ProgressScreen
import kotlinx.coroutines.delay

private val bottomDestinations: Set<NavKey> = setOf(Home, Cooldowns, Progress, GhostCardSettings)

@Composable
fun MainNavigation(
    initialCooldownId: String? = null,
    initialSharedUrl: String? = null,
    initialSharedTitle: String? = null,
    initialSharedImageUrl: String? = null,
    sharedRequestKey: Long? = null
) {
    val initial = when { initialSharedUrl != null -> CaptureAlmostBuy; initialCooldownId != null -> Cooldowns; else -> Splash }
    val backStack = rememberNavBackStack(initial)
    val appViewModel: AppViewModel = viewModel()
    val state by appViewModel.uiState.collectAsState()
    val current = backStack.lastOrNull()
    val showBottomNav = current in bottomDestinations || current == CaptureAlmostBuy

    LaunchedEffect(initialCooldownId) {
        if (initialCooldownId != null && backStack.lastOrNull() != Cooldowns) {
            backStack.add(Cooldowns)
        }
    }

    LaunchedEffect(sharedRequestKey) {
        if (initialSharedUrl != null) {
            appViewModel.importSharedProduct(initialSharedUrl, initialSharedTitle, initialSharedImageUrl)
            if (backStack.lastOrNull() != CaptureAlmostBuy) backStack.add(CaptureAlmostBuy)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Paper,
            bottomBar = {
                if (showBottomNav) {
                    GhostBottomNav(current) { destination ->
                        if (backStack.lastOrNull() != destination) backStack.add(destination)
                    }
                }
            }
        ) { insets ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(insets),
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<Splash> {
                        LaunchedEffect(state.authEmail) {
                            delay(1_200)
                            backStack.clear()
                            backStack.add(if (state.authEmail == null) Auth else Home)
                        }
                        SplashContent()
                    }
                    entry<Auth> {
                        AuthScreen(
                            onAuthSuccess = { email ->
                                appViewModel.authenticate(email)
                                backStack.add(ProfileSelect)
                            },
                            onGuest = {
                                backStack.clear()
                                backStack.add(Home)
                            },
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<ProfileSelect> {
                        ProfileSelectScreen(
                            selectedProfile = state.selectedProfile,
                            onSelectProfile = appViewModel::selectProfile,
                            onContinue = { backStack.add(Personalization) },
                            onSkip = {
                                backStack.clear()
                                backStack.add(Home)
                            }
                        )
                    }
                    entry<Personalization> {
                        PersonalizationScreen(
                            selectedCategoryIds = state.selectedOverspendIds,
                            onToggleCategory = appViewModel::toggleOverspendCategory,
                            selectedSavingsGoal = state.selectedSavingsGoal,
                            onSelectSavingsGoal = appViewModel::selectSavingsGoal,
                            onContinue = {
                                backStack.clear()
                                backStack.add(Home)
                            }
                        )
                    }
                    entry<Home> {
                        GhostHomeScreen(
                            items = state.almostBuys,
                            catalogProducts = appViewModel.allProducts,
                            communityProducts = state.communityProducts,
                            communityProductsLoading = state.communityProductsLoading,
                            onGhostSomething = {
                                appViewModel.clearCaptureSeed()
                                backStack.add(CaptureAlmostBuy)
                            },
                            onOpenCooldowns = { backStack.add(Cooldowns) },
                            onOpenProgress = { backStack.add(Progress) },
                            onGhostCatalog = { id ->
                                appViewModel.quickGhostCatalogProduct(id) { backStack.add(Cooldowns) }
                            },
                            onCoolCatalog = { id ->
                                appViewModel.prepareCatalogProduct(id)
                                backStack.add(CaptureAlmostBuy)
                            },
                            onGhostCommunity = { id ->
                                appViewModel.quickGhostCommunityProduct(id) { backStack.add(Cooldowns) }
                            },
                            onCoolCommunity = { id ->
                                appViewModel.prepareCommunityProduct(id)
                                backStack.add(CaptureAlmostBuy)
                            }
                        )
                    }
                    entry<CaptureAlmostBuy> {
                        CaptureAlmostBuyScreen(
                            seed = state.captureSeed,
                            importState = state.productImportState,
                            onImportSharedUrl = appViewModel::importSharedProduct,
                            onBack = {
                                appViewModel.clearCaptureSeed()
                                backStack.removeLastOrNull()
                            },
                            onGhost = { appViewModel.createAlmostBuy(it) },
                            onComplete = {
                                appViewModel.clearCaptureSeed()
                                backStack.clear()
                                backStack.add(Cooldowns)
                            }
                        )
                    }
                    entry<Cooldowns> {
                        CooldownsScreen(
                            almostBuys = state.almostBuys,
                            onGhostSomething = { backStack.add(CaptureAlmostBuy) },
                            onResolve = { id, resolution -> appViewModel.resolveAlmostBuy(id, resolution) },
                            onMoreTime = appViewModel::extendAlmostBuy
                        )
                    }
                    entry<Progress> { ProgressScreen(almostBuys = state.almostBuys) }
                    entry<GhostCardSettings> {
                        ProfileScreen(
                            config = state.walletConfig,
                            authEmail = state.authEmail,
                            onSetCardholderName = { name ->
                                appViewModel.updateWalletConfig { it.copy(cardholderName = name) }
                                appViewModel.showToast("Name updated")
                            },
                            onSelectTheme = { theme -> appViewModel.updateWalletConfig { it.copy(cardTheme = theme) } },
                            onDownloadCard = appViewModel::downloadGhostCard,
                            onToggleCooling = {
                                appViewModel.updateWalletConfig { it.copy(coolingNotificationsEnabled = !it.coolingNotificationsEnabled) }
                            },
                            onToggleLunch = {
                                appViewModel.updateWalletConfig { it.copy(lunchReminderEnabled = !it.lunchReminderEnabled) }
                            },
                            onToggleDinner = {
                                appViewModel.updateWalletConfig { it.copy(dinnerReminderEnabled = !it.dinnerReminderEnabled) }
                            },
                            onSignOut = {
                                appViewModel.signOut()
                                backStack.clear()
                                backStack.add(Splash)
                            }
                        )
                    }
                }
            )
        }

        state.toastMessage?.let { message ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 42.dp, start = 18.dp, end = 18.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ink)
                    .border(1.dp, GhostGreen, RoundedCornerShape(14.dp))
                    .padding(horizontal = 15.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = GhostGreen, modifier = Modifier.size(18.dp))
                Text(message, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun SplashContent() {
    Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GhostMascotPose("wave", Modifier.size(100.dp))
            Spacer(Modifier.height(16.dp))
            Text("Ghost Cart", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("For everything you almost bought.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun GhostBottomNav(current: NavKey?, onNavigate: (NavKey) -> Unit) {
    data class Item(val label: String, val destination: NavKey, val icon: androidx.compose.ui.graphics.vector.ImageVector, val central: Boolean = false)
    val items = listOf(
        Item(stringResource(R.string.nav_home), Home, Icons.Filled.Home),
        Item(stringResource(R.string.nav_cooldowns), Cooldowns, Icons.Filled.Timer),
        Item(stringResource(R.string.nav_ghost), CaptureAlmostBuy, Icons.Filled.Add, central = true),
        Item(stringResource(R.string.nav_progress), Progress, Icons.Filled.Timeline),
        Item(stringResource(R.string.nav_profile), GhostCardSettings, Icons.Filled.Person)
    )

    NavigationBar(containerColor = Paper, tonalElevation = 0.dp) {
        items.forEach { item ->
            val selected = current == item.destination
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(if (item.central) 42.dp else 30.dp)
                            .clip(CircleShape)
                            .background(if (item.central) GhostGreen else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (item.central) Ink else if (selected) GhostGreen else MutedText,
                            modifier = Modifier.size(if (item.central) 23.dp else 20.dp)
                        )
                    }
                },
                label = { Text(item.label, color = if (selected) Ink else MutedText, fontSize = 9.sp) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
            )
        }
    }
}
