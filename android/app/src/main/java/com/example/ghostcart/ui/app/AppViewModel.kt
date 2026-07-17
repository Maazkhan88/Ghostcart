package com.example.ghostcart.ui.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.ghostcart.data.CoolingReminderWorker
import com.example.ghostcart.data.DailyGhostReminderWorker
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.data.WalletConfig
import com.example.ghostcart.data.WalletDemoData
import com.example.ghostcart.data.DeliveryStepWorker
import com.example.ghostcart.data.GhostActivityRepository
import com.example.ghostcart.data.GhostRanking
import com.example.ghostcart.data.GhostCardImageExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.UUID
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class AppUiState(
    val selectedProfile: String = "Male",
    val selectedOverspendIds: Set<String> = emptySet(),
    val selectedSavingsGoal: Int = 1000,
    val cartProductIds: List<String> = emptyList(),
    val cartQuantities: Map<String, Int> = emptyMap(),
    val walletConfig: WalletConfig = WalletConfig(),
    val coolingUntilByProductId: Map<String, Long> = emptyMap(),
    val lastOrderId: String = "",
    val lastOrderTotal: Int = 0,
    val deliveryStep: Int = -1,
    val promoApplied: Boolean = true,
    val authEmail: String? = null,
    val simulationIntervalMinutes: Int = 5,
    val hasAppliedForCard: Boolean = false,
    val isApplying: Boolean = false,
    val mostGhostedToday: List<GhostRanking> = emptyList(),
    val isMostGhostedLoading: Boolean = true,
    val isMostGhostedUnavailable: Boolean = false,
    val toastMessage: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("ghost_cart_prefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(AppUiState(
        authEmail = sharedPrefs.getString("auth_email", null),
        walletConfig = loadWalletConfig(),
        coolingUntilByProductId = loadCoolingPeriods()
    ))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var deliveryJob: Job? = null
    private var toastJob: Job? = null

    init {
        refreshMostGhostedToday()
        syncDailyGhostReminders(_uiState.value.walletConfig.walletNotificationsEnabled)
    }

    val allProducts: List<MarketplaceProduct> =
        (Marketplace.featuredCatalog + Marketplace.fakeFlashDeals + Marketplace.foodAndCoffeeCatalog + Marketplace.dummyCatalog)
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

    fun refreshMostGhostedToday() {
        _uiState.update { it.copy(isMostGhostedLoading = true, isMostGhostedUnavailable = false) }
        viewModelScope.launch {
            GhostActivityRepository.mostGhostedToday()
                .onSuccess { rankings ->
                    _uiState.update {
                        it.copy(
                            mostGhostedToday = rankings,
                            isMostGhostedLoading = false,
                            isMostGhostedUnavailable = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            mostGhostedToday = emptyList(),
                            isMostGhostedLoading = false,
                            isMostGhostedUnavailable = true
                        )
                    }
                }
        }
    }

    fun updateWalletConfig(transform: (WalletConfig) -> WalletConfig) {
        val notificationsWereEnabled = _uiState.value.walletConfig.walletNotificationsEnabled
        val nextConfig = transform(_uiState.value.walletConfig)
        persistWalletConfig(nextConfig)
        _uiState.update { it.copy(walletConfig = nextConfig) }
        if (notificationsWereEnabled != nextConfig.walletNotificationsEnabled) {
            syncDailyGhostReminders(nextConfig.walletNotificationsEnabled)
        }
    }

    fun downloadGhostCard() {
        val config = _uiState.value.walletConfig
        showToast("Creating high-resolution Ghost Card…")
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                GhostCardImageExporter.export(
                    context = getApplication<Application>(),
                    config = config,
                    cardholderName = config.cardholderName
                )
            }
            result.onSuccess {
                showToast("Ghost Card saved to Pictures/Ghost Cart")
            }.onFailure {
                showToast("Couldn't save the Ghost Card")
            }
        }
    }

    fun startCoolingPeriod(productId: String) {
        val product = findProduct(productId) ?: return
        val coolingUntil = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)
        val updatedPeriods = _uiState.value.coolingUntilByProductId + (productId to coolingUntil)
        persistCoolingPeriods(updatedPeriods)
        _uiState.update { it.copy(coolingUntilByProductId = updatedPeriods) }

        val request = OneTimeWorkRequestBuilder<CoolingReminderWorker>()
            .setInitialDelay(24, TimeUnit.HOURS)
            .setInputData(workDataOf("productName" to product.name, "productId" to product.id))
            .addTag("ghost_cooling_reminder")
            .build()
        WorkManager.getInstance(getApplication<Application>()).enqueueUniqueWork(
            "ghost_cooling_${product.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
        showToast("24-hour cooling started for ${product.name}")
    }

    private fun loadCoolingPeriods(): Map<String, Long> =
        sharedPrefs.getStringSet("cooling_periods", emptySet())
            .orEmpty()
            .mapNotNull { entry ->
                val separator = entry.lastIndexOf('|')
                if (separator <= 0) return@mapNotNull null
                val timestamp = entry.substring(separator + 1).toLongOrNull() ?: return@mapNotNull null
                entry.substring(0, separator) to timestamp
            }
            .toMap()

    private fun persistCoolingPeriods(periods: Map<String, Long>) {
        sharedPrefs.edit()
            .putStringSet("cooling_periods", periods.map { (id, until) -> "$id|$until" }.toSet())
            .apply()
    }

    private fun syncDailyGhostReminders(enabled: Boolean) {
        val workManager = WorkManager.getInstance(getApplication<Application>())
        if (!enabled) {
            workManager.cancelUniqueWork("ghost_daily_lunch")
            workManager.cancelUniqueWork("ghost_daily_dinner")
            return
        }
        scheduleDailyGhostReminder(workManager, meal = "lunch", hourOfDay = 13)
        scheduleDailyGhostReminder(workManager, meal = "dinner", hourOfDay = 20)
    }

    private fun scheduleDailyGhostReminder(workManager: WorkManager, meal: String, hourOfDay: Int) {
        val request = PeriodicWorkRequestBuilder<DailyGhostReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntilHour(hourOfDay), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("meal" to meal))
            .addTag("ghost_daily_reminder")
            .build()
        workManager.enqueueUniquePeriodicWork(
            "ghost_daily_$meal",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun delayUntilHour(hourOfDay: Int): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return (next.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    private fun loadWalletConfig(): WalletConfig {
        val ghostId = sharedPrefs.getString("wallet_ghost_id", null)
            ?: createGhostId().also { sharedPrefs.edit().putString("wallet_ghost_id", it).apply() }
        val memberSince = sharedPrefs.getString("wallet_member_since", null)
            ?: SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date()).also {
                sharedPrefs.edit().putString("wallet_member_since", it).apply()
            }
        return WalletConfig(
            monthlySalary = sharedPrefs.getInt("wallet_monthly_salary", 12000),
            monthlySavingsGoal = sharedPrefs.getInt("wallet_monthly_savings_goal", 2500),
            temptationBudget = sharedPrefs.getInt("wallet_temptation_budget", 1500),
            startingBalance = sharedPrefs.getInt("wallet_starting_balance", 0),
            salaryShieldEnabled = sharedPrefs.getBoolean("wallet_salary_shield_enabled", true),
            walletNotificationsEnabled = sharedPrefs.getBoolean("wallet_notifications_enabled", true),
            autoAllocateToGoals = sharedPrefs.getBoolean("wallet_auto_allocate", true),
            cardFrozen = sharedPrefs.getBoolean("wallet_card_frozen", false),
            cardTheme = sharedPrefs.getString("wallet_card_theme", "Dark") ?: "Dark",
            cardName = sharedPrefs.getString("wallet_card_name", "Ghost Card") ?: "Ghost Card",
            cardholderName = sharedPrefs.getString("wallet_cardholder_name", "Ghost Member") ?: "Ghost Member",
            salaryShieldPercent = sharedPrefs.getInt("wallet_salary_shield_percent", 20),
            ghostId = ghostId,
            memberSince = memberSince
        )
    }

    private fun createGhostId(): String {
        val token = UUID.randomUUID().toString().replace("-", "").uppercase(Locale.ENGLISH).take(12)
        return "GC-${token.chunked(4).joinToString("-")}"
    }

    private fun persistWalletConfig(config: WalletConfig) {
        sharedPrefs.edit()
            .putInt("wallet_monthly_salary", config.monthlySalary)
            .putInt("wallet_monthly_savings_goal", config.monthlySavingsGoal)
            .putInt("wallet_temptation_budget", config.temptationBudget)
            .putInt("wallet_starting_balance", config.startingBalance)
            .putBoolean("wallet_salary_shield_enabled", config.salaryShieldEnabled)
            .putBoolean("wallet_notifications_enabled", config.walletNotificationsEnabled)
            .putBoolean("wallet_auto_allocate", config.autoAllocateToGoals)
            .putBoolean("wallet_card_frozen", config.cardFrozen)
            .putString("wallet_card_theme", config.cardTheme)
            .putString("wallet_card_name", config.cardName)
            .putString("wallet_cardholder_name", config.cardholderName)
            .putInt("wallet_salary_shield_percent", config.salaryShieldPercent)
            .putString("wallet_ghost_id", config.ghostId)
            .putString("wallet_member_since", config.memberSince)
            .apply()
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
        val activityCheckoutId = UUID.randomUUID().toString()
        val ghostedProductIds = _uiState.value.cartProductIds.distinct()
        _uiState.update {
            it.copy(
                lastOrderId = orderId,
                lastOrderTotal = total,
                cartProductIds = emptyList(),
                cartQuantities = emptyMap()
            )
        }
        showToast("Ghost Order Placed Successfully!")

        if (ghostedProductIds.isNotEmpty()) {
            viewModelScope.launch {
                GhostActivityRepository.recordCheckout(
                    checkoutId = activityCheckoutId,
                    productIds = ghostedProductIds
                ).onSuccess {
                    refreshMostGhostedToday()
                }
            }
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
