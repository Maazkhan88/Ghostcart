package com.example.ghostcart.ui.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.data.WalletConfig
import com.example.ghostcart.data.DeliveryStepWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class AppUiState(
    val selectedProfile: String = "Male",
    val selectedOverspendIds: Set<String> = emptySet(),
    val selectedSavingsGoal: Int = 1000,
    val cartProductIds: List<String> = emptyList(),
    val cartQuantities: Map<String, Int> = emptyMap(),
    val walletConfig: WalletConfig = WalletConfig(),
    val lastOrderId: String = "",
    val lastOrderTotal: Int = 0,
    val deliveryStep: Int = -1,
    val promoApplied: Boolean = true,
    val authEmail: String? = null,
    val simulationIntervalMinutes: Int = 5,
    val hasAppliedForCard: Boolean = false,
    val isApplying: Boolean = false,
    val toastMessage: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("ghost_cart_prefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(AppUiState(
        authEmail = sharedPrefs.getString("auth_email", null)
    ))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var deliveryJob: Job? = null
    private var toastJob: Job? = null

    val allProducts: List<MarketplaceProduct> =
        (Marketplace.mostGhostedToday + Marketplace.fakeFlashDeals + Marketplace.foodAndCoffeeCatalog + Marketplace.dummyCatalog)
            .distinctBy { it.id }

    fun findProduct(id: String): MarketplaceProduct? = allProducts.find { it.id == id }

    fun cartProducts(): List<MarketplaceProduct> =
        _uiState.value.cartProductIds.mapNotNull { findProduct(it) }

    fun cartProductsWithQuantities(): List<Pair<MarketplaceProduct, Int>> =
        _uiState.value.cartQuantities.mapNotNull { (id, qty) ->
            findProduct(id)?.let { it to qty }
        }

    fun cartSubtotal(): Int = cartProductsWithQuantities().sumOf { (product, qty) ->
        product.price * qty
    }

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
            val nextQty = (current.cartQuantities[productId] ?: 0) + 1
            val nextMap = current.cartQuantities + (productId to nextQty)
            val nextList = nextMap.keys.toList()
            current.copy(
                cartQuantities = nextMap,
                cartProductIds = nextList
            )
        }
        val name = findProduct(productId)?.name ?: "item"
        showToast("Added $name to Ghost Cart")
    }

    fun removeFromCart(productId: String) {
        _uiState.update { current ->
            val nextQty = (current.cartQuantities[productId] ?: 0) - 1
            val nextMap = if (nextQty <= 0) {
                current.cartQuantities - productId
            } else {
                current.cartQuantities + (productId to nextQty)
            }
            val nextList = nextMap.keys.toList()
            current.copy(
                cartQuantities = nextMap,
                cartProductIds = nextList
            )
        }
        val name = findProduct(productId)?.name ?: "item"
        showToast("Removed $name from Ghost Cart")
    }

    fun updateQuantity(productId: String, quantity: Int) {
        _uiState.update { current ->
            val nextMap = if (quantity <= 0) {
                current.cartQuantities - productId
            } else {
                current.cartQuantities + (productId to quantity)
            }
            val nextList = nextMap.keys.toList()
            current.copy(
                cartQuantities = nextMap,
                cartProductIds = nextList
            )
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartProductIds = emptyList(), cartQuantities = emptyMap()) }
    }

    fun updateWalletConfig(transform: (WalletConfig) -> WalletConfig) {
        _uiState.update { it.copy(walletConfig = transform(it.walletConfig)) }
    }

    fun setSimulationInterval(minutes: Int) {
        _uiState.update { it.copy(simulationIntervalMinutes = minutes) }
    }

    fun authenticate(email: String) {
        sharedPrefs.edit().putString("auth_email", email).apply()
        _uiState.update { it.copy(authEmail = email) }
        showToast("Signed in as $email")
    }

    fun signOut() {
        sharedPrefs.edit().remove("auth_email").apply()
        _uiState.update { it.copy(authEmail = null) }
        showToast("Signed out successfully")
    }

    fun applyForGhostCard() {
        if (_uiState.value.hasAppliedForCard || _uiState.value.isApplying) return
        _uiState.update { it.copy(isApplying = true) }
        viewModelScope.launch {
            delay(1500) // simulated processing delay
            _uiState.update { it.copy(hasAppliedForCard = true, isApplying = false) }
            showToast("Ghost Card Digitally Delivered!")
        }
    }

    fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(2500)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    fun placeSimulatedOrder() {
        val total = cartSubtotal()
        val orderId = "GHOST-${10000 + Random.nextInt(90000)}"
        _uiState.update {
            it.copy(
                lastOrderId = orderId,
                lastOrderTotal = total,
                cartProductIds = emptyList(),
                cartQuantities = emptyMap()
            )
        }
        showToast("Ghost Order Placed Successfully!")
    }

    fun startDeliveryTracking() {
        deliveryJob?.cancel()
        _uiState.update { it.copy(deliveryStep = 0) }

        val orderId = _uiState.value.lastOrderId.ifBlank { "GHOST-00000" }
        val amountSaved = _uiState.value.lastOrderTotal
        val intervalMinutes = _uiState.value.simulationIntervalMinutes

        // Enqueue background notifications and step progression using WorkManager
        val context = getApplication<Application>()
        val workManager = WorkManager.getInstance(context)
        
        // Cancel any existing simulation work first
        workManager.cancelAllWorkByTag("ghost_delivery_simulation")

        // Schedule step 1 to 5 with proportional initial delays
        for (step in 1..5) {
            val delaySeconds = step * intervalMinutes * 60L
            val workRequest = OneTimeWorkRequestBuilder<DeliveryStepWorker>()
                .setInputData(workDataOf(
                    "orderId" to orderId,
                    "amountSaved" to amountSaved,
                    "stepIndex" to step
                ))
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .addTag("ghost_delivery_simulation")
                .build()
            workManager.enqueue(workRequest)
        }

        // We also run a local coroutine in the app for immediate UI updates if the app is open
        deliveryJob = viewModelScope.launch {
            val intervalMs = intervalMinutes * 60L * 1000L
            for (step in 1..5) {
                delay(intervalMs)
                _uiState.update { it.copy(deliveryStep = step) }
            }
        }
    }

    fun resetDeliveryTracking() {
        deliveryJob?.cancel()
        val context = getApplication<Application>()
        WorkManager.getInstance(context).cancelAllWorkByTag("ghost_delivery_simulation")
        _uiState.update { it.copy(deliveryStep = -1) }
    }
}
