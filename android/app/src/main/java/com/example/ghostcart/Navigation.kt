package com.example.ghostcart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import coil3.compose.AsyncImage
import com.example.ghostcart.data.AlmostBuyResolution
import com.example.ghostcart.data.AlmostBuyStatus
import com.example.ghostcart.data.ContentBlockItem
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.openProductSource
import com.example.ghostcart.data.shareGhostItem
import com.example.ghostcart.data.toGhostShareItem
import com.ghostcart.app.R
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.DarkGray
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.GhostCartTheme
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.GhostCartWordmark
import com.example.ghostcart.ui.app.AppViewModel
import com.example.ghostcart.ui.checkout.FakeDeliveryTrackingScreen
import com.example.ghostcart.ui.checkout.GhostCartListScreen
import com.example.ghostcart.ui.checkout.GhostCheckoutScreen
import com.example.ghostcart.ui.checkout.OrderGhostedSuccessScreen
import com.example.ghostcart.ui.onboarding.AuthScreen
import com.example.ghostcart.ui.onboarding.PersonalizationScreen
import com.example.ghostcart.ui.onboarding.ProfileSelectScreen
import com.example.ghostcart.ui.marketplace.ProductDetailScreen
import com.example.ghostcart.ui.marketplace.CategoryBrowseScreen
import com.example.ghostcart.ui.v2.CaptureAlmostBuyScreen
import com.example.ghostcart.ui.v2.CooldownsScreen
import com.example.ghostcart.ui.common.InAppMessageDialog
import com.example.ghostcart.ui.onboarding.SimulationConsentScreen
import com.example.ghostcart.ui.v2.GhostHomeScreen
import com.example.ghostcart.ui.v2.LegalDocumentScreen
import com.example.ghostcart.ui.v2.ProfileScreen
import com.example.ghostcart.ui.v2.ProgressScreen
import com.example.ghostcart.ui.v2.ShareQueueReviewScreen
import kotlinx.coroutines.delay

private val onboardingDestinations: Set<NavKey> = setOf(Splash, Auth, ProfileSelect, Personalization)

private fun selectedBottomDestination(current: NavKey?): NavKey = when (current) {
    Cooldowns -> Cooldowns
    Progress -> Progress
    GhostCardSettings, WalletHome, WalletSetup, SalaryShield, Goals,
    WalletActivity, WeeklyStatement, Trends -> GhostCardSettings
    is LegalDocument -> GhostCardSettings
    GhostCartList, CaptureAlmostBuy, GhostCheckout, OrderGhostedSuccess,
    FakeDeliveryTracking, PayWithGhostCard, OrderProtected -> GhostCartList
    else -> Home
}

