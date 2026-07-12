package com.example.ghostcart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import kotlinx.coroutines.delay
import com.example.ghostcart.ui.onboarding.AuthScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.WalletDemoData
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.ui.app.AppViewModel
import com.example.ghostcart.ui.checkout.FakeDeliveryTrackingScreen
import com.example.ghostcart.ui.checkout.GhostCartListScreen
import com.example.ghostcart.ui.checkout.GhostCheckoutScreen
import com.example.ghostcart.ui.checkout.OrderGhostedSuccessScreen
import com.example.ghostcart.ui.checkout.OrderProtectedScreen
import com.example.ghostcart.ui.checkout.PayWithGhostCardScreen
import com.example.ghostcart.ui.common.materialIconFor
import com.example.ghostcart.ui.main.MainScreen
import com.example.ghostcart.ui.marketplace.CategoryBrowseScreen
import com.example.ghostcart.ui.marketplace.HomeMarketplaceScreen
import com.example.ghostcart.ui.marketplace.ProductDetailScreen
import com.example.ghostcart.ui.onboarding.PersonalizationScreen
import com.example.ghostcart.ui.onboarding.ProfileSelectScreen
import com.example.ghostcart.ui.onboarding.SplashScreen
import com.example.ghostcart.ui.wallet.GhostCardSettingsScreen
import com.example.ghostcart.ui.wallet.GoalsScreen
import com.example.ghostcart.ui.wallet.SalaryShieldScreen
import com.example.ghostcart.ui.wallet.TrendsScreen
import com.example.ghostcart.ui.wallet.WalletActivityScreen
import com.example.ghostcart.ui.wallet.WalletHomeScreen
import com.example.ghostcart.ui.wallet.WalletSetupScreen
import com.example.ghostcart.ui.wallet.WeeklyStatementScreen

