package com.example.ghostcart

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.ghostcart.data.AlmostBuyResolution
import com.example.ghostcart.data.AlmostBuyStatus
import com.example.ghostcart.data.AlmostBuy
import com.example.ghostcart.data.GhostOrderResolution
import com.example.ghostcart.data.GhostDeliveryState
import com.example.ghostcart.data.GhostGiftDraft
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.openProductSource
import com.example.ghostcart.data.shareGhostItem
import com.example.ghostcart.data.Analytics
import com.example.ghostcart.data.toGhostShareItem
import com.example.ghostcart.data.TUTORIAL_PRODUCT_ID
import com.example.ghostcart.data.TutorialStatus
import com.example.ghostcart.data.TutorialStep
import com.example.ghostcart.data.tutorialPracticeProduct
import com.example.ghostcart.data.ghostDeliverySnapshot
import com.ghostcart.app.R
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.DarkGray
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.ExpressivePrimaryText
import com.example.ghostcart.theme.ExpressiveSecondaryText
import com.example.ghostcart.theme.ExpressiveSurfaceHigh
import com.example.ghostcart.theme.GhostGlass
import com.example.ghostcart.theme.GhostSubtleBorder
import com.example.ghostcart.theme.GhostCartTheme
import com.example.ghostcart.theme.GhostMorphShapes
import com.example.ghostcart.theme.GhostMotion
import com.example.ghostcart.theme.ghostMorphClip
import com.example.ghostcart.theme.rememberGhostMorph
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.GhostCartWordmark
import com.example.ghostcart.ui.app.AppViewModel
import com.example.ghostcart.ui.checkout.FakeDeliveryTrackingScreen
import com.example.ghostcart.ui.checkout.GhostCartListScreen
import com.example.ghostcart.ui.checkout.GhostCheckoutScreen
import com.example.ghostcart.ui.checkout.OrderGhostedSuccessScreen
import com.example.ghostcart.ui.delivery.GhostDeliveryTimeDialog
import com.example.ghostcart.ui.delivery.GhostDeliveryTrackerScreen
import com.example.ghostcart.ui.gifts.GhostGiftRevealScreen
import com.example.ghostcart.ui.gifts.GiftsScreen
import com.example.ghostcart.ui.onboarding.AuthScreen
import com.example.ghostcart.ui.onboarding.PersonalizationScreen
import com.example.ghostcart.ui.onboarding.ProfileSelectScreen
import com.example.ghostcart.ui.marketplace.ProductDetailScreen
import com.example.ghostcart.ui.marketplace.CategoryBrowseScreen
import com.example.ghostcart.ui.v2.CaptureAlmostBuyScreen
import com.example.ghostcart.ui.v2.CooldownsScreen
import com.example.ghostcart.ui.common.InAppMessageDialog
import com.example.ghostcart.ui.common.DEFAULT_GHOST_COOLDOWN_MILLIS
import com.example.ghostcart.ui.onboarding.SimulationConsentScreen
import com.example.ghostcart.ui.v2.GhostHomeScreen
import com.example.ghostcart.ui.v2.LegalDocumentScreen
import com.example.ghostcart.ui.v2.ProfileScreen
import com.example.ghostcart.ui.v2.ProgressScreen
import com.example.ghostcart.ui.v2.ShareQueueReviewScreen
import com.example.ghostcart.ui.v2.rememberNotificationPermissionRequest
import com.example.ghostcart.ui.tutorial.TutorialScreen
import com.example.ghostcart.ui.tutorial.TutorialGuideSpec
import com.example.ghostcart.ui.tutorial.TutorialViewModel
import kotlinx.coroutines.delay

private val onboardingDestinations: Set<NavKey> = setOf(Splash, Auth, ProfileSelect, Personalization, Tutorial)

private data class PendingGhostCheckout(
    val total: Int,
    val gift: GhostGiftDraft?,
    val deliveryAddress: String,
    val itemLabel: String,
    val category: String
)

