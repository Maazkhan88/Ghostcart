package com.example.ghostcart.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GhostCartUiState(
    val cartIds: Set<String> = emptySet(),
    val cooledIds: Set<String> = emptySet(),
    val ghostedIds: Set<String> = emptySet(),
    val deliveryStep: Int = -1,
    val receiptVisible: Boolean = false,
    val focusMode: Boolean = false,
    val waitlistSubmitted: Boolean = false,
    val waitlistEmail: String = "",
    val activeTab: String = "Catalog" // "Catalog", "Dashboard", "Waitlist"
)

class MainScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GhostCartUiState())
    val uiState: StateFlow<GhostCartUiState> = _uiState.asStateFlow()

    private var deliveryJob: Job? = null

    fun addToCart(productId: String) {
        _uiState.update { current ->
            current.copy(
                cartIds = current.cartIds + productId,
                receiptVisible = false
            )
        }
    }

    fun removeFromCart(productId: String) {
        _uiState.update { current ->
            current.copy(cartIds = current.cartIds - productId)
        }
    }

    fun startCooling(productId: String) {
        _uiState.update { current ->
            current.copy(
                cooledIds = current.cooledIds + productId,
                cartIds = current.cartIds - productId
            )
        }
    }

    fun resetProductState(productId: String) {
        _uiState.update { current ->
            current.copy(
                cooledIds = current.cooledIds - productId,
                ghostedIds = current.ghostedIds - productId,
                cartIds = current.cartIds - productId
            )
        }
    }

    fun triggerFakeCheckout() {
        val cartItems = _uiState.value.cartIds
        if (cartItems.isEmpty()) return

        _uiState.update { current ->
            current.copy(
                ghostedIds = current.ghostedIds + cartItems,
                cartIds = emptySet(),
                receiptVisible = false,
                deliveryStep = 0
            )
        }

        // Start delivery timeline loop
        deliveryJob?.cancel()
        deliveryJob = viewModelScope.launch {
            val durations = listOf(2000L, 2000L, 2500L, 2500L, 3000L)
            for (step in 0..4) {
                delay(durations[step])
                _uiState.update { it.copy(deliveryStep = step + 1) }
            }
        }
    }

    fun showReceipt() {
        _uiState.update { current ->
            current.copy(
                deliveryStep = -1,
                receiptVisible = true
            )
        }
    }

    fun dismissReceipt() {
        _uiState.update { current ->
            current.copy(receiptVisible = false)
        }
    }

    fun toggleFocusMode() {
        _uiState.update { current ->
            current.copy(focusMode = !current.focusMode)
        }
    }

    fun submitWaitlist(email: String) {
        _uiState.update { current ->
            current.copy(
                waitlistSubmitted = true,
                waitlistEmail = email
            )
        }
    }

    fun changeTab(tab: String) {
        _uiState.update { current ->
            current.copy(activeTab = tab)
        }
    }
}
