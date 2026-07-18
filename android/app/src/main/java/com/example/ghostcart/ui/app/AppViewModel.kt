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
import com.example.ghostcart.data.AlmostBuy
import com.example.ghostcart.data.AlmostBuyDraft
import com.example.ghostcart.data.AlmostBuyRepository
import com.example.ghostcart.data.AlmostBuyResolution
import com.example.ghostcart.data.LocalAlmostBuyRepository
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.data.WalletConfig
import com.example.ghostcart.data.WalletDemoData
import com.example.ghostcart.data.DeliveryStepWorker
import com.example.ghostcart.data.GhostActivityRepository
import com.example.ghostcart.data.GhostRanking
import com.example.ghostcart.data.GhostCardImageExporter
import com.example.ghostcart.data.CommunityProduct
import com.example.ghostcart.data.DeviceLinkPreview
import com.example.ghostcart.data.mergeDeviceMetadata
import com.example.ghostcart.data.LinkImportResult
import com.example.ghostcart.data.ListingProductStub
import com.example.ghostcart.data.ProductImportRepository
import com.example.ghostcart.data.ProductImportState
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
    val lastOrderPlacedAtMillis: Long = 0L,
    val deliveryStep: Int = -1,
    val promoApplied: Boolean = true,
    val authEmail: String? = null,
    val simulationIntervalMinutes: Int = 5,
    val hasAppliedForCard: Boolean = false,
    val isApplying: Boolean = false,
    val mostGhostedToday: List<GhostRanking> = emptyList(),
    val isMostGhostedLoading: Boolean = true,
    val isMostGhostedUnavailable: Boolean = false,
    val toastMessage: String? = null,
    val almostBuys: List<AlmostBuy> = emptyList(),
    val productImportState: ProductImportState = ProductImportState.Idle,
    val communityProducts: List<CommunityProduct> = emptyList(),
    val communityProductsLoading: Boolean = true,
    val captureSeed: AlmostBuyDraft? = null,
    val appTheme: String = "System",
    val feedbackSubmittedOrderIds: Set<String> = emptySet()
)