private val BOTTOM_NAV_DESTINATIONS: Set<NavKey> = setOf(Home, GhostCartList, WalletHome, Trends, GhostCardSettings)

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Splash)
  val appViewModel: AppViewModel = viewModel()
  val current = backStack.lastOrNull()
  val showBottomNav = current != null && current in BOTTOM_NAV_DESTINATIONS

  val state by appViewModel.uiState.collectAsState()

  androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      containerColor = Paper,
    bottomBar = {
      if (showBottomNav) {
        GhostBottomNav(
          current = current,
          onNavigate = { destination ->
            if (backStack.lastOrNull() != destination) {
              backStack.add(destination)
            }
          }
        )
      }
    }
  ) { padding ->
    NavDisplay(
      backStack = backStack,
      modifier = Modifier.padding(padding),
      onBack = { backStack.removeLastOrNull() },
      entryProvider =
        entryProvider {
          entry<Main> {
            MainScreen(modifier = Modifier.safeDrawingPadding())
          }

          // Onboarding
          entry<Splash> {
            val state by appViewModel.uiState.collectAsState()
            LaunchedEffect(state.authEmail) {
              delay(2000) // Show logo for 2 seconds
              backStack.clear()
              if (state.authEmail != null) {
                backStack.add(Home)
              } else {
                backStack.add(Auth)
              }
            }
            androidx.compose.foundation.layout.Box(
              modifier = Modifier.fillMaxSize().background(Paper),
              contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
              androidx.compose.foundation.layout.Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
              ) {
                com.example.ghostcart.ui.GhostMascotPose(poseName = "wave", modifier = Modifier.size(100.dp))
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                Text(
                  text = "Ghost Cart",
                  color = Ink,
                  fontSize = 32.sp,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                )
                Text(
                  text = "✦ UAE's #1 Simulated Cart ✦",
                  color = GhostGreen,
                  fontSize = 12.sp,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                  modifier = Modifier.padding(top = 8.dp)
                )
              }
            }
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
            val state by appViewModel.uiState.collectAsState()
            ProfileSelectScreen(
              selectedProfile = state.selectedProfile,
              onSelectProfile = { appViewModel.selectProfile(it) },
              onContinue = { backStack.add(Personalization) },
              onSkip = {
                backStack.clear()
                backStack.add(Home)
              }
            )
          }
          entry<Personalization> {
            val state by appViewModel.uiState.collectAsState()
            PersonalizationScreen(
              selectedCategoryIds = state.selectedOverspendIds,
              onToggleCategory = { appViewModel.toggleOverspendCategory(it) },
              selectedSavingsGoal = state.selectedSavingsGoal,
              onSelectSavingsGoal = { appViewModel.selectSavingsGoal(it) },
              onContinue = { backStack.add(WalletSetup) }
            )
          }

          // Marketplace
          entry<Home> {
            HomeMarketplaceScreen(
              onOpenCart = { backStack.add(GhostCartList) },
              onOpenWallet = { backStack.add(WalletHome) },
              onOpenTrends = { backStack.add(Trends) },
              onOpenCategory = { id -> backStack.add(CategoryBrowse(id)) },
              onOpenProduct = { id -> backStack.add(ProductDetail(id)) },
              onAddToCart = { id -> appViewModel.addToCart(id) }
            )
          }
          entry<CategoryBrowse> { key ->
            val state by appViewModel.uiState.collectAsState()
            val categoryProducts = appViewModel.allProducts.filter { prod ->
              when (key.categoryId) {
                "food" -> prod.category.contains("Food", true) || prod.category.contains("Fast", true)
                "beauty" -> prod.category.contains("Beauty", true)
                "fashion" -> prod.category.contains("Fashion", true)
                "gadgets" -> prod.category.contains("Gadgets", true) || prod.category.contains("Tech", true)
                "most_ghosted" -> Marketplace.mostGhostedToday.any { it.id == prod.id } || (prod.id.startsWith("dummy") && prod.price > 200)
                "flash_deals" -> Marketplace.fakeFlashDeals.any { it.id == prod.id } || (prod.id.startsWith("dummy") && prod.price < 100)
                "all" -> true
                else -> true
              }
            }
            CategoryBrowseScreen(
              categoryId = key.categoryId,
              products = categoryProducts,
              cartItemCount = state.cartProductIds.size,
              cartTotal = appViewModel.cartSubtotal(),
              savedTotal = Marketplace.savedThisWeekAcaiTotal,
              onBack = { backStack.removeLastOrNull() },
              onOpenProduct = { id -> backStack.add(ProductDetail(id)) },
              onAddToCart = { id -> appViewModel.addToCart(id) }
            )
          }
          entry<ProductDetail> { key ->
            val product = appViewModel.findProduct(key.productId) ?: appViewModel.allProducts.first()
            ProductDetailScreen(
              product = product,
              onBack = { backStack.removeLastOrNull() },
              onAddToCart = { appViewModel.addToCart(product.id) },
              onGhostBuyNow = {
                appViewModel.addToCart(product.id)
                appViewModel.placeSimulatedOrder()
                backStack.add(OrderGhostedSuccess)
              }
            )
          }
          entry<GhostCartList> {
            val state by appViewModel.uiState.collectAsState()
            GhostCartListScreen(
              products = appViewModel.cartProductsWithQuantities(),
              onBack = { backStack.removeLastOrNull() },
              onAdd = { appViewModel.addToCart(it) },
              onRemove = { appViewModel.removeFromCart(it) },
              onClearAll = { appViewModel.clearCart() },
              onCheckout = { backStack.add(GhostCheckout) }
            )
          }
          entry<GhostCheckout> {
            val state by appViewModel.uiState.collectAsState()
            GhostCheckoutScreen(
              products = appViewModel.cartProductsWithQuantities(),
              walletBalance = WalletDemoData.currentBalance,
              simulationIntervalMinutes = state.simulationIntervalMinutes,
              onSelectInterval = { appViewModel.setSimulationInterval(it) },
              onBack = { backStack.removeLastOrNull() },
              onPlaceOrder = {
                appViewModel.placeSimulatedOrder()
                backStack.add(OrderGhostedSuccess)
              }
            )
          }
          entry<OrderGhostedSuccess> {
            val state by appViewModel.uiState.collectAsState()
            OrderGhostedSuccessScreen(
              orderId = state.lastOrderId.ifBlank { "GHOST-00000" },
              amountAvoided = state.lastOrderTotal,
              onTrackDelivery = {
                appViewModel.startDeliveryTracking()
                backStack.add(FakeDeliveryTracking)
              },
              onViewSavings = { backStack.add(WalletHome) }
            )
          }
          entry<FakeDeliveryTracking> {
            val state by appViewModel.uiState.collectAsState()
            FakeDeliveryTrackingScreen(
              orderId = state.lastOrderId.ifBlank { "GHOST-00000" },
              amountSaved = state.lastOrderTotal,
              deliveryStep = state.deliveryStep,
              onViewReceipt = {
                appViewModel.resetDeliveryTracking()
                backStack.clear()
                backStack.add(Home)
              }
            )
          }
          entry<PayWithGhostCard> {
            val product = appViewModel.allProducts.first { it.id == "perfumeBlind" }
            PayWithGhostCardScreen(
              product = product,
              walletBalance = WalletDemoData.currentBalance,
              onBack = { backStack.removeLastOrNull() },
              onConfirm = { backStack.add(OrderProtected) },
              onCancel = { backStack.removeLastOrNull() }
            )
          }
          entry<OrderProtected> {
            val product = appViewModel.allProducts.first { it.id == "perfumeBlind" }
            OrderProtectedScreen(
              orderId = "GHOST-58291",
              amountSaved = product.price,
              balanceBefore = WalletDemoData.currentBalance,
              onBack = { backStack.removeLastOrNull() },
              onViewReceipt = {
                backStack.clear()
                backStack.add(WalletHome)
              },
              onBackToWallet = {
                backStack.clear()
                backStack.add(WalletHome)
              }
            )
          }

          // Ghost Wallet
          entry<WalletHome> {
            val state by appViewModel.uiState.collectAsState()
            WalletHomeScreen(
              hasAppliedForCard = state.hasAppliedForCard,
              isApplying = state.isApplying,
              onApplyForCard = { appViewModel.applyForGhostCard() },
              onGhostPay = { backStack.add(PayWithGhostCard) },
              onViewWallet = { backStack.add(WalletSetup) },
              onViewActivity = { backStack.add(WalletActivity) },
              onViewGoals = { backStack.add(Goals) }
            )
          }
          entry<WalletSetup> {
            val state by appViewModel.uiState.collectAsState()
            WalletSetupScreen(
              config = state.walletConfig,
              onBack = { backStack.removeLastOrNull() },
              onToggleSalaryShield = { appViewModel.updateWalletConfig { it.copy(salaryShieldEnabled = !it.salaryShieldEnabled) } },
              onOpenSalaryShield = { backStack.add(SalaryShield) },
              onCreateWallet = {
                backStack.clear()
                backStack.add(Home)
              },
              onContinueWithDefaults = {
                backStack.clear()
                backStack.add(Home)
              }
            )
          }
          entry<SalaryShield> {
            SalaryShieldScreen(onBack = { backStack.removeLastOrNull() })
          }
          entry<Goals> {
            GoalsScreen(
              onAllocateSavings = { backStack.removeLastOrNull() },
              onCreateGoal = { backStack.removeLastOrNull() }
            )
          }
          entry<WalletActivity> {
            WalletActivityScreen(
              onBack = { backStack.removeLastOrNull() },
              onOpenStatement = { backStack.add(WeeklyStatement) }
            )
          }
          entry<WeeklyStatement> {
            WeeklyStatementScreen(
              onBack = { backStack.removeLastOrNull() },
              onDownload = {},
              onShare = {}
            )
          }
          entry<Trends> {
            TrendsScreen(onGhostAnotherCart = {
              backStack.clear()
              backStack.add(Home)
            })
          }
          entry<GhostCardSettings> {
            val state by appViewModel.uiState.collectAsState()
            GhostCardSettingsScreen(
              config = state.walletConfig,
              authEmail = state.authEmail,
              onBack = { backStack.removeLastOrNull() },
              onToggleNotifications = { appViewModel.updateWalletConfig { it.copy(walletNotificationsEnabled = !it.walletNotificationsEnabled) } },
              onToggleAutoAllocate = { appViewModel.updateWalletConfig { it.copy(autoAllocateToGoals = !it.autoAllocateToGoals) } },
              onToggleFreeze = { appViewModel.updateWalletConfig { it.copy(cardFrozen = !it.cardFrozen) } },
              onDeleteWallet = { backStack.add(WalletHome) },
              onSignOut = {
                appViewModel.signOut()
                backStack.clear()
                backStack.add(Splash)
              }
            )
          }
        },
    )
  }

  val msg = state.toastMessage
  if (msg != null) {
    androidx.compose.foundation.layout.Box(
      modifier = Modifier
        .align(androidx.compose.ui.Alignment.TopCenter)
        .padding(top = 40.dp, start = 16.dp, end = 16.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Ink)
        .border(1.dp, GhostGreen, RoundedCornerShape(12.dp))
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = null,
          tint = GhostGreen,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = msg,
          color = Paper,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 8.dp)
        )
      }
    }
  }
}
}

@Composable
private fun GhostBottomNav(current: NavKey?, onNavigate: (NavKey) -> Unit) {
  val items: List<Triple<String, NavKey, androidx.compose.ui.graphics.vector.ImageVector>> = listOf(
    Triple("Home", Home, Icons.Filled.Home),
    Triple("Ghost Cart", GhostCartList, materialIconFor("bag")),
    Triple("Wallet", WalletHome, materialIconFor("wallet")),
    Triple("Trends", Trends, materialIconFor("target")),
    Triple("Profile", GhostCardSettings, Icons.Filled.Person)
  )

  NavigationBar(containerColor = Paper, tonalElevation = 0.dp) {
    items.forEach { (label, destination, icon) ->
      val selected = current == destination
      NavigationBarItem(
        selected = selected,
        onClick = { onNavigate(destination) },
        icon = { Icon(icon, contentDescription = label, tint = if (selected) GhostGreen else MutedText, modifier = Modifier.size(20.dp)) },
        label = { Text(text = label, fontSize = 9.sp, color = if (selected) Ink else MutedText) },
        colors = NavigationBarItemDefaults.colors(indicatorColor = androidx.compose.ui.graphics.Color.Transparent)
      )
    }
  }
}
