package com.example.ghostcart.ui.main

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostcart.theme.DarkGray
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.ui.CartScreen
import com.example.ghostcart.ui.CatalogScreen
import com.example.ghostcart.ui.CheckoutScreen
import com.example.ghostcart.ui.DashboardScreen
import com.example.ghostcart.ui.ProductIcon
import com.example.ghostcart.ui.ReceiptScreen
import com.example.ghostcart.ui.WaitlistScreen

@Composable
fun MainScreen(
    onItemClick: (Any) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var isCartOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                BottomTabs(
                    activeTab = state.activeTab,
                    cartCount = state.cartIds.size,
                    onTabSelected = { tab ->
                        if (tab == "Cart") {
                            isCartOpen = true
                        } else {
                            viewModel.changeTab(tab)
                        }
                    }
                )
            },
            containerColor = Ink
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (state.activeTab) {
                    "Catalog" -> CatalogScreen(
                        cartIds = state.cartIds,
                        cooledIds = state.cooledIds,
                        ghostedIds = state.ghostedIds,
                        onAddToCart = { viewModel.addToCart(it) },
                        onRemoveFromCart = { viewModel.removeFromCart(it) },
                        onCoolDown = { viewModel.startCooling(it) },
                        onResetState = { viewModel.resetProductState(it) }
                    )
                    "Dashboard" -> DashboardScreen(
                        ghostedCount = state.ghostedIds.size,
                        cooledCount = state.cooledIds.size
                    )
                    "Waitlist" -> WaitlistScreen(
                        submitted = state.waitlistSubmitted,
                        onSubmit = { viewModel.submitWaitlist(it) }
                    )
                }
            }
        }

        // Overlay 1: Cart screen (Drawer-style overlay)
        if (isCartOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Ink)
            ) {
                CartScreen(
                    cartIds = state.cartIds,
                    onRemoveFromCart = { viewModel.removeFromCart(it) },
                    onCheckout = {
                        isCartOpen = false
                        viewModel.triggerFakeCheckout()
                    }
                )
                // Back button/Close button at top left
                Text(
                    text = "← Back",
                    color = GhostGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 48.dp)
                        .clickable { isCartOpen = false }
                )
            }
        }

        // Overlay 2: Checkout / Simulated delivery timeline
        if (state.deliveryStep >= 0) {
            CheckoutScreen(
                deliveryStep = state.deliveryStep,
                onViewReceipt = { viewModel.showReceipt() }
            )
        }

        // Overlay 3: Receipt view
        if (state.receiptVisible) {
            ReceiptScreen(
                ghostedCount = state.ghostedIds.size,
                onDismiss = { viewModel.dismissReceipt() }
            )
        }
    }
}

@Composable
fun BottomTabs(
    activeTab: String,
    cartCount: Int,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Ink,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        val items = listOf("Catalog", "Dashboard", "Cart", "Waitlist")

        items.forEach { item ->
            val isSelected = activeTab == item
            val iconColor = if (isSelected) GhostGreen else Color.Gray

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item == "Cart" && cartCount > 0) {
                                Badge(
                                    containerColor = GhostGreen,
                                    contentColor = Ink
                                ) {
                                    Text("$cartCount", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        val iconName = when (item) {
                            "Catalog" -> "leaf"
                            "Dashboard" -> "chart"
                            "Cart" -> "wallet"
                            else -> "lock"
                        }
                        ProductIcon(name = iconName, modifier = Modifier.size(22.dp), color = iconColor)
                    }
                },
                label = {
                    Text(
                        text = item,
                        color = if (isSelected) Paper else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