private fun selectedBottomDestination(current: NavKey?): NavKey = when (current) {
    Cooldowns -> Cooldowns
    Progress -> Progress
    GhostCardSettings, WalletHome, WalletSetup, SalaryShield, Goals,
    WalletActivity, WeeklyStatement, Trends -> GhostCardSettings
    is LegalDocument, Gifts -> GhostCardSettings
    GhostCartList, CaptureAlmostBuy, GhostCheckout, OrderGhostedSuccess,
    FakeDeliveryTracking, PayWithGhostCard, OrderProtected -> GhostCartList
    is GhostDeliveryTracker -> Cooldowns
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
    ghostShareRequestKey: Long? = null,
    initialGiftToken: String? = null,
    giftRequestKey: Long? = null
) {
    val initial = when {
        initialGiftToken != null -> GhostGiftReveal(initialGiftToken)
        initialSharedUrl != null -> CaptureAlmostBuy
        initialGhostTitle != null || initialGhostShareId != null -> CaptureAlmostBuy
        initialCooldownId != null -> Cooldowns
        else -> Splash
    }
    val backStack = rememberNavBackStack(initial)
    val navSlideSpec = GhostMotion.offsetSpec()
    val navFadeSpec = GhostMotion.fadeSpec()
    val appViewModel: AppViewModel = viewModel()
    val tutorialViewModel: TutorialViewModel = viewModel()
    val context = LocalContext.current
    val tutorialProduct = remember(context.packageName) { tutorialPracticeProduct(context) }
    val state by appViewModel.uiState.collectAsState()
    val tutorialState by tutorialViewModel.state.collectAsState()
    val current = backStack.lastOrNull()
    val tutorialActive = tutorialState.status == TutorialStatus.IN_PROGRESS
    val showBottomNav = current != null && current !in onboardingDestinations && !tutorialActive
    var showTutorialExitDialog by remember { mutableStateOf(false) }
    var dismissedOrderId by remember { mutableStateOf<String?>(null) }
    var pendingGhostCheckout by remember { mutableStateOf<PendingGhostCheckout?>(null) }
    val requestDeliveryNotifications = rememberNotificationPermissionRequest()
    // Full-screen story viewer overlay state - lives here (not inside
    // GhostHomeScreen) so it renders above the bottom nav/Scaffold entirely,
    // matching how a real Stories viewer covers the whole screen.
    var openStoryIndex by remember { mutableStateOf<Int?>(null) }
    val activeBannerOrder = state.almostBuys
        .asSequence()
        .filter { it.status == AlmostBuyStatus.COOLING && !it.tutorialOnly }
        .map {
            it to ghostDeliverySnapshot(
                nowMillis = System.currentTimeMillis(),
                startMillis = it.deliveryStartedAtMillis,
                endMillis = it.deliveryEndsAtMillis,
                persistedState = it.deliveryState
            )
        }
        .minByOrNull { (item, _) -> item.deliveryEndsAtMillis }
    val showDeliveryBanner = current !is GhostDeliveryTracker &&
        activeBannerOrder != null &&
        dismissedOrderId != activeBannerOrder.first.id
    val darkTheme = when (state.appTheme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }
    val tutorialProductionRoute = (current == Home && tutorialState.currentStep == TutorialStep.PRODUCT) ||
        (current is ProductDetail && current.productId == TUTORIAL_PRODUCT_ID) ||
        current == GhostCartList || current == GhostCheckout
    BackHandler(enabled = tutorialActive && tutorialProductionRoute) {
        showTutorialExitDialog = true
    }

    LaunchedEffect(state.simulationConsentStatus?.accepted, state.simulationConsentStatus?.version) {
        val consent = state.simulationConsentStatus
        if (consent?.accepted == true) tutorialViewModel.recordConsentAccepted()
    }

    LaunchedEffect(initialCooldownId, state.almostBuys) {
        val target = initialCooldownId?.let { id ->
            state.almostBuys.firstOrNull { it.id == id || it.ghostOrderId == id }
        }
        if (target != null && backStack.lastOrNull() !is GhostDeliveryTracker) {
            com.example.ghostcart.data.Analytics.logNotificationOpened(context, "ghost_notification_opened")
            Analytics.logGhostOrderEvent(context, "ghost_notification_opened", target.ghostOrderId ?: target.id)
            backStack.add(GhostDeliveryTracker(target.id))
        }
    }

    // Firebase's automatic screen tracking is Activity-lifecycle-based, and
    // this whole app is one Activity - without this, every in-app
    // destination would be invisible to Firebase's engagement/time-on-screen
    // reporting. class simpleName is stable and readable enough to use
    // directly as the screen name (e.g. "Home", "ProductDetail").
    LaunchedEffect(current) {
        current?.let { destination ->
            Analytics.logScreenView(context, destination::class.simpleName ?: "Unknown")
            when (destination) {
                is GhostDeliveryTracker -> Analytics.logGhostOrderEvent(context, "tracker_opened", destination.itemId)
                Progress -> Analytics.logGhostOrderEvent(context, "wallet_opened")
                Leaderboard -> Analytics.logGhostOrderEvent(context, "leaderboard_opened")
                else -> Unit
            }
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

    LaunchedEffect(giftRequestKey) {
        if (initialGiftToken != null) {
            val destination = GhostGiftReveal(initialGiftToken)
            if (backStack.lastOrNull() != destination) backStack.add(destination)
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
            onAccept = {
                appViewModel.acceptSimulationConsent()
            }
        )
    } else {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Paper,
            bottomBar = {
                if (showBottomNav) {
                    Column {
                        if (showDeliveryBanner) {
                            val (order, snapshot) = activeBannerOrder
                            DeliveryTrackingBanner(
                                order = order,
                                state = snapshot.state,
                                onClick = { backStack.add(GhostDeliveryTracker(order.id)) },
                                onClose = { dismissedOrderId = order.id }
                            )
                        }
                        GhostBottomNav(
                            current = selectedBottomDestination(current),
                            cartItemCount = state.cartQuantities.values.sum(),
                            onNavigate = { destination ->
                                if (backStack.lastOrNull() != destination) backStack.add(destination)
                            }
                        )
                    }
                }
            }
        ) { insets ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(insets),
                onBack = { backStack.removeLastOrNull() },
                // Directional shared-axis: forward navigation slides the new screen in from the
                // end edge (with the outgoing screen sliding + fading out toward the start edge),
                // back navigation reverses it - so forward/backward always read as opposite
                // directions instead of the previous default (an undirected crossfade), which is
                // what made back-navigation feel disorienting. slideIntoContainer/
                // slideOutOfContainer (not manual offsets) so this is correct in RTL layouts too.
                // Both specs come from GhostMotion, so this - like everything else in the M3E
                // migration - falls back to an instant snap under reduced-motion.
                transitionSpec = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = navSlideSpec,
                    ) + fadeIn(animationSpec = navFadeSpec) togetherWith
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = navSlideSpec,
                        ) + fadeOut(animationSpec = navFadeSpec)
                },
                popTransitionSpec = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = navSlideSpec,
                    ) + fadeIn(animationSpec = navFadeSpec) togetherWith
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = navSlideSpec,
                        ) + fadeOut(animationSpec = navFadeSpec)
                },
                predictivePopTransitionSpec = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = navSlideSpec,
                    ) + fadeIn(animationSpec = navFadeSpec) togetherWith
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = navSlideSpec,
                        ) + fadeOut(animationSpec = navFadeSpec)
                },
                entryProvider = entryProvider {
                    entry<Splash> {
                        OfficialBrandSplashScreen(
                            onFinished = {
                                backStack.clear()
                                if (tutorialViewModel.shouldAutoLaunch()) {
                                    tutorialViewModel.startIfNeeded()
                                    backStack.add(Tutorial)
                                } else {
                                    backStack.add(if (state.authEmail == null) Auth else Home)
                                }
                            }
                        )
                    }
                    entry<Tutorial> {
                        val productionStep = tutorialState.currentStep in setOf(
                            TutorialStep.PRODUCT,
                            TutorialStep.CART,
                            TutorialStep.COOLDOWN,
                            TutorialStep.FAKE_CHECKOUT
                        )
                        LaunchedEffect(tutorialState.currentStep) {
                            val destination: NavKey? = when (tutorialState.currentStep) {
                                TutorialStep.PRODUCT -> Home
                                TutorialStep.CART, TutorialStep.COOLDOWN -> GhostCartList
                                TutorialStep.FAKE_CHECKOUT -> GhostCheckout
                                else -> null
                            }
                            if (destination != null && backStack.lastOrNull() != destination) {
                                backStack.add(destination)
                            }
                        }
                        if (productionStep) {
                            Box(Modifier.fillMaxSize().background(Paper))
                        } else {
                            TutorialScreen(
                                state = tutorialState,
                                onContinueWelcome = tutorialViewModel::continueFromWelcome,
                                onSkip = {
                                    tutorialViewModel.skip()
                                    backStack.clear()
                                    backStack.add(Home)
                                },
                                onOpenPracticeProduct = {
                                    tutorialViewModel.openPracticeProduct()
                                    backStack.clear()
                                    backStack.add(Home)
                                },
                                onAddToCart = tutorialViewModel::addPracticeItemToCart,
                                onOpenCooldown = tutorialViewModel::openCooldownPicker,
                                onSelectTutorialCooldown = tutorialViewModel::selectTutorialCooldown,
                                onContinueToCheckout = tutorialViewModel::continueToFakeCheckout,
                                onCompleteFakeCheckout = tutorialViewModel::completeFakeCheckout,
                                onFinishCooling = tutorialViewModel::finishCooling,
                                onChooseDecision = tutorialViewModel::chooseDecision,
                                onContinueFromReceipt = tutorialViewModel::continueFromReceipt,
                                onDeliveryFinished = {
                                    tutorialViewModel.complete()
                                    backStack.clear()
                                    backStack.add(Home)
                                },
                                onReplay = tutorialViewModel::replay,
                                onExit = {
                                    tutorialViewModel.skip(exitDuringTutorial = true)
                                    backStack.clear()
                                    backStack.add(Home)
                                }
                            )
                        }
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
                        val tutorialMarketplace = tutorialActive && tutorialState.currentStep == TutorialStep.PRODUCT
                        GhostHomeScreen(
                            items = state.almostBuys,
                            unifiedProducts = if (tutorialMarketplace) {
                                listOf(tutorialProduct) + appViewModel.unifiedMarketplaceProducts().filterNot { it.id == TUTORIAL_PRODUCT_ID }
                            } else appViewModel.unifiedMarketplaceProducts(),
                            favoriteProducts = state.favoriteProductIds.mapNotNull(appViewModel::findProduct),
                            favoriteProductIds = state.favoriteProductIds,
                            communityProductsLoading = state.communityProductsLoading,
                            onGhostSomething = {
                                appViewModel.clearCaptureSeed()
                                backStack.add(CaptureAlmostBuy)
                            },
                            onOpenCooldowns = { backStack.add(Cooldowns) },
                            onTrackDelivery = { id -> backStack.add(GhostDeliveryTracker(id)) },
                            onOpenProgress = { backStack.add(Progress) },
                            onGhost = appViewModel::addToCart,
                            onOpen = { id -> backStack.add(ProductDetail(id)) },
                            onToggleFavorite = appViewModel::toggleFavorite,
                            onShareProduct = { product -> shareGhostItem(context, product.toGhostShareItem()) },
                            onNotifications = { backStack.add(Notifications) },
                            hasUnreadNotifications = state.hasUnreadNotifications,
                            onViewAllCatalog = { categoryId -> backStack.add(CategoryBrowse(categoryId)) },
                            onViewAllFavorites = { backStack.add(CategoryBrowse("favorites")) },
                            onNotificationsGranted = appViewModel::enableMealRemindersByDefault,
                            onRefresh = {
                                appViewModel.refreshCommunityProducts()
                                appViewModel.refreshCatalogProducts()
                                appViewModel.refreshHomeBanners()
                                appViewModel.refreshGhostCartStories()
                            },
                            homeBanners = state.homeBanners,
                            ghostCartStories = state.ghostCartStories,
                            onOpenLeaderboard = { backStack.add(Leaderboard) },
                            onOpenStory = { index -> openStoryIndex = index },
                            tutorialProductId = TUTORIAL_PRODUCT_ID.takeIf { tutorialMarketplace },
                            tutorialSpotlightStep = tutorialState.marketplaceSpotlightStep,
                            onTutorialAdvance = tutorialViewModel::advanceMarketplaceSpotlight,
                            onTutorialGhost = {
                                tutorialViewModel.addPracticeItemToCart()
                                tutorialViewModel.openTutorialCart()
                                backStack.add(GhostCartList)
                            }
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
                            favoriteProductIds = state.favoriteProductIds,
                            onToggleFavorite = appViewModel::toggleFavorite,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenProduct = { id -> backStack.add(ProductDetail(id)) },
                            onGhostProduct = appViewModel::addToCart,
                            onShareProduct = { product -> shareGhostItem(context, product.toGhostShareItem()) },
                            onReviews = { id ->
                                Analytics.logGhostOrderEvent(context, "product_reviews_opened")
                                backStack.add(ProductDetail(id))
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
                                onGhostAll = { shareWithCommunity ->
                                    appViewModel.bulkCoolShareQueue(shareWithCommunity, DEFAULT_GHOST_COOLDOWN_MILLIS)
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
                                onGhostIt = { draft ->
                                    appViewModel.addDraftToCart(draft)
                                    appViewModel.clearCaptureSeed()
                                    backStack.removeLastOrNull()
                                    backStack.add(GhostCartList)
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
                            onOpenSource = { url -> openProductSource(context, url) },
                            onTrack = { id -> backStack.add(GhostDeliveryTracker(id)) }
                        )
                    }
                    entry<GhostDeliveryTracker> { key ->
                        val order = state.almostBuys.firstOrNull { it.id == key.itemId || it.ghostOrderId == key.itemId }
                        if (order == null) {
                            LaunchedEffect(key.itemId) {
                                backStack.removeLastOrNull()
                                if (backStack.lastOrNull() != Cooldowns) backStack.add(Cooldowns)
                            }
                        } else {
                            GhostDeliveryTrackerScreen(
                                item = order,
                                onBack = { backStack.removeLastOrNull() },
                                onResolve = { resolution -> appViewModel.resolveGhostDelivery(order.id, resolution) },
                                onRestart = { duration -> appViewModel.extendAlmostBuy(order.id, duration) },
                                onOpenSource = { url -> openProductSource(context, url) },
                                onShare = { shareGhostItem(context, order.toGhostShareItem()) }
                            )
                        }
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
                        // Keep the tutorial product backed by the isolated local session for as
                        // long as this destination remains on the back stack. The tutorial step
                        // advances before navigation to the cart, so tying this lookup strictly
                        // to PRODUCT can briefly make the item disappear and pop the destination
                        // that was just opened.
                        val tutorialProductScreen = tutorialActive &&
                            key.productId == TUTORIAL_PRODUCT_ID
                        val product = if (tutorialProductScreen) tutorialProduct else appViewModel.findProduct(key.productId)
                        if (product == null) {
                            LaunchedEffect(key.productId) { backStack.removeLastOrNull() }
                        } else {
                            ProductDetailScreen(
                                product = product,
                                coolingUntilMillis = if (tutorialProductScreen) null else state.coolingUntilByProductId[product.id],
                                isFavorite = !tutorialProductScreen && product.id in state.favoriteProductIds,
                                ghostCount = if (tutorialProductScreen) 0 else product.ghostCount + (state.ghostCountsByProductId[product.id] ?: 0),
                                onBack = { if (tutorialProductScreen) showTutorialExitDialog = true else backStack.removeLastOrNull() },
                                onShare = { if (!tutorialProductScreen) shareGhostItem(context, product.toGhostShareItem()) },
                                onToggleFavorite = { if (!tutorialProductScreen) appViewModel.toggleFavorite(product.id) },
                                onGhost = {
                                    if (tutorialProductScreen) tutorialViewModel.addPracticeItemToCart()
                                    else appViewModel.addToCart(product.id)
                                },
                                isInCart = if (tutorialProductScreen) tutorialState.practiceItemInCart else product.id in state.cartQuantities,
                                onOpenCart = {
                                    if (tutorialProductScreen) {
                                        tutorialViewModel.openTutorialCart()
                                        backStack.add(GhostCartList)
                                    } else backStack.add(GhostCartList)
                                },
                                onOpenCooldown = { if (!tutorialProductScreen) backStack.add(Cooldowns) },
                                tutorialGuide = if (tutorialProductScreen) {
                                    TutorialGuideSpec(
                                        message = if (tutorialState.practiceItemInCart) {
                                            "Great. Open your real Ghost Cart to continue."
                                        } else {
                                            "Start by adding the item to your Ghost Cart."
                                        },
                                        stepLabel = if (tutorialState.practiceItemInCart) "STEP 2 OF 4" else "STEP 1 OF 4"
                                    )
                                } else null
                            )
                        }
                    }
                    entry<GhostCartList> {
                        val tutorialCartScreen = tutorialActive && tutorialState.currentStep in setOf(
                            TutorialStep.CART,
                            TutorialStep.COOLDOWN
                        )
                        val cartProducts = if (tutorialCartScreen) listOf(tutorialProduct to 1) else appViewModel.cartProductsWithQuantities()
                        GhostCartListScreen(
                            products = cartProducts,
                            onBack = { if (tutorialCartScreen) showTutorialExitDialog = true else if (backStack.size > 1) backStack.removeLastOrNull() else backStack.add(Home) },
                            onAdd = { id -> if (!tutorialCartScreen) appViewModel.addToCart(id) },
                            onRemove = { id -> if (!tutorialCartScreen) appViewModel.removeFromCart(id) },
                            onClearAll = { if (!tutorialCartScreen) appViewModel.clearCart() },
                            onOpenProduct = { id -> if (!tutorialCartScreen) backStack.add(ProductDetail(id)) },
                            onShareProduct = { product -> if (!tutorialCartScreen) shareGhostItem(context, product.toGhostShareItem()) },
                            onCheckout = {
                                if (tutorialCartScreen) {
                                    tutorialViewModel.openCooldownPicker()
                                } else {
                                    when {
                                        state.cartQuantities.isEmpty() -> appViewModel.showToast("Add an item before checkout")
                                        state.authEmail == null -> backStack.add(Auth)
                                        else -> backStack.add(GhostCheckout)
                                    }
                                }
                            },
                            tutorialGuide = if (tutorialCartScreen && tutorialState.currentStep == TutorialStep.CART) {
                                TutorialGuideSpec(
                                    message = "This is your simulated cart. Continue to choose a short practice cooldown.",
                                    stepLabel = "STEP 3 OF 4"
                                )
                            } else null,
                            tutorialCooldownMode = tutorialCartScreen && tutorialState.currentStep == TutorialStep.COOLDOWN,
                            onTutorialCooldownSelected = {
                                tutorialViewModel.selectTutorialCooldown()
                                tutorialViewModel.continueToFakeCheckout()
                                backStack.add(GhostCheckout)
                            },
                            onTutorialCooldownDismiss = tutorialViewModel::returnToTutorialCart
                        )
                    }
                    entry<GhostCheckout> {
                        val tutorialCheckoutScreen = tutorialActive && tutorialState.currentStep == TutorialStep.FAKE_CHECKOUT
                        GhostCheckoutScreen(
                            products = if (tutorialCheckoutScreen) listOf(tutorialProduct to 1) else appViewModel.cartProductsWithQuantities(),
                            walletBalance = if (tutorialCheckoutScreen) 10_000 else state.walletConfig.startingBalance,
                            onBack = { if (tutorialCheckoutScreen) showTutorialExitDialog = true else backStack.removeLastOrNull() },
                            onOpenWallet = { if (!tutorialCheckoutScreen) backStack.add(Progress) },
                            onPlaceOrder = { total, ghostGift, deliveryAddress ->
                                if (tutorialCheckoutScreen) {
                                    tutorialViewModel.completeFakeCheckout()
                                    backStack.clear()
                                    backStack.add(Tutorial)
                                } else {
                                    val checkoutProducts = appViewModel.cartProductsWithQuantities()
                                    val distinctProducts = checkoutProducts.map { it.first }.distinctBy { it.id }
                                    val allFood = distinctProducts.isNotEmpty() && distinctProducts.all { product ->
                                        product.category.contains("food", ignoreCase = true) ||
                                            product.category.contains("coffee", ignoreCase = true) ||
                                            product.category.contains("delivery", ignoreCase = true)
                                    }
                                    pendingGhostCheckout = PendingGhostCheckout(
                                        total = total,
                                        gift = ghostGift,
                                        deliveryAddress = deliveryAddress,
                                        itemLabel = distinctProducts.singleOrNull()?.name
                                            ?: "${distinctProducts.size} Ghost Cart items",
                                        category = if (allFood) "Food & delivery" else "Marketplace"
                                    )
                                }
                            },
                            tutorialMode = tutorialCheckoutScreen,
                            primaryButtonLabel = "Complete Fake Checkout".takeIf { tutorialCheckoutScreen },
                            tutorialGuide = TutorialGuideSpec(
                                message = "Complete the checkout feeling without spending real money.",
                                stepLabel = "STEP 4 OF 4"
                            ).takeIf { tutorialCheckoutScreen }
                        )
                    }
                    entry<GhostGiftReveal> { key ->
                        GhostGiftRevealScreen(
                            token = key.token,
                            onBack = {
                                if (backStack.size > 1) backStack.removeLastOrNull()
                                else backStack.add(Home)
                            },
                            onGhostGift = { gift ->
                                appViewModel.ghostRevealedGift(gift) {
                                    backStack.add(Cooldowns)
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
                                backStack.add(GhostDeliveryTracker(state.lastOrderId))
                            },
                            onViewSavings = {
                                backStack.clear()
                                backStack.add(Progress)
                            },
                            onOpenSource = { url -> openProductSource(context, url) },
                            placedAtMillis = state.lastOrderPlacedAtMillis,
                            deliveryAddress = state.lastOrderDeliveryAddress,
                            invoiceItems = state.lastOrderItemsWithQty,
                            subtotal = state.lastOrderSubtotal,
                            promoDiscount = state.lastOrderPromoDiscount,
                            serviceFee = state.lastOrderServiceFee,
                            vat = state.lastOrderVat,
                            onDownloadInvoice = appViewModel::downloadInvoice,
                            onShareInvoice = appViewModel::shareInvoice
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
                                tutorialViewModel.onUserSignedOut()
                                appViewModel.signOut()
                                backStack.clear()
                                backStack.add(Splash)
                            },
                            profile = state.profile,
                            profileSaving = state.profileSaving,
                            profileError = state.profileError,
                            onSaveDisplayName = appViewModel::updateDisplayName,
                            onUploadAvatar = appViewModel::uploadAvatar,
                            onSelectAvatarPreset = appViewModel::selectAvatarPreset,
                            onSetCommunityOptIn = appViewModel::setCommunityLeaderboardOptIn,
                            onSetShowRecentActivityPublicly = appViewModel::setShowRecentActivityPublicly,
                            onOpenLeaderboard = { backStack.add(Leaderboard) },
                            onReplayTutorial = {
                                tutorialViewModel.replay()
                                backStack.add(Tutorial)
                            },
                            tutorialDebugState = "${tutorialState.status.name} · ${tutorialState.currentStep.name}",
                            onResetTutorialDebug = tutorialViewModel::resetForDebug,
                            onClearTutorialSessionDebug = tutorialViewModel::clearTutorialSession,
                            onStartTutorialStepDebug = { step ->
                                tutorialViewModel.startAtForDebug(step)
                                backStack.add(Tutorial)
                            },
                            onOpenGifts = { backStack.add(Gifts) }
                        )
                    }
                    entry<Gifts> {
                        GiftsScreen(onBack = { backStack.removeLastOrNull() })
                    }
                    entry<Notifications> {
                        com.example.ghostcart.ui.notifications.NotificationsScreen(
                            notifications = state.notifications,
                            loading = state.notificationsLoading,
                            onBack = { backStack.removeLastOrNull() },
                            onOpen = { appViewModel.markNotificationsSeen() }
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
                            onBack = { backStack.removeLastOrNull() },
                            onOpenDetail = { username -> backStack.add(LeaderboardDetail(username)) }
                        )
                    }
                    entry<LeaderboardDetail> { key ->
                        LaunchedEffect(key.username) { appViewModel.openLeaderboardDetail(key.username) }
                        com.example.ghostcart.ui.community.LeaderboardDetailScreen(
                            detail = state.leaderboardDetail?.takeIf { it.username == key.username },
                            loading = state.leaderboardDetailLoading,
                            isYou = state.profile?.username == key.username,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
            )
        }

        pendingGhostCheckout?.let { checkout ->
            GhostDeliveryTimeDialog(
                productName = checkout.itemLabel,
                category = checkout.category,
                onDismiss = { pendingGhostCheckout = null },
                onConfirm = { duration ->
                    pendingGhostCheckout = null
                    if (appViewModel.placeSimulatedOrder(
                            checkoutTotal = checkout.total,
                            ghostGift = checkout.gift,
                            deliveryAddress = checkout.deliveryAddress,
                            deliveryDurationMillis = duration
                        )
                    ) {
                        requestDeliveryNotifications()
                        backStack.add(OrderGhostedSuccess)
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


        if (showTutorialExitDialog) {
            AlertDialog(
                onDismissRequest = { showTutorialExitDialog = false },
                title = { Text("Leave the tutorial?") },
                text = { Text("The practice coffee and donut will disappear, but you can replay the tutorial later from Profile.") },
                confirmButton = {
                    TextButton(onClick = { showTutorialExitDialog = false }) {
                        Text("Continue tutorial")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showTutorialExitDialog = false
                        tutorialViewModel.skip(exitDuringTutorial = true)
                        backStack.clear()
                        backStack.add(Home)
                    }) {
                        Text("Exit tutorial")
                    }
                }
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
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.ghost_cart_splash_reference),
            contentDescription = "GhostCart. For everything you almost bought",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private const val BRAND_SPLASH_DURATION_MS = 1_200L

@Composable
private fun OfficialBrandSplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(BRAND_SPLASH_DURATION_MS)
        onFinished()
    }
    SplashContent()
}

@Composable
private fun DeliveryTrackingBanner(order: AlmostBuy, state: GhostDeliveryState, onClick: () -> Unit, onClose: () -> Unit) {
    val status = when (state) {
        GhostDeliveryState.PLACED -> "Ghost Order placed"
        GhostDeliveryState.PREPARING -> "Being prepared"
        GhostDeliveryState.RIDER_PICKING_UP -> "Ghost Rider picking up"
        GhostDeliveryState.OUT_FOR_DELIVERY -> "Out for Ghost Delivery"
        GhostDeliveryState.RIDER_NEARBY -> "Ghost Rider nearby"
        else -> "Ghost Delivery"
    }
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
            Text(text = "YOUR GHOST DELIVERY · SIMULATION", color = GhostGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(text = status, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = order.name, color = Color.White.copy(alpha = 0.68f), fontSize = 9.sp, maxLines = 1)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.68f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun GhostBottomNav(current: NavKey?, cartItemCount: Int = 0, onNavigate: (NavKey) -> Unit) {
    data class Item(val label: String, val destination: NavKey, val icon: androidx.compose.ui.graphics.vector.ImageVector, val central: Boolean = false)
    val items = listOf(
        Item(stringResource(R.string.nav_home), Home, Icons.Filled.Home),
        Item(stringResource(R.string.nav_cooldowns), Cooldowns, Icons.Filled.Timer),
        Item("Cart", GhostCartList, Icons.Filled.ShoppingCart, central = true),
        Item("Wallet", Progress, Icons.Filled.AccountBalanceWallet),
        Item(stringResource(R.string.nav_profile), GhostCardSettings, Icons.Filled.Person)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(32.dp),
        color = GhostGlass,
        contentColor = ExpressivePrimaryText,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, GhostSubtleBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            items.forEach { item ->
                val selected = current == item.destination
                val indicatorColor by animateColorAsState(
                    targetValue = when {
                        item.central -> GhostGreen
                        selected -> GhostGreen
                        else -> Color.Transparent
                    },
                    animationSpec = GhostMotion.colorSpec(),
                    label = "navIndicator"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (selected) Color(0xFF071006) else ExpressiveSecondaryText,
                    animationSpec = GhostMotion.colorSpec(),
                    label = "navIconColor"
                )
                val indicatorWidth by animateDpAsState(
                    targetValue = if (selected || item.central) 52.dp else 40.dp,
                    animationSpec = GhostMotion.sizeSpec(),
                    label = "navIndicatorWidth"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.08f else 1f,
                    animationSpec = GhostMotion.popSpec(),
                    label = "navIconScale"
                )
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val morph = rememberGhostMorph(GhostMorphShapes.circle, GhostMorphShapes.blob)
                val morphSpec = GhostMotion.morphSpec()
                val morphProgressAnimatable = remember { Animatable(0f) }

                LaunchedEffect(isPressed) {
                    if (item.central) {
                        morphProgressAnimatable.animateTo(if (isPressed) 1f else 0f, animationSpec = morphSpec)
                    }
                }

                if (item.central) {
                    var previousCartCount by remember { mutableStateOf(cartItemCount) }
                    LaunchedEffect(cartItemCount) {
                        // One-shot circle->blob->circle pulse when the cart goes from empty to
                        // non-empty, skipped while the button is actively being pressed so it
                        // doesn't fight the press-driven morph above.
                        if (previousCartCount == 0 && cartItemCount > 0 && !isPressed) {
                            morphProgressAnimatable.animateTo(1f, animationSpec = morphSpec)
                            morphProgressAnimatable.animateTo(0f, animationSpec = morphSpec)
                        }
                        previousCartCount = cartItemCount
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            role = Role.Tab
                        ) { onNavigate(item.destination) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(
                            modifier = Modifier
                                .width(indicatorWidth)
                                .height(if (item.central) 46.dp else 34.dp)
                                .let {
                                    if (item.central) {
                                        it.ghostMorphClip(morph) { morphProgressAnimatable.value }
                                    } else {
                                        it.clip(RoundedCornerShape(18.dp))
                                    }
                                }
                                .background(indicatorColor)
                                .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.central) {
                                GhostMascotPose(
                                    poseName = "cart",
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }
                        if (item.central && cartItemCount > 0) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 4.dp, y = (-2).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE4342F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (cartItemCount > 9) "9+" else "$cartItemCount",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (!item.central) {
                        Text(
                            item.label,
                            color = if (selected) ExpressivePrimaryText else ExpressiveSecondaryText,
                            fontSize = 9.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