@Composable
fun MainNavigation(
    initialCooldownId: String? = null,
    initialSharedUrl: String? = null,
    initialSharedTitle: String? = null,
    initialSharedImageUrl: String? = null,
    sharedRequestKey: Long? = null,
    initialGhostTitle: String? = null,
    initialGhostShareId: String? = null,
    initialGhostPriceCents: Long? = null,
    initialGhostCategory: String? = null,
    initialGhostImageUrl: String? = null,
    initialGhostSourceUrl: String? = null,
    ghostShareRequestKey: Long? = null
) {
    val initial = when {
        initialSharedUrl != null -> CaptureAlmostBuy
        initialGhostTitle != null || initialGhostShareId != null -> CaptureAlmostBuy
        initialCooldownId != null -> Cooldowns
        else -> Splash
    }
    val backStack = rememberNavBackStack(initial)
    val appViewModel: AppViewModel = viewModel()
    val context = LocalContext.current
    val state by appViewModel.uiState.collectAsState()
    val current = backStack.lastOrNull()
    val showBottomNav = current != null && current !in onboardingDestinations
    var dismissedOrderId by remember { mutableStateOf<String?>(null) }
    // Full-screen story viewer overlay state - lives here (not inside
    // GhostHomeScreen) so it renders above the bottom nav/Scaffold entirely,
    // matching how a real Stories viewer covers the whole screen.
    var openStoryIndex by remember { mutableStateOf<Int?>(null) }
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

    // Cooling/ghosting/add-to-cart are gated to signed-in accounts
    // (AppViewModel.requireSignIn) - this pushes the sign-in screen the
    // moment one of those actions is attempted while signed out, mirroring
    // the existing checkout sign-in gate.
    LaunchedEffect(state.authRequiredPrompt) {
        if (state.authRequiredPrompt) {
            if (backStack.lastOrNull() != Auth) backStack.add(Auth)
            appViewModel.consumeAuthRequiredPrompt()
        }
    }

    // In-app nudge for a cooldown that already expired before the user
    // opened the app (the local WorkManager notification already fired, but
    // people miss/dismiss notifications) - surfaces the same Cooldowns
    // resolve prompt proactively instead of waiting for it to be noticed.
    // Only fires once per process, and only once Home has actually been
    // reached (never while still on Splash - it must not race the splash
    // screen or preempt reaching Home at all, since first-open notification
    // permission is requested from Home and would otherwise never fire).
    var expiredCooldownPromptShown by remember { mutableStateOf(false) }
    LaunchedEffect(current, state.almostBuys) {
        if (expiredCooldownPromptShown || initialCooldownId != null) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val hasExpiredCooldown = state.almostBuys.any {
            it.status == AlmostBuyStatus.COOLING && it.coolingUntilMillis <= now
        }
        if (hasExpiredCooldown && current == Home) {
            expiredCooldownPromptShown = true
            backStack.add(Cooldowns)
        }
    }

    LaunchedEffect(sharedRequestKey) {
        if (initialSharedUrl != null) {
            appViewModel.importSharedProduct(initialSharedUrl, initialSharedTitle, initialSharedImageUrl)
            if (backStack.lastOrNull() != CaptureAlmostBuy) backStack.add(CaptureAlmostBuy)
        }
    }

    LaunchedEffect(ghostShareRequestKey) {
        if (initialGhostShareId != null) {
            appViewModel.importGhostShare(initialGhostShareId)
            if (backStack.lastOrNull() != CaptureAlmostBuy) backStack.add(CaptureAlmostBuy)
        } else if (initialGhostTitle != null) {
            appViewModel.prepareSharedGhostItem(
                title = initialGhostTitle,
                priceCents = initialGhostPriceCents ?: 0L,
                category = initialGhostCategory ?: "Other",
                imageUrl = initialGhostImageUrl,
                sourceUrl = initialGhostSourceUrl
            )
            if (backStack.lastOrNull() != CaptureAlmostBuy) backStack.add(CaptureAlmostBuy)
        }
    }

    GhostCartTheme(darkTheme = darkTheme) {
    val consentStatus = state.simulationConsentStatus
    if (consentStatus == null) {
        // Still checking (or the check failed and will retry) - show a neutral splash rather
        // than either the gate or the full app. Letting the full app briefly mount here would
        // let other launch-time UI (e.g. the in-app message dialog) flash on and get torn down
        // the instant the real consentStatus arrives, if it turns out acceptance is required.
        SplashContent()
    } else if (!consentStatus.accepted) {
        SimulationConsentScreen(
            consentText = consentStatus.consentText,
            onAccept = appViewModel::acceptSimulationConsent
        )
    } else {
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
                        GhostBottomNav(
                            selectedBottomDestination(current),
                            cartCount = state.cartQuantities.values.sum()
                        ) { destination ->
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
                        RandomStorySplashScreen(
                            stories = state.ghostCartStories,
                            onFinished = {
                                backStack.clear()
                                backStack.add(if (state.authEmail == null) Auth else Home)
                            }
                        )
                    }
                    entry<Auth> {
                        AuthScreen(
                            onAuthSuccess = { email ->
                                appViewModel.authenticate(email)
                                // If Auth was reached via the checkout sign-in gate (pushed on
                                // top of GhostCartList without clearing the stack), resume the
                                // checkout attempt instead of the first-run onboarding flow.
                                // The backStack itself (rememberNavBackStack, a Serializable
                                // NavKey list) is the durable mechanism here - it survives
                                // config changes and process death, so this doesn't need any
                                // separate transient Compose state to remember where to return.
                                val cameFromCheckoutGate = backStack.getOrNull(backStack.size - 2) == GhostCartList
                                if (cameFromCheckoutGate) {
                                    backStack.removeLastOrNull()
                                    backStack.add(GhostCheckout)
                                } else {
                                    backStack.add(ProfileSelect)
                                }
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
                            unifiedProducts = appViewModel.unifiedMarketplaceProducts(),
                            favoriteProducts = state.favoriteProductIds.mapNotNull(appViewModel::findProduct),
                            favoriteProductIds = state.favoriteProductIds,
                            communityProductsLoading = state.communityProductsLoading,
                            onGhostSomething = {
                                appViewModel.clearCaptureSeed()
                                backStack.add(CaptureAlmostBuy)
                            },
                            onOpenCooldowns = { backStack.add(Cooldowns) },
                            onOpenProgress = { backStack.add(Progress) },
                            onGhost = { id -> appViewModel.addToCart(id) },
                            onCool = { id, durationMillis, _ ->
                                appViewModel.quickGhostCatalogProduct(id, durationMillis)
                                backStack.add(Cooldowns)
                            },
                            onOpen = { id -> backStack.add(ProductDetail(id)) },
                            onToggleFavorite = appViewModel::toggleFavorite,
                            onNotifications = { backStack.add(GhostCardSettings) },
                            onViewAllCatalog = { categoryId -> backStack.add(CategoryBrowse(categoryId)) },
                            onViewAllFavorites = { backStack.add(CategoryBrowse("favorites")) },
                            onRefresh = {
                                appViewModel.refreshCommunityProducts()
                                appViewModel.refreshCatalogProducts()
                                appViewModel.refreshHomeBanners()
                                appViewModel.refreshGhostCartStories()
                            },
                            homeBanners = state.homeBanners,
                            ghostCartStories = state.ghostCartStories,
                            onOpenLeaderboard = { backStack.add(Leaderboard) },
                            onOpenStory = { index -> openStoryIndex = index }
                        )
                    }
                    entry<CategoryBrowse> { key ->
                        val products = when (key.categoryId) {
                            // Kept as a backward-compatible alias for any existing deep link;
                            // the live UI surfaces this same filter as the "User Ghosted" chip
                            // inside the unified "all" listing instead of a separate category.
                            "community" -> appViewModel.unifiedMarketplaceProducts().filter { it.isUserGhosted }
                            "favorites" -> state.favoriteProductIds.mapNotNull(appViewModel::findProduct)
                            "most_ghosted" -> state.mostGhostedToday.mapNotNull { ranking ->
                                appViewModel.findProduct(ranking.productId)
                            }
                            else -> Marketplace.productsForCategory(key.categoryId, appViewModel.unifiedMarketplaceProducts())
                        }
                        CategoryBrowseScreen(
                            categoryId = key.categoryId,
                            products = products,
                            activityCounts = state.ghostCountsByProductId +
                                state.mostGhostedToday.associate { it.productId to it.ghostCount },
                            cartItemCount = state.cartQuantities.values.sum(),
                            cartTotal = appViewModel.cartSubtotal(),
                            favoriteProductIds = state.favoriteProductIds,
                            onToggleFavorite = appViewModel::toggleFavorite,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenCart = { backStack.add(GhostCartList) },
                            onOpenProduct = { id -> backStack.add(ProductDetail(id)) },
                            onAddToCart = appViewModel::addToCart,
                            onCoolProduct = { id, durationMillis, _ ->
                                appViewModel.quickGhostCatalogProduct(id, durationMillis)
                                backStack.add(Cooldowns)
                            }
                        )
                    }
                    entry<CaptureAlmostBuy> {
                        // Sharing product links (single or sequential) into the app is the same
                        // "add a product" flow as the manual Ghost + form - it's not a separate
                        // destination the user has to navigate to. When links are queued, this
                        // same screen shows the queue review instead of the single-item form.
                        if (state.shareQueue.isNotEmpty()) {
                            ShareQueueReviewScreen(
                                queue = state.shareQueue,
                                importState = state.productImportState,
                                onUpdateItem = appViewModel::updateShareQueueItem,
                                onRemoveItem = appViewModel::removeShareQueueItem,
                                onConfirmAll = { shareWithCommunity ->
                                    appViewModel.bulkConfirmShareQueue(shareWithCommunity)
                                    backStack.clear()
                                    backStack.add(GhostCartList)
                                },
                                onCoolAll = { shareWithCommunity, durationMillis ->
                                    appViewModel.bulkCoolShareQueue(shareWithCommunity, durationMillis)
                                    backStack.clear()
                                    backStack.add(Cooldowns)
                                },
                                onBack = {
                                    backStack.removeLastOrNull()
                                }
                            )
                        } else {
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
                                },
                                onAddListingToCart = { items ->
                                    appViewModel.addListingItemsToCart(items)
                                    appViewModel.clearCaptureSeed()
                                }
                            )
                        }
                    }
                    entry<Cooldowns> {
                        CooldownsScreen(
                            almostBuys = state.almostBuys,
                            onGhostSomething = { backStack.add(CategoryBrowse("all")) },
                            onResolve = { id, resolution -> appViewModel.resolveAlmostBuy(id, resolution) },
                            onMoreTime = appViewModel::extendAlmostBuy,
                            onShare = { item -> shareGhostItem(context, item.toGhostShareItem()) },
                            onOpenSource = { url -> openProductSource(context, url) }
                        )
                    }
                    entry<Progress> {
                        ProgressScreen(
                            almostBuys = state.almostBuys,
                            config = state.walletConfig,
                            onSetCardholderName = { name ->
                                appViewModel.updateWalletConfig { it.copy(cardholderName = name) }
                                appViewModel.showToast("Name updated")
                            },
                            onSelectCardTheme = { theme -> appViewModel.updateWalletConfig { it.copy(cardTheme = theme) } },
                            onDownloadCard = appViewModel::downloadGhostCard,
                            onAddBalance = appViewModel::addSimulatedWalletBalance
                        )
                    }
                    entry<ProductDetail> { key ->
                        val product = appViewModel.findProduct(key.productId)
                        if (product == null) {
                            LaunchedEffect(key.productId) { backStack.removeLastOrNull() }
                        } else {
                            ProductDetailScreen(
                                product = product,
                                coolingUntilMillis = state.coolingUntilByProductId[product.id],
                                isFavorite = product.id in state.favoriteProductIds,
                                ghostCount = product.ghostCount + (state.ghostCountsByProductId[product.id] ?: 0),
                                onBack = { backStack.removeLastOrNull() },
                                onShare = { shareGhostItem(context, product.toGhostShareItem()) },
                                onToggleFavorite = { appViewModel.toggleFavorite(product.id) },
                                hasSourceLink = !product.sourceUrl.isNullOrBlank(),
                                onAddToCart = { appViewModel.addToCart(product.id) },
                                onGhostBuyNow = {
                                    appViewModel.addToCart(product.id)
                                    backStack.add(GhostCartList)
                                },
                                onStartCooling = { durationMillis, durationLabel ->
                                    appViewModel.startCoolingPeriod(product.id, durationMillis, durationLabel)
                                    backStack.add(Cooldowns)
                                }
                            )
                        }
                    }
                    entry<GhostCartList> {
                        GhostCartListScreen(
                            products = appViewModel.cartProductsWithQuantities(),
                            coolingUntilByProductId = state.coolingUntilByProductId,
                            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() else backStack.add(Home) },
                            onAdd = appViewModel::addToCart,
                            onRemove = appViewModel::removeFromCart,
                            onClearAll = appViewModel::clearCart,
                            onStartCooling = { id, durationMillis, durationLabel ->
                                appViewModel.startCoolingPeriod(id, durationMillis, durationLabel)
                                backStack.add(Cooldowns)
                            },
                            onOpenProduct = { id -> backStack.add(ProductDetail(id)) },
                            onShareProduct = { product -> shareGhostItem(context, product.toGhostShareItem()) },
                            onCheckout = {
                                when {
                                    state.cartQuantities.isEmpty() -> appViewModel.showToast("Add an item before checkout")
                                    state.authEmail == null -> backStack.add(Auth)
                                    else -> backStack.add(GhostCheckout)
                                }
                            }
                        )
                    }
                    entry<GhostCheckout> {
                        GhostCheckoutScreen(
                            products = appViewModel.cartProductsWithQuantities(),
                            walletBalance = state.walletConfig.startingBalance,
                            simulationIntervalMinutes = state.simulationIntervalMinutes,
                            onSelectInterval = appViewModel::setSimulationInterval,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenWallet = { backStack.add(Progress) },
                            onPlaceOrder = { total ->
                                if (appViewModel.placeSimulatedOrder(total)) {
                                    backStack.add(OrderGhostedSuccess)
                                }
                            }
                        )
                    }
                    entry<OrderGhostedSuccess> {
                        OrderGhostedSuccessScreen(
                            orderId = state.lastOrderId,
                            amountAvoided = state.lastOrderTotal,
                            sourceProducts = state.lastOrderProducts,
                            onTrackDelivery = {
                                appViewModel.startDeliveryTracking()
                                backStack.add(FakeDeliveryTracking)
                            },
                            onViewSavings = {
                                backStack.clear()
                                backStack.add(Progress)
                            },
                            onOpenSource = { url -> openProductSource(context, url) }
                        )
                    }
                    entry<FakeDeliveryTracking> {
                        FakeDeliveryTrackingScreen(
                            orderId = state.lastOrderId,
                            amountSaved = state.lastOrderTotal,
                            deliveryStep = state.deliveryStep,
                            orderPlacedAtMillis = state.lastOrderPlacedAtMillis,
                            simulationIntervalMinutes = state.simulationIntervalMinutes,
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
                            onSelectAppTheme = appViewModel::setAppTheme,
                            onToggleCooling = {
                                appViewModel.updateWalletConfig { it.copy(coolingNotificationsEnabled = !it.coolingNotificationsEnabled) }
                            },
                            onToggleLunch = {
                                appViewModel.updateWalletConfig { it.copy(lunchReminderEnabled = !it.lunchReminderEnabled) }
                            },
                            onToggleDinner = {
                                appViewModel.updateWalletConfig { it.copy(dinnerReminderEnabled = !it.dinnerReminderEnabled) }
                            },
                            onOpenLegal = { docId -> backStack.add(LegalDocument(docId)) },
                            onDeleteAccount = {
                                appViewModel.deleteAccountAndLocalData()
                                backStack.clear()
                                backStack.add(Auth)
                            },
                            onSignOut = {
                                appViewModel.signOut()
                                backStack.clear()
                                backStack.add(Splash)
                            },
                            profile = state.profile,
                            profileSaving = state.profileSaving,
                            profileError = state.profileError,
                            onSaveDisplayName = appViewModel::updateDisplayName,
                            onUploadAvatar = appViewModel::uploadAvatar,
                            onSetCommunityOptIn = appViewModel::setCommunityLeaderboardOptIn,
                            onOpenLeaderboard = { backStack.add(Leaderboard) }
                        )
                    }
                    entry<LegalDocument> { key ->
                        LegalDocumentScreen(docId = key.docId, onBack = { backStack.removeLastOrNull() })
                    }
                    entry<Leaderboard> {
                        LaunchedEffect(Unit) {
                            appViewModel.refreshLeaderboard()
                            com.example.ghostcart.data.Analytics.logLeaderboardViewed(context)
                        }
                        com.example.ghostcart.ui.community.LeaderboardScreen(
                            entries = state.leaderboard,
                            loading = state.leaderboardLoading,
                            currentUsername = state.profile?.username,
                            onBack = { backStack.removeLastOrNull() }
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

        state.activeInAppMessage?.let { message ->
            InAppMessageDialog(
                message = message,
                onDismiss = { appViewModel.dismissInAppMessage(message.id) },
                onOpenLink = { url -> openProductSource(context, url) }
            )
        }

        openStoryIndex?.let { index ->
            com.example.ghostcart.ui.community.StoryViewer(
                stories = state.ghostCartStories,
                startIndex = index,
                onClose = { openStoryIndex = null }
            )
        }
    }
    }
    }
}