internal fun deliveryStepAt(nowMillis: Long, placedAtMillis: Long, intervalMinutes: Int): Int {
    if (placedAtMillis <= 0L) return -1
    val stepDuration = intervalMinutes.coerceAtLeast(1) * 60_000L
    return ((nowMillis - placedAtMillis).coerceAtLeast(0L) / stepDuration).toInt().coerceIn(0, 4)
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("ghost_cart_prefs", Context.MODE_PRIVATE)
    private val almostBuyRepository: AlmostBuyRepository = LocalAlmostBuyRepository(application)
    
    private val _uiState = MutableStateFlow(AppUiState(
        authEmail = sharedPrefs.getString("auth_email", null),
        walletConfig = loadWalletConfig(),
        coolingUntilByProductId = loadCoolingPeriods(),
        appTheme = sharedPrefs.getString("app_theme", "System") ?: "System",
        lastOrderId = sharedPrefs.getString("last_order_id", "") ?: "",
        lastOrderTotal = sharedPrefs.getInt("last_order_total", 0),
        lastOrderPlacedAtMillis = sharedPrefs.getLong("last_order_placed_at", 0L),
        simulationIntervalMinutes = sharedPrefs.getInt("simulation_interval_minutes", 5),
        deliveryStep = deliveryStepAt(
            System.currentTimeMillis(),
            sharedPrefs.getLong("last_order_placed_at", 0L),
            sharedPrefs.getInt("simulation_interval_minutes", 5)
        )
    ))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var deliveryJob: Job? = null
    private var toastJob: Job? = null
    private val sharedCartProducts = mutableMapOf<String, MarketplaceProduct>()

    init {
        refreshMostGhostedToday()
        refreshCommunityProducts()
        syncDailyGhostReminder("lunch", 13, _uiState.value.walletConfig.lunchReminderEnabled)
        syncDailyGhostReminder("dinner", 20, _uiState.value.walletConfig.dinnerReminderEnabled)
        if (_uiState.value.deliveryStep in 0..3) beginDeliveryClock()
        viewModelScope.launch {
            almostBuyRepository.items.collect { items ->
                _uiState.update { it.copy(almostBuys = items) }
            }
        }
    }

    val allProducts: List<MarketplaceProduct> =
        (Marketplace.featuredCatalog + Marketplace.discoveryCatalog + Marketplace.fakeFlashDeals + Marketplace.foodAndCoffeeCatalog)
            .distinctBy { it.id }

    fun findProduct(id: String): MarketplaceProduct? =
        allProducts.find { it.id == id } ?: sharedCartProducts[id]

    fun setAppTheme(theme: String) {
        val normalized = theme.takeIf { it in setOf("System", "Light", "Dark") } ?: "System"
        sharedPrefs.edit().putString("app_theme", normalized).apply()
        _uiState.update { it.copy(appTheme = normalized) }
    }

    fun addCommunityToCart(productId: String) {
        val source = _uiState.value.communityProducts.find { it.id == productId } ?: return
        val product = communityMarketplaceProduct(source)
        sharedCartProducts[product.id] = product
        addToCart(product.id)
    }

    /** Makes an anonymous community item available to the shared details screen. */
    fun communityProductDetailId(productId: String): String? {
        val source = _uiState.value.communityProducts.find { it.id == productId } ?: return null
        val product = communityMarketplaceProduct(source)
        sharedCartProducts[product.id] = product
        return product.id
    }

    private fun communityMarketplaceProduct(source: CommunityProduct) = MarketplaceProduct(
        id = "community_${source.id}",
        name = source.title,
        category = normalizeCategory(source.category),
        price = (source.priceCents / 100L).toInt().coerceAtLeast(0),
        iconName = "gadget",
        brand = source.sourceDomain,
        imageUrl = source.imageUrl
    )

    fun addDraftToCart(draft: AlmostBuyDraft) {
        val id = "shared_${UUID.randomUUID()}"
        val product = MarketplaceProduct(
            id = id,
            name = draft.name,
            category = normalizeCategory(draft.category),
            price = (draft.amountCents / 100L).toInt().coerceAtLeast(0),
            iconName = "bag",
            imageUrl = draft.imageUrl,
            sourceUrl = draft.sourceUrl
        )
        sharedCartProducts[id] = product
        addToCart(id)
        if (draft.shareWithCommunity) publishCommunityDraft(draft)
    }

    fun refreshCommunityProducts() {
        _uiState.update { it.copy(communityProductsLoading = true) }
        viewModelScope.launch {
            ProductImportRepository.communityFeed()
                .onSuccess { products ->
                    _uiState.update { it.copy(communityProducts = products, communityProductsLoading = false) }
                }
                .onFailure { _uiState.update { it.copy(communityProductsLoading = false) } }
        }
    }

    private fun publishCommunityDraft(draft: AlmostBuyDraft) {
        viewModelScope.launch {
            ProductImportRepository.publish(draft)
                .onSuccess { published ->
                    if (published != null) {
                        _uiState.update { state ->
                            state.copy(
                                communityProducts = listOf(published) + state.communityProducts.filterNot { it.id == published.id },
                                communityProductsLoading = false
                            )
                        }
                    }
                    refreshCommunityProducts()
                }
                .onFailure { showToast("Added to cart; anonymous community sharing could not be completed") }
        }
    }

    fun importSharedProduct(sourceUrl: String, sharedTitle: String? = null, sharedImageUrl: String? = null) {
        _uiState.update { it.copy(productImportState = ProductImportState.Loading) }
        viewModelScope.launch {
            val result = ProductImportRepository.previewLink(sourceUrl, sharedTitle, sharedImageUrl)
            val linkResult = result.getOrElse { error ->
                _uiState.update { it.copy(productImportState = ProductImportState.Error(error.message ?: "Unable to read this product")) }
                return@launch
            }
            if (linkResult is LinkImportResult.Listing) {
                if (linkResult.items.isEmpty()) {
                    _uiState.update {
                        it.copy(productImportState = ProductImportState.Error("Ghost Cart could not find any products on that page. Try a single product link instead."))
                    }
                } else {
                    _uiState.update {
                        it.copy(productImportState = ProductImportState.ListingDetected(linkResult.sourceDomain, linkResult.retailer, linkResult.items))
                    }
                }
                return@launch
            }
            val serverProduct = (linkResult as LinkImportResult.Product).product
            val needsDevicePreview = serverProduct.imageUrl == null || serverProduct.priceCents == null || serverProduct.status != "complete"
            val deviceMetadata = if (needsDevicePreview) {
                DeviceLinkPreview.read(getApplication(), serverProduct.sourceUrl)
            } else {
                null
            }
            val product = deviceMetadata?.let { mergeDeviceMetadata(serverProduct, it) } ?: serverProduct
            _uiState.update {
                it.copy(
                    productImportState = ProductImportState.Ready(product),
                    captureSeed = AlmostBuyDraft(
                        name = product.title,
                        amountCents = if (product.currencyCode == null || product.currencyCode == "AED") product.priceCents ?: 0 else 0,
                        category = product.category,
                        trigger = "FOMO",
                        coolingDurationMillis = recommendedCooling(product.category),
                        sourceUrl = product.sourceUrl,
                        imageUrl = product.imageUrl,
                        sourceKind = "share"
                    )
                )
            }
        }
    }

    fun addListingItemsToCart(items: List<ListingProductStub>) {
        items.forEach { stub ->
            addDraftToCart(
                AlmostBuyDraft(
                    name = stub.title,
                    amountCents = if (stub.currencyCode == null || stub.currencyCode == "AED") stub.priceCents ?: 0 else 0,
                    category = normalizeCategory(stub.category),
                    trigger = "FOMO",
                    coolingDurationMillis = recommendedCooling(stub.category),
                    sourceUrl = stub.sourceUrl,
                    imageUrl = stub.imageUrl,
                    sourceKind = "bulk_share"
                )
            )
        }
        showToast(
            if (items.size == 1) "Added 1 item to Ghost Cart" else "Added ${items.size} items to Ghost Cart"
        )
    }

    fun clearProductImport() {
        _uiState.update { it.copy(productImportState = ProductImportState.Idle, captureSeed = null) }
    }

    fun prepareCatalogProduct(productId: String) {
        val product = findProduct(productId) ?: return
        _uiState.update {
            it.copy(captureSeed = AlmostBuyDraft(
                name = product.name,
                amountCents = product.price.toLong() * 100,
                category = normalizeCategory(product.category),
                trigger = "FOMO",
                coolingDurationMillis = recommendedCooling(product.category),
                sourceKind = "catalog"
            ))
        }
    }

    fun prepareCommunityProduct(productId: String) {
        val product = _uiState.value.communityProducts.find { it.id == productId } ?: return
        _uiState.update {
            it.copy(captureSeed = AlmostBuyDraft(
                name = product.title,
                amountCents = product.priceCents,
                category = normalizeCategory(product.category),
                trigger = "FOMO",
                coolingDurationMillis = recommendedCooling(product.category),
                imageUrl = product.imageUrl,
                sourceKind = "catalog"
            ))
        }
    }

    fun clearCaptureSeed() {
        _uiState.update { it.copy(captureSeed = null, productImportState = ProductImportState.Idle) }
    }

    fun quickGhostCatalogProduct(productId: String, onCreated: () -> Unit = {}) {
        prepareCatalogProduct(productId)
        val draft = _uiState.value.captureSeed ?: return
        createAlmostBuy(draft) { clearCaptureSeed(); onCreated() }
    }

    fun quickGhostCommunityProduct(productId: String, onCreated: () -> Unit = {}) {
        prepareCommunityProduct(productId)
        val draft = _uiState.value.captureSeed ?: return
        createAlmostBuy(draft) { clearCaptureSeed(); onCreated() }
    }

    private fun normalizeCategory(value: String): String = when {
        value.contains("food", true) || value.contains("coffee", true) || value.contains("delivery", true) -> "Food & drinks"
        value.contains("tech", true) || value.contains("gadget", true) -> "Electronics"
        value.contains("fashion", true) || value.contains("apparel", true) -> "Fashion"
        value.contains("beauty", true) -> "Beauty"
        value.contains("gaming", true) -> "Gaming"
        value.contains("music", true) -> "Music"
        value.contains("home", true) -> "Home"
        else -> value.ifBlank { "Other" }
    }

    private fun recommendedCooling(category: String): Long = when {
        category.contains("food", true) || category.contains("coffee", true) -> TimeUnit.MINUTES.toMillis(15)
        category.contains("electronic", true) || category.contains("tech", true) || category.contains("gadget", true) -> TimeUnit.DAYS.toMillis(3)
        else -> TimeUnit.HOURS.toMillis(24)
    }

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
        val previousConfig = _uiState.value.walletConfig
        val nextConfig = transform(_uiState.value.walletConfig)
        persistWalletConfig(nextConfig)
        _uiState.update { it.copy(walletConfig = nextConfig) }
        if (previousConfig.lunchReminderEnabled != nextConfig.lunchReminderEnabled) {
            syncDailyGhostReminder("lunch", 13, nextConfig.lunchReminderEnabled)
        }
        if (previousConfig.dinnerReminderEnabled != nextConfig.dinnerReminderEnabled) {
            syncDailyGhostReminder("dinner", 20, nextConfig.dinnerReminderEnabled)
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

    private fun syncDailyGhostReminder(meal: String, hourOfDay: Int, enabled: Boolean) {
        val workManager = WorkManager.getInstance(getApplication<Application>())
        if (!enabled) {
            workManager.cancelUniqueWork("ghost_daily_$meal")
            return
        }
        scheduleDailyGhostReminder(workManager, meal = meal, hourOfDay = hourOfDay)
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
            coolingNotificationsEnabled = sharedPrefs.getBoolean("cooling_notifications_enabled", true),
            lunchReminderEnabled = sharedPrefs.getBoolean("lunch_reminder_enabled", false),
            dinnerReminderEnabled = sharedPrefs.getBoolean("dinner_reminder_enabled", false),
            autoAllocateToGoals = sharedPrefs.getBoolean("wallet_auto_allocate", true),
            cardFrozen = sharedPrefs.getBoolean("wallet_card_frozen", false),
            cardTheme = sharedPrefs.getString("wallet_card_theme", "Dark") ?: "Dark",
            cardName = sharedPrefs.getString("wallet_card_name", "Ghost Membership") ?: "Ghost Membership",
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
            .putBoolean("cooling_notifications_enabled", config.coolingNotificationsEnabled)
            .putBoolean("lunch_reminder_enabled", config.lunchReminderEnabled)
            .putBoolean("dinner_reminder_enabled", config.dinnerReminderEnabled)
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
        val normalized = minutes.coerceAtLeast(1)
        sharedPrefs.edit().putInt("simulation_interval_minutes", normalized).apply()
        _uiState.update { it.copy(simulationIntervalMinutes = normalized) }
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

    fun createAlmostBuy(draft: AlmostBuyDraft, onCreated: (AlmostBuy) -> Unit = {}) {
        viewModelScope.launch {
            val item = almostBuyRepository.create(draft)
            scheduleCoolingNotification(item)
            if (draft.shareWithCommunity) {
                ProductImportRepository.publish(draft)
                    .onSuccess { refreshCommunityProducts() }
                    .onFailure { showToast("Item is cooling; anonymous sharing could not be completed") }
            }
            showToast("${item.name} is cooling")
            onCreated(item)
        }
    }

    fun resolveAlmostBuy(id: String, resolution: AlmostBuyResolution) {
        viewModelScope.launch {
            val item = almostBuyRepository.resolve(id, resolution) ?: return@launch
            WorkManager.getInstance(getApplication<Application>())
                .cancelUniqueWork("ghost_cooling_${item.id}")
            showToast(
                if (resolution == AlmostBuyResolution.SKIPPED) {
                    "Confirmed as money kept"
                } else {
                    "Decision recorded without counting savings"
                }
            )
        }
    }

    fun extendAlmostBuy(id: String, durationMillis: Long) {
        viewModelScope.launch {
            val item = almostBuyRepository.extendCooling(id, durationMillis) ?: return@launch
            scheduleCoolingNotification(item)
            showToast("Cooling extended")
        }
    }

    private fun scheduleCoolingNotification(item: AlmostBuy) {
        if (!_uiState.value.walletConfig.coolingNotificationsEnabled) return
        val delayMillis = (item.coolingUntilMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
        val request = OneTimeWorkRequestBuilder<CoolingReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("productName" to item.name, "productId" to item.id))
            .addTag("ghost_cooling_reminder")
            .build()
        WorkManager.getInstance(getApplication<Application>()).enqueueUniqueWork(
            "ghost_cooling_${item.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun placeSimulatedOrder() {
        val total = cartSubtotal()
        val orderId = "GHOST-${10000 + Random.nextInt(90000)}"
        val placedAt = System.currentTimeMillis()
        val activityCheckoutId = UUID.randomUUID().toString()
        val ghostedProductIds = _uiState.value.cartProductIds.distinct()
        _uiState.update {
            it.copy(
                lastOrderId = orderId,
                lastOrderTotal = total,
                lastOrderPlacedAtMillis = placedAt,
                deliveryStep = 0,
                cartProductIds = emptyList(),
                cartQuantities = emptyMap()
            )
        }
        sharedPrefs.edit()
            .putString("last_order_id", orderId)
            .putInt("last_order_total", total)
            .putLong("last_order_placed_at", placedAt)
            .apply()
        scheduleDeliveryWorkers(orderId, total, _uiState.value.simulationIntervalMinutes)
        beginDeliveryClock()
        showToast("Ghost Order Placed Successfully!")

        if (ghostedProductIds.isNotEmpty()) {
            viewModelScope.launch {
                GhostActivityRepository.recordCheckout(
                    checkoutId = activityCheckoutId,
                    productIds = ghostedProductIds
                ).onSuccess {
                    refreshMostGhostedToday()
        refreshCommunityProducts()
                }
            }
        }
    }

    fun startDeliveryTracking() {
        val orderId = _uiState.value.lastOrderId.ifBlank { "GHOST-00000" }
        val amountSaved = _uiState.value.lastOrderTotal
        val intervalMinutes = _uiState.value.simulationIntervalMinutes
        val needsInitialization = _uiState.value.lastOrderPlacedAtMillis <= 0L
        if (needsInitialization) {
            val placedAt = System.currentTimeMillis()
            sharedPrefs.edit().putLong("last_order_placed_at", placedAt).apply()
            _uiState.update { it.copy(lastOrderPlacedAtMillis = placedAt, deliveryStep = 0) }
            scheduleDeliveryWorkers(orderId, amountSaved, intervalMinutes)
        }
        beginDeliveryClock()
    }

    fun prepareSharedGhostItem(
        title: String,
        priceCents: Long,
        category: String,
        imageUrl: String?,
        sourceUrl: String?
    ) {
        val safeTitle = title.trim().take(160)
        if (safeTitle.isBlank()) return
        _uiState.update {
            it.copy(
                productImportState = ProductImportState.Idle,
                captureSeed = AlmostBuyDraft(
                    name = safeTitle,
                    amountCents = priceCents.coerceAtLeast(0),
                    category = normalizeCategory(category),
                    trigger = "Shared by a friend",
                    coolingDurationMillis = recommendedCooling(category),
                    sourceUrl = sourceUrl,
                    imageUrl = imageUrl,
                    sourceKind = "ghost_share"
                )
            )
        }
    }

    private fun scheduleDeliveryWorkers(orderId: String, amountSaved: Int, intervalMinutes: Int) {
        val context = getApplication<Application>()
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("ghost_delivery_simulation")
        for (step in 1..4) {
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
    }

    private fun beginDeliveryClock() {
        deliveryJob?.cancel()
        deliveryJob = viewModelScope.launch {
            while (true) {
                val state = _uiState.value
                val step = deliveryStepAt(
                    System.currentTimeMillis(),
                    state.lastOrderPlacedAtMillis,
                    state.simulationIntervalMinutes
                )
                _uiState.update { it.copy(deliveryStep = step) }
                if (step >= 4 || step < 0) break
                delay(1_000L)
            }
        }
    }

    fun resetDeliveryTracking() {
        deliveryJob?.cancel()
        val context = getApplication<Application>()
        WorkManager.getInstance(context).cancelAllWorkByTag("ghost_delivery_simulation")
        sharedPrefs.edit()
            .remove("last_order_id")
            .remove("last_order_total")
            .remove("last_order_placed_at")
            .apply()
        _uiState.update { it.copy(lastOrderId = "", lastOrderTotal = 0, lastOrderPlacedAtMillis = 0L, deliveryStep = -1) }
    }

    fun submitGhostFeedback(orderId: String, rating: Int, comment: String) {
        _uiState.update { it.copy(feedbackSubmittedOrderIds = it.feedbackSubmittedOrderIds + orderId) }
        showToast("Thanks for your feedback!")
    }
}
