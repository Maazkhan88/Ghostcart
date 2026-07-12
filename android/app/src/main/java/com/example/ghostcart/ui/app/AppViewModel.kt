package com.example.ghostcart.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.data.WalletConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class AppUiState(
    val selectedProfile: String = "Male",
    val selectedOverspendIds: Set<String> = emptySet(),
    val selectedSavingsGoal: Int = 1000,
    val cartProductIds: List<String> = emptyList(),
    val walletConfig: WalletConfig = WalletConfig(),
    val lastOrderId: String = "",
    val lastOrderTotal: Int = 0,
    val deliveryStep: Int = -1,
    val promoApplied: Boolean = true
)

class AppViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var deliveryJob: Job? = null

    val allProducts: List<MarketplaceProduct> =
        (Marketplace.mostGhostedToday + Marketplace.fakeFlashDeals + Marketplace.foodAndCoffeeCatalog)
            .distinctBy { it.id }

    fun findProduct(id: String): MarketplaceProduct? = allProducts.find { it.id == id }

    fun cartProducts(): List<MarketplaceProduct> =
        _uiState.value.cartProductIds.mapNotNull { findProduct(it) }

    fun cartSubtotal(): Int = cartProducts().sumOf { it.price }

    fun selectProfile(profile: String) {
        _uiState.update { it.copy(selectedProfile = profile) }
    }

    fun toggleOverspendCategory(id: String) {
        _uiState.update { current ->
            val next = if (current.selectedOverspendIds.contains(id)) {
                current.selectedOverspendIds - id
            } else {
                current.selectedOverspendIds + id
            }
            current.copy(selectedOverspendIds = next)
        }
    }

    fun selectSavingsGoal(amount: Int) {
        _uiState.update { it.copy(selectedSavingsGoal = amount) }
    }

    fun addToCart(productId: String) {
        _uiState.update { current ->
            if (current.cartProductIds.contains(productId)) current
            else current.copy(cartProductIds = current.cartProductIds + productId)
        }
    }

    fun removeFromCart(productId: String) {
        _uiState.update { current ->
            current.copy(cartProductIds = current.cartProductIds.filterNot { it == productId })
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartProductIds = emptyList()) }
    }

    fun updateWalletConfig(transform: (WalletConfig) -> WalletConfig) {
        _uiState.update { it.copy(walletConfig = transform(it.walletConfig)) }
    }

    fun placeSimulatedOrder() {
        val total = cartSubtotal()
        val orderId = "GHOST-${10000 + Random.nextInt(90000)}"
        _uiState.update {
            it.copy(
                lastOrderId = orderId,
                lastOrderTotal = total,
                cartProductIds = emptyList()
            )
        }
    }

    fun startDeliveryTracking() {
        deliveryJob?.cancel()
        _uiState.update { it.copy(deliveryStep = 0) }
        deliveryJob = viewModelScope.launch {
            val durations = listOf(1200L, 1400L, 1600L, 1400L, 1200L)
            for (step in 0..4) {
                delay(durations[step])
                _uiState.update { it.copy(deliveryStep = step + 1) }
            }
        }
    }

    fun resetDeliveryTracking() {
        deliveryJob?.cancel()
        _uiState.update { it.copy(deliveryStep = -1) }
    }
}