@Composable
private fun SplashContent() {
    Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GhostCartWordmark(modifier = Modifier.width(220.dp).height(72.dp), tint = Ink)
            Spacer(Modifier.height(12.dp))
            Text("For everything you almost bought.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private const val SPLASH_SKIP_BUTTON_DELAY_MS = 3_000L
private const val SPLASH_AUTO_ADVANCE_MS = 5_000L
private const val SPLASH_FALLBACK_MS = 1_200L

/**
 * Cold-start splash: picks one random Ghost Cart Story to show full-bleed
 * (the same admin-managed content the Stories rail uses), with a Skip button
 * that appears after 3s and an automatic advance at 5s regardless. Falls
 * back to the plain wordmark splash (old behavior, ~1.2s) if no story is
 * available yet - stories are fetched async on app start and may not have
 * arrived by the time this renders, especially on a cold/slow connection.
 */
@Composable
private fun RandomStorySplashScreen(stories: List<ContentBlockItem>, onFinished: () -> Unit) {
    // Image stories only - autoplaying a video (with audio) on cold start,
    // on top of the fixed 3s/5s skip timing below, isn't a good splash
    // experience. Video stories still play in full in the regular viewer.
    val imageStories = remember(stories) { stories.filter { it.mediaType != "video" } }
    val story = remember(imageStories.isNotEmpty()) { imageStories.randomOrNull() }
    var showSkip by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val finish = {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    if (story == null) {
        LaunchedEffect(Unit) {
            delay(SPLASH_FALLBACK_MS)
            finish()
        }
        SplashContent()
        return
    }

    LaunchedEffect(story) {
        delay(SPLASH_SKIP_BUTTON_DELAY_MS)
        showSkip = true
        delay(SPLASH_AUTO_ADVANCE_MS - SPLASH_SKIP_BUTTON_DELAY_MS)
        finish()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = story.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        AnimatedVisibility(
            visible = showSkip,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).navigationBarsPadding(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TextButton(
                onClick = { finish() },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Skip", fontWeight = FontWeight.Bold)
            }
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
            .background(DarkGray)
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
private fun GhostBottomNav(current: NavKey?, cartCount: Int = 0, onNavigate: (NavKey) -> Unit) {
    data class Item(val label: String, val destination: NavKey, val icon: androidx.compose.ui.graphics.vector.ImageVector, val central: Boolean = false)
    val items = listOf(
        Item(stringResource(R.string.nav_home), Home, Icons.Filled.Home),
        Item(stringResource(R.string.nav_cooldowns), Cooldowns, Icons.Filled.Timer),
        Item("Ghost Cart", GhostCartList, Icons.Filled.ShoppingCart, central = true),
        Item("Wallet", Progress, Icons.Filled.AccountBalanceWallet),
        Item(stringResource(R.string.nav_profile), GhostCardSettings, Icons.Filled.Person)
    )

    NavigationBar(containerColor = Paper, tonalElevation = 0.dp) {
        items.forEach { item ->
            val selected = current == item.destination
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.offset(y = 6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(if (item.central) 48.dp else 34.dp)
                                .clip(CircleShape)
                                .background(if (item.central) GhostGreen else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.central) {
                                GhostMascotPose(
                                    poseName = "cart",
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) GhostGreen else MutedText,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        if (item.central && cartCount > 0) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Ink)
                                    .border(2.dp, Paper, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (cartCount > 9) "9+" else "$cartCount",
                                    color = Paper,
                                    fontSize = 8.sp,
                                    lineHeight = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                },
                label = if (item.central) null else {
                    { Text(item.label, color = if (selected) Ink else MutedText, fontSize = 9.sp) }
                },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
            )
        }
    }
}
