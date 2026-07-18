package com.example.ghostcart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ghostcart.data.WalletDemoData
import com.ghostcart.app.R
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.GhostCartTheme
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.app.AppViewModel
import com.example.ghostcart.ui.checkout.FakeDeliveryTrackingScreen
import com.example.ghostcart.ui.checkout.GhostCartListScreen
import com.example.ghostcart.ui.checkout.GhostCheckoutScreen
import com.example.ghostcart.ui.checkout.OrderGhostedSuccessScreen
import com.example.ghostcart.ui.onboarding.AuthScreen
import com.example.ghostcart.ui.onboarding.PersonalizationScreen
import com.example.ghostcart.ui.onboarding.ProfileSelectScreen
import com.example.ghostcart.ui.v2.CaptureAlmostBuyScreen
import com.example.ghostcart.ui.v2.CooldownsScreen
import com.example.ghostcart.ui.v2.GhostHomeScreen
import com.example.ghostcart.ui.v2.ProfileScreen
import com.example.ghostcart.ui.v2.ProgressScreen
import kotlinx.coroutines.delay

private val bottomDestinations: Set<NavKey> = setOf(Home, Cooldowns, GhostCartList, Progress, GhostCardSettings)

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
    val showBottomNav = current in bottomDestinations
    var dismissedOrderId by remember { mutableStateOf<String?>(null) }
    val showDeliveryBanner = current != FakeDeliveryTracking &&
        state.deliveryStep in 0..3 &&
        state.lastOrderId.isNotBlank() &&
        dismissedOrderId != state.lastOrderId
    val darkTheme = when (state.appTheme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }

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

    GhostCartTheme(darkTheme = darkTheme) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Paper,
            bottomBar = {
                if (showBottomNav) {
                    Column {
                        if (showDeliveryBanner) {
                            DeliveryTrackingBanner(
                                orderId = state.lastOrderId,
                                deliveryStep = state.deliveryStep,
                                onClick = { backStack.add(FakeDeliveryTracking) },
                                onClose = { dismissedOrderId = state.lastOrderId }
                            )
                        }
                        GhostBottomNav(current) { destination ->
                            if (backStack.lastOrNull() != destination) backStack.add(destination)
                        }
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
                            }
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
                                appViewModel.addToCart(id)
                            },
                            onCoolCatalog = { id ->
                                appViewModel.prepareCatalogProduct(id)
                                backStack.add(CaptureAlmostBuy)
                            },
                            onGhostCommunity = { id ->
                                appViewModel.addCommunityToCart(id)
                            },
                            onCoolCommunity = { id ->
                                appViewModel.prepareCommunityProduct(id)
                                backStack.add(CaptureAlmostBuy)
                            },
                            onRefresh = { appViewModel.refreshCommunityProducts() }
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
                            onAddToCart = {
                                appViewModel.addDraftToCart(it)
                                appViewModel.clearCaptureSeed()
                                backStack.clear()
                                backStack.add(GhostCartList)
                            },
                            onCoolIt = {
                                appViewModel.createAlmostBuy(it)
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
                    entry<GhostCartList> {
                        GhostCartListScreen(
                            products = appViewModel.cartProductsWithQuantities(),
                            coolingUntilByProductId = state.coolingUntilByProductId,
                            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() else backStack.add(Home) },
                            onAdd = appViewModel::addToCart,
                            onRemove = appViewModel::removeFromCart,
                            onClearAll = appViewModel::clearCart,
                            onStartCooling = appViewModel::startCoolingPeriod,
                            onCheckout = {
                                if (state.cartQuantities.isEmpty()) appViewModel.showToast("Add an item before checkout")
                                else backStack.add(GhostCheckout)
                            }
                        )
                    }
                    entry<GhostCheckout> {
                        GhostCheckoutScreen(
                            products = appViewModel.cartProductsWithQuantities(),
                            walletBalance = WalletDemoData.currentBalance,
                            simulationIntervalMinutes = state.simulationIntervalMinutes,
                            onSelectInterval = appViewModel::setSimulationInterval,
                            onBack = { backStack.removeLastOrNull() },
                            onPlaceOrder = {
                                appViewModel.placeSimulatedOrder()
                                backStack.add(OrderGhostedSuccess)
                            }
                        )
                    }
                    entry<OrderGhostedSuccess> {
                        OrderGhostedSuccessScreen(
                            orderId = state.lastOrderId,
                            amountAvoided = state.lastOrderTotal,
                            onTrackDelivery = {
                                appViewModel.startDeliveryTracking()
                                backStack.add(FakeDeliveryTracking)
                            },
                            onViewSavings = {
                                backStack.clear()
                                backStack.add(Progress)
                            }
                        )
                    }
                    entry<FakeDeliveryTracking> {
                        FakeDeliveryTrackingScreen(
                            orderId = state.lastOrderId,
                            amountSaved = state.lastOrderTotal,
                            deliveryStep = state.deliveryStep,
                            feedbackSubmitted = state.lastOrderId in state.feedbackSubmittedOrderIds,
                            onSubmitFeedback = { rating, comment ->
                                appViewModel.submitGhostFeedback(state.lastOrderId, rating, comment)
                            },
                            onViewReceipt = {
                                backStack.clear()
                                backStack.add(Home)
                            }
                        )
                    }
                    entry<GhostCardSettings> {
                        ProfileScreen(
                            config = state.walletConfig,
                            authEmail = state.authEmail,
                            appTheme = state.appTheme,
                            onSetCardholderName = { name ->
                                appViewModel.updateWalletConfig { it.copy(cardholderName = name) }
                                appViewModel.showToast("Name updated")
                            },
                            onSelectTheme = { theme -> appViewModel.updateWalletConfig { it.copy(cardTheme = theme) } },
                            onSelectAppTheme = appViewModel::setAppTheme,
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

private val deliveryStepLabels = listOf(
    "Order placed",
    "Preparing imaginary order",
    "Ghost Rider is on the way",
    "Rider left absolutely nothing at your doorstep"
)

@Composable
private fun DeliveryTrackingBanner(orderId: String, deliveryStep: Int, onClick: () -> Unit, onClose: () -> Unit) {
    val safeStep = deliveryStep.coerceIn(0, deliveryStepLabels.lastIndex)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(GhostGreen),
            contentAlignment = Alignment.Center
        ) {
            GhostMascotPose(poseName = "wave", modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(text = "Tracking $orderId", color = Color.White.copy(alpha = 0.68f), fontSize = 9.sp)
            Text(text = deliveryStepLabels[safeStep], color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.68f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun GhostBottomNav(current: NavKey?, onNavigate: (NavKey) -> Unit) {
    data class Item(val label: String, val destination: NavKey, val icon: androidx.compose.ui.graphics.vector.ImageVector, val central: Boolean = false)
    val items = listOf(
        Item(stringResource(R.string.nav_home), Home, Icons.Filled.Home),
        Item(stringResource(R.string.nav_cooldowns), Cooldowns, Icons.Filled.Timer),
        Item("Ghost Cart", GhostCartList, Icons.Filled.ShoppingCart, central = true),
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
