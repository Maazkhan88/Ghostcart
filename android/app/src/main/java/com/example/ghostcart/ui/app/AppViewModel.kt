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
    val walletConfig: WalletConfig = WalletConfig(),
    val lastOrderId: String = "",
    val lastOrderTotal: Int = 0,
    val deliveryStep: Int = -1,
    val promoApplied: Boolean = true,
    val authEmail: String? = null,
    val simulationIntervalMinutes: Int = 5 // User customizable time between steps
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("ghost_cart_prefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(AppUiState(
        authEmail = sharedPrefs.getString("auth_email", null)
    ))
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

    fun setSimulationInterval(minutes: Int) {
        _uiState.update { it.copy(simulationIntervalMinutes = minutes) }
    }

    fun authenticate(email: String) {
        sharedPrefs.edit().putString("auth_email", email).apply()
        _uiState.update { it.copy(authEmail = email) }
    }

    fun signOut() {
        sharedPrefs.edit().remove("auth_email").apply()
        _uiState.update { it.copy(authEmail = null) }
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
