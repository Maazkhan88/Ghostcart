package com.example.ghostcart.ui.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ghostcart.app.BuildConfig
import com.example.ghostcart.data.CoolingReminderWorker
import com.example.ghostcart.data.DailyGhostReminderWorker
import com.example.ghostcart.data.AlmostBuy
import com.example.ghostcart.data.AlmostBuyDraft
import com.example.ghostcart.data.AlmostBuyStatus
import com.example.ghostcart.data.AlmostBuyRepository
import com.example.ghostcart.data.AlmostBuyResolution
import com.example.ghostcart.data.AlmostBuySync
import com.example.ghostcart.data.Analytics
import com.example.ghostcart.data.AuthRepository
import com.example.ghostcart.data.CommunityProfileRepository
import com.example.ghostcart.data.ContentBlockItem
import com.example.ghostcart.data.AppUpdateInfo
import com.example.ghostcart.data.LeaderboardDetail
import com.example.ghostcart.data.LeaderboardEntry
import com.example.ghostcart.data.UserProfile
import com.example.ghostcart.data.LocalAlmostBuyRepository
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.data.WalletConfig
import com.example.ghostcart.data.WalletDemoData
import com.example.ghostcart.data.GhostDeliveryScheduler
import com.example.ghostcart.data.GhostDeliveryState
import com.example.ghostcart.data.GhostOrderResolution
import com.example.ghostcart.data.GhostActivityRepository
import com.example.ghostcart.data.GhostRanking
import com.example.ghostcart.data.GhostCardImageExporter
import com.example.ghostcart.data.FavoriteRepository
import com.example.ghostcart.data.InvoiceData
import com.example.ghostcart.data.InvoiceEmailRepository
import com.example.ghostcart.data.InvoiceLineItem
import com.example.ghostcart.data.InvoicePdfExporter
import com.example.ghostcart.data.UpdateDownloader
import com.example.ghostcart.data.UpdateRepository
import com.example.ghostcart.data.GhostGiftDraft
import com.example.ghostcart.data.GhostGiftRepository
import com.example.ghostcart.data.RevealedGhostGift
import com.example.ghostcart.data.CommunityProduct
import com.example.ghostcart.data.DeviceLinkPreview
import com.example.ghostcart.data.mergeDeviceMetadata
import com.example.ghostcart.data.LinkImportResult
import com.example.ghostcart.data.ListingProductStub
import com.example.ghostcart.data.InAppMessage
import com.example.ghostcart.data.InAppMessageRepository
import com.example.ghostcart.data.SimulationConsentRepository
import com.example.ghostcart.data.SimulationConsentStatus
import com.example.ghostcart.data.ProductImportRepository
import com.example.ghostcart.data.ProductImportState
import com.example.ghostcart.data.fetchSharedGhostItem
import com.example.ghostcart.data.registerCurrentFcmToken
import com.example.ghostcart.data.ShareQueueItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val lastOrderProducts: List<MarketplaceProduct> = emptyList(),
    val lastOrderPlacedAtMillis: Long = 0L,
    val lastOrderItemsWithQty: List<Pair<MarketplaceProduct, Int>> = emptyList(),
    val lastOrderDeliveryAddress: String = "",
    val lastOrderSubtotal: Int = 0,
    val lastOrderPromoDiscount: Int = 0,
    val lastOrderServiceFee: Int = 0,
    val lastOrderVat: Int = 0,
    val deliveryStep: Int = -1,
    val promoApplied: Boolean = true,
    val authEmail: String? = null,
    val authRequiredPrompt: Boolean = false,
    val simulationIntervalMinutes: Int = 5,
    val hasAppliedForCard: Boolean = false,
    val isApplying: Boolean = false,
    val mostGhostedToday: List<GhostRanking> = emptyList(),
    val ghostCountsByProductId: Map<String, Int> = emptyMap(),
    val isMostGhostedLoading: Boolean = true,
    val isMostGhostedUnavailable: Boolean = false,
    val toastMessage: String? = null,
    val almostBuys: List<AlmostBuy> = emptyList(),
    val productImportState: ProductImportState = ProductImportState.Idle,
    val communityProducts: List<CommunityProduct> = emptyList(),
    val communityProductsLoading: Boolean = true,
    /** Admin-managed catalog from /api/products. Null = not yet loaded or the fetch failed,
     * in which case the bundled fallback catalog is used instead - never an empty app. */
    val catalogProducts: List<MarketplaceProduct>? = null,
    val homeBanners: List<ContentBlockItem> = emptyList(),
    val ghostCartStories: List<ContentBlockItem> = emptyList(),
    val profile: UserProfile? = null,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val leaderboardLoading: Boolean = false,
    val leaderboardDetail: LeaderboardDetail? = null,
    val leaderboardDetailLoading: Boolean = false,
    val availableUpdate: AppUpdateInfo? = null,
    val updateDismissed: Boolean = false,
    val updateDownloading: Boolean = false,
    val profileSaving: Boolean = false,
    val profileError: String? = null,
    val captureSeed: AlmostBuyDraft? = null,
    val appTheme: String = "System",
    val favoriteProductIds: Set<String> = emptySet(),
    val feedbackSubmittedOrderIds: Set<String> = emptySet(),
    val shareQueue: List<ShareQueueItem> = emptyList(),
    val simulationConsentStatus: SimulationConsentStatus? = null,
    val activeInAppMessage: InAppMessage? = null
)

// Mirrors CheckoutFlowScreens.kt's rates exactly - kept in sync there since
// this is where the invoice's breakdown is recomputed after the cart clears.
private const val CHECKOUT_SERVICE_FEE_RATE = 0.05f
private const val CHECKOUT_VAT_RATE = 0.05f
private const val CHECKOUT_PROMO_RATE = 0.10f

internal fun deliveryStepAt(nowMillis: Long, placedAtMillis: Long, intervalMinutes: Int): Int {
    if (placedAtMillis <= 0L) return -1
    val stepDuration = intervalMinutes.coerceAtLeast(1) * 60_000L
    return ((nowMillis - placedAtMillis).coerceAtLeast(0L) / stepDuration).toInt().coerceIn(0, 4)
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("ghost_cart_prefs", Context.MODE_PRIVATE)
    private val almostBuyRepository: AlmostBuyRepository = LocalAlmostBuyRepository(application)

    init {
        // Self-heal a stale sign-in: accounts that signed in before the
        // auth-token fix (or whose session simply expired) have an
        // auth_email remembered locally with no real token behind it - every
        // authenticated call (profile, avatar, leaderboard opt-in) would
        // silently fail with "Not signed in" while the app still claims to
        // be signed in. Clearing both here forces a real re-sign-in, which
        // now correctly saves a token.
        if (sharedPrefs.getString("auth_email", null) != null && AuthRepository.getToken(application) == null) {
            sharedPrefs.edit().remove("auth_email").apply()
        }
    }

    private val _uiState = MutableStateFlow(AppUiState(
        authEmail = sharedPrefs.getString("auth_email", null),
        walletConfig = loadWalletConfig(),
        coolingUntilByProductId = loadCoolingPeriods(),
        appTheme = sharedPrefs.getString("app_theme", "System") ?: "System",
        favoriteProductIds = loadFavoriteProductIds(),
        cartQuantities = loadCartQuantities(),
        cartProductIds = loadCartQuantities().keys.toList(),
        shareQueue = loadShareQueue(),
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
        refreshCatalogProducts()
        refreshHomeBanners()
        refreshGhostCartStories()
        refreshProfile()
        refreshLeaderboard()
        checkForUpdate()
        syncDailyGhostReminder("lunch", 13, _uiState.value.walletConfig.lunchReminderEnabled)
        syncDailyGhostReminder("dinner", 20, _uiState.value.walletConfig.dinnerReminderEnabled)
        if (_uiState.value.deliveryStep in 0..3) beginDeliveryClock()
        viewModelScope.launch {
            GhostDeliveryScheduler.restore(application, almostBuyRepository.items.first())
        }
        viewModelScope.launch {
            almostBuyRepository.items.collect { items ->
                _uiState.update { it.copy(almostBuys = items) }
            }
        }
        refreshSimulationConsentStatus()
        refreshInAppMessages()
        if (_uiState.value.authEmail != null) {
            viewModelScope.launch { registerCurrentFcmToken(getApplication()) }
            backfillUnsyncedHistory()
            hydrateFromServer()
        }
    }

    private fun refreshSimulationConsentStatus() {
        viewModelScope.launch {
            SimulationConsentRepository.fetchStatus(getApplication())
                .onSuccess { status ->
                    _uiState.update { it.copy(simulationConsentStatus = status) }
                }
                .onFailure {
                    // Offline/unreachable: fail open rather than stranding the user on a splash
                    // screen forever. version=0 never matches a real published version, so if
                    // connectivity returns later this placeholder is simply replaced by the real
                    // status on the next app start - it never gets treated as a genuine
                    // acceptance record server-side (nothing is ever POSTed for it).
                    _uiState.update {
                        it.copy(simulationConsentStatus = SimulationConsentStatus(0, "", accepted = true))
                    }
                }
        }
    }

    /** Only ever called from the consent screen's own explicit "I understand" button - never
     * inferred or auto-accepted on the user's behalf. */
    fun acceptSimulationConsent() {
        val status = _uiState.value.simulationConsentStatus ?: return
        viewModelScope.launch {
            SimulationConsentRepository.submitAcceptance(getApplication(), status.version)
                .onSuccess {
                    _uiState.update { it.copy(simulationConsentStatus = status.copy(accepted = true)) }
                }
                .onFailure {
                    showToast("Could not record your acceptance - check your connection and try again.")
                }
        }
    }

    private fun refreshInAppMessages() {
        viewModelScope.launch {
            val dismissedIds = sharedPrefs.getStringSet("dismissed_in_app_message_ids", emptySet()).orEmpty()
            InAppMessageRepository.fetchActiveMessages().onSuccess { messages ->
                val next = messages.firstOrNull { it.id !in dismissedIds }
                _uiState.update { it.copy(activeInAppMessage = next) }
            }
        }
    }

    fun dismissInAppMessage(id: String) {
        val current = sharedPrefs.getStringSet("dismissed_in_app_message_ids", emptySet()).orEmpty()
        sharedPrefs.edit().putStringSet("dismissed_in_app_message_ids", current + id).apply()
        _uiState.update {
            if (it.activeInAppMessage?.id == id) it.copy(activeInAppMessage = null) else it
        }
    }

    // Bundled catalog used only as an offline/failed-fetch fallback - the admin-managed
    // /api/products catalog (this exact list, seeded once into D1) is the real source of
    // truth now, so admin add/edit/remove actually shows up in the app.
    private val bundledFallbackCatalog: List<MarketplaceProduct> =
        (Marketplace.featuredCatalog + Marketplace.discoveryCatalog + Marketplace.fakeFlashDeals + Marketplace.foodAndCoffeeCatalog + Marketplace.merchCatalog)
            .distinctBy { it.id }

    val allProducts: List<MarketplaceProduct>
        get() = _uiState.value.catalogProducts ?: bundledFallbackCatalog

    fun findProduct(id: String): MarketplaceProduct? =
        allProducts.find { it.id == id }
            ?: sharedCartProducts[id]
            ?: id.removePrefix("community_").takeIf { id.startsWith("community_") }
                ?.let { communityId ->
                    _uiState.value.communityProducts.find { it.id == communityId }
                        ?.let(::communityMarketplaceProduct)
                        ?.also { sharedCartProducts[it.id] = it }
                }

    fun setAppTheme(theme: String) {
        val normalized = theme.takeIf { it in setOf("System", "Light", "Dark") } ?: "System"
        sharedPrefs.edit().putString("app_theme", normalized).apply()
        _uiState.update { it.copy(appTheme = normalized) }
    }

    fun toggleFavorite(productId: String) {
        if (findProduct(productId) == null) return
        val nowFavorited = productId !in _uiState.value.favoriteProductIds
        _uiState.update { current ->
            val next = if (nowFavorited) current.favoriteProductIds + productId else current.favoriteProductIds - productId
            sharedPrefs.edit()
                .putStringSet("favorite_product_ids", HashSet(next))
                .putString("favorite_product_ids_v2", next.joinToString("\n"))
                .apply()
            current.copy(favoriteProductIds = next)
        }
        if (nowFavorited) Analytics.logGhostOrderEvent(getApplication(), "product_favorited")
        // Best-effort - a signed-out user's favorites stay local-only, and a
        // failed request here just means the next sign-in's merge catches up.
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (nowFavorited) FavoriteRepository.add(context, productId) else FavoriteRepository.remove(context, productId)
        }
    }

    private fun loadCartQuantities(): Map<String, Int> =
        sharedPrefs.getString("cart_quantities_v1", "")
            .orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val separator = line.lastIndexOf('|')
                if (separator <= 0) return@mapNotNull null
                val qty = line.substring(separator + 1).toIntOrNull() ?: return@mapNotNull null
                line.substring(0, separator) to qty
            }
            .toMap()

    private fun persistCartQuantities(quantities: Map<String, Int>) {
        sharedPrefs.edit()
            .putString("cart_quantities_v1", quantities.entries.joinToString("\n") { "${it.key}|${it.value}" })
            .apply()
    }

    private fun loadFavoriteProductIds(): Set<String> {
        val legacy = sharedPrefs.getStringSet("favorite_product_ids", emptySet()).orEmpty().toSet()
        val stable = sharedPrefs.getString("favorite_product_ids_v2", "")
            .orEmpty()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
        return legacy + stable
    }

    private fun communityMarketplaceProduct(source: CommunityProduct) = MarketplaceProduct(
        id = "community_${source.id}",
        name = source.title,
        category = normalizeCategory(source.category),
        price = (source.priceCents / 100L).toInt().coerceAtLeast(0),
        iconName = "gadget",
        // The real brand of a community-shared item isn't reliably known - source domain (e.g.
        // "amazon.ae") is a separate signal and must never be shown/used as if it were the brand.
        brand = null,
        sourceDomain = source.sourceDomain,
        isUserGhosted = true,
        lastGhostedAtMillis = source.lastGhostedAtMillis,
        imageUrl = source.imageUrl,
        sourceUrl = source.sourceUrl,
        ghostCount = source.ghostCount
    )

    fun communityMarketplaceProducts(): List<MarketplaceProduct> =
        _uiState.value.communityProducts.map { source ->
            communityMarketplaceProduct(source).also { sharedCartProducts[it.id] = it }
        }

    /**
     * Catalog + community products combined into one list, with likely duplicates (the same
     * underlying product appearing in both) collapsed into a single card rather than shown twice.
     * When a duplicate is found, the curated catalog entry is kept (more complete data) but
     * flagged as also user-ghosted, with the higher of the two ghost counts.
     */
    fun unifiedMarketplaceProducts(): List<MarketplaceProduct> {
        val community = communityMarketplaceProducts()
        val usedCommunityIds = mutableSetOf<String>()
        val merged = allProducts.map { catalogItem ->
            val duplicate = community.firstOrNull { it.id !in usedCommunityIds && isLikelyDuplicateProduct(catalogItem, it) }
            if (duplicate != null) {
                usedCommunityIds += duplicate.id
                catalogItem.copy(
                    isUserGhosted = true,
                    ghostCount = maxOf(catalogItem.ghostCount, duplicate.ghostCount),
                    lastGhostedAtMillis = duplicate.lastGhostedAtMillis ?: catalogItem.lastGhostedAtMillis
                )
            } else {
                catalogItem
            }
        }
        // User-ghosted items surface first in the marketplace, ahead of the curated catalog -
        // sortedByDescending is stable, so relative order within each group (catalog order,
        // community recency) is preserved.
        return (merged + community.filterNot { it.id in usedCommunityIds })
            .sortedByDescending { it.isUserGhosted }
    }

    // Cooling and ghosting require an account - anonymous users can browse
    // and add to cart freely, but any action that creates something durable
    // (an almost-buy) needs somewhere real to belong to. Checkout has its
    // own explicit gate in placeSimulatedOrder rather than relying on this
    // indirectly through createAlmostBuy, since by the time that ran the
    // wallet balance and order state had already been mutated. Sets a
    // one-shot flag Navigation.kt observes to push the sign-in screen,
    // mirroring the existing checkout sign-in gate rather than introducing a
    // second pattern.
    private fun requireSignIn(): Boolean {
        if (_uiState.value.authEmail != null) return true
        _uiState.update { it.copy(authRequiredPrompt = true) }
        showToast("Sign in to cool, ghost, or check out")
        return false
    }

    fun consumeAuthRequiredPrompt() {
        _uiState.update { it.copy(authRequiredPrompt = false) }
    }

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
        Analytics.logCaptureCompleted(getApplication(), draft.sourceKind, product.category)
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

    /** Leaves catalogProducts null (falling back to the bundled catalog) on failure - never
     * strands the user with an empty marketplace because of one bad network call. */
    fun refreshCatalogProducts() {
        viewModelScope.launch {
            ProductImportRepository.fetchCatalogProducts()
                .onSuccess { fetched ->
                    if (fetched.isNotEmpty()) _uiState.update { it.copy(catalogProducts = fetched) }
                }
        }
    }

    fun refreshHomeBanners() {
        viewModelScope.launch {
            ProductImportRepository.fetchContentBlocks("banner")
                .onSuccess { banners -> _uiState.update { it.copy(homeBanners = banners) } }
        }
    }

    fun refreshGhostCartStories() {
        viewModelScope.launch {
            ProductImportRepository.fetchContentBlocks("story")
                .onSuccess { stories -> _uiState.update { it.copy(ghostCartStories = stories) } }
        }
    }

    fun refreshProfile() {
        if (_uiState.value.authEmail == null) return
        viewModelScope.launch {
            CommunityProfileRepository.fetchProfile(getApplication())
                .onSuccess { profile -> _uiState.update { it.copy(profile = profile) } }
        }
    }

    // One-time catch-up for accounts whose real ghost/skip history exists
    // only on-device (captured before this account ever had a working auth
    // token, or before server sync existed at all) - without this, someone
    // who has genuinely ghosted many items would show 0 on the Community
    // Leaderboard, since it's computed purely from server-side almost_buys.
    // Reads straight from the repository Flow rather than _uiState.almostBuys
    // to avoid racing the separate collector that populates it.
    private fun backfillUnsyncedHistory() {
        viewModelScope.launch {
            val unsynced = almostBuyRepository.items.first().filter {
                it.status != AlmostBuyStatus.COOLING && it.serverId == null
            }
            var backfilled = false
            for (item in unsynced) {
                AlmostBuySync.syncResolvedBackfill(getApplication(), item)?.let { serverId ->
                    almostBuyRepository.attachServerId(item.id, serverId)
                    backfilled = true
                }
            }
            if (backfilled) refreshLeaderboard()
        }
    }

    // Pulls this account's real cooldown/ghost history down from the server -
    // without this, local SharedPreferences storage (tied to this exact app
    // install) was the *only* place that history ever lived, so a reinstall,
    // a new device, or switching between differently-signed builds silently
    // lost everything even though the account itself never changed. Runs
    // after backfillUnsyncedHistory() (push first, so nothing local-only is
    // at risk, then pull the now-complete picture).
    private fun hydrateFromServer() {
        viewModelScope.launch {
            val remote = AlmostBuySync.fetchRemote(getApplication()) ?: return@launch
            if (remote.isNotEmpty()) almostBuyRepository.mergeFromServer(remote)
        }
        viewModelScope.launch { syncFavoritesWithServer() }
    }

    /** Two-way merge on sign-in: anything favorited locally before this
     * feature existed (or while offline) gets pushed up, anything already on
     * the server gets pulled down - a plain union, since unfavoriting is rare
     * enough that "favorited on either side wins" is the right default. */
    private suspend fun syncFavoritesWithServer() {
        val context = getApplication<Application>()
        val remoteIds = FavoriteRepository.list(context).getOrNull() ?: return
        val localIds = _uiState.value.favoriteProductIds
        val merged = localIds + remoteIds
        if (merged != localIds) {
            sharedPrefs.edit()
                .putStringSet("favorite_product_ids", HashSet(merged))
                .putString("favorite_product_ids_v2", merged.joinToString("\n"))
                .apply()
            _uiState.update { it.copy(favoriteProductIds = merged) }
        }
        (localIds - remoteIds).forEach { productId -> FavoriteRepository.add(context, productId) }
    }

    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(profileSaving = true, profileError = null) }
        viewModelScope.launch {
            CommunityProfileRepository.updateProfile(getApplication(), displayName = name)
                .onSuccess { profile -> _uiState.update { it.copy(profile = profile, profileSaving = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(profileSaving = false, profileError = error.message ?: "Could not save name.") }
                }
        }
    }

    /** Opting out (consent = false) keeps the username reserved - re-opting in later
     * keeps the same leaderboard identity rather than forcing a new username. */
    fun setCommunityLeaderboardOptIn(username: String?, consent: Boolean) {
        _uiState.update { it.copy(profileSaving = true, profileError = null) }
        viewModelScope.launch {
            CommunityProfileRepository.updateProfile(getApplication(), username = username, communityConsent = consent)
                .onSuccess { profile ->
                    _uiState.update { it.copy(profile = profile, profileSaving = false) }
                    Analytics.logLeaderboardOptIn(getApplication(), consent)
                    if (consent) refreshLeaderboard()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(profileSaving = false, profileError = error.message ?: "Could not update leaderboard settings.") }
                }
        }
    }

    fun setShowRecentActivityPublicly(show: Boolean) {
        _uiState.update { it.copy(profileSaving = true, profileError = null) }
        viewModelScope.launch {
            CommunityProfileRepository.updateProfile(getApplication(), showRecentActivityPublicly = show)
                .onSuccess { profile -> _uiState.update { it.copy(profile = profile, profileSaving = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(profileSaving = false, profileError = error.message ?: "Could not update this setting.") }
                }
        }
    }

    fun selectAvatarPreset(presetId: String) {
        _uiState.update { it.copy(profileSaving = true, profileError = null) }
        viewModelScope.launch {
            CommunityProfileRepository.updateProfile(getApplication(), avatarPresetId = presetId)
                .onSuccess { profile -> _uiState.update { it.copy(profile = profile, profileSaving = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(profileSaving = false, profileError = error.message ?: "Could not update avatar.") }
                }
        }
    }

    fun uploadAvatar(bytes: ByteArray, mimeType: String) {
        _uiState.update { it.copy(profileSaving = true, profileError = null) }
        viewModelScope.launch {
            CommunityProfileRepository.uploadAvatar(getApplication(), bytes, mimeType)
                .onSuccess { refreshProfile() }
                .onFailure { error ->
                    _uiState.update { it.copy(profileSaving = false, profileError = error.message ?: "Could not upload photo.") }
                }
            _uiState.update { it.copy(profileSaving = false) }
        }
    }

    fun refreshLeaderboard() {
        _uiState.update { it.copy(leaderboardLoading = true) }
        viewModelScope.launch {
            CommunityProfileRepository.fetchLeaderboard()
                .onSuccess { entries -> _uiState.update { it.copy(leaderboard = entries, leaderboardLoading = false) } }
                .onFailure { _uiState.update { it.copy(leaderboardLoading = false) } }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            UpdateRepository.checkForUpdate()
                .onSuccess { info -> _uiState.update { it.copy(availableUpdate = info) } }
        }
    }

    fun dismissUpdateBanner() {
        _uiState.update { it.copy(updateDismissed = true) }
    }

    fun downloadAndInstallUpdate() {
        val info = _uiState.value.availableUpdate ?: return
        _uiState.update { it.copy(updateDownloading = true) }
        viewModelScope.launch {
            UpdateDownloader.downloadAndPromptInstall(getApplication(), info.apkUrl, info.latestVersionName)
                .onSuccess { _uiState.update { it.copy(updateDownloading = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(updateDownloading = false) }
                    showToast(error.message ?: "Couldn't download the update")
                }
        }
    }

    fun openLeaderboardDetail(username: String) {
        _uiState.update { it.copy(leaderboardDetail = null, leaderboardDetailLoading = true) }
        viewModelScope.launch {
            CommunityProfileRepository.fetchLeaderboardDetail(username)
                .onSuccess { detail -> _uiState.update { it.copy(leaderboardDetail = detail, leaderboardDetailLoading = false) } }
                .onFailure { _uiState.update { it.copy(leaderboardDetailLoading = false) } }
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
            // Belt-and-suspenders on top of the status check below: an isBlank()
            // guard here (not just == null) means this can never silently skip
            // the WebView fallback just because some upstream layer stored ""
            // instead of null for a missing image.
            val missingImage = serverProduct.imageUrl.isNullOrBlank()
            val missingPrice = serverProduct.priceCents == null
            val statusIncomplete = serverProduct.status != "complete"
            val needsDevicePreview = missingImage || missingPrice || statusIncomplete
            Log.d(
                "ProductImport",
                "capture pipeline: host=${serverProduct.sourceDomain} afterServerAndDeviceFetch " +
                    "image=${!missingImage} price=${!missingPrice} status=${serverProduct.status} " +
                    "needsWebViewFallback=$needsDevicePreview (missingImage=$missingImage missingPrice=$missingPrice statusIncomplete=$statusIncomplete)"
            )
            val deviceMetadata = if (needsDevicePreview) {
                DeviceLinkPreview.read(getApplication(), serverProduct.sourceUrl)
            } else {
                null
            }
            val product = deviceMetadata?.let { mergeDeviceMetadata(serverProduct, it) } ?: serverProduct
            Log.d(
                "ProductImport",
                "capture pipeline: host=${serverProduct.sourceDomain} FINAL image=${!product.imageUrl.isNullOrBlank()} status=${product.status}"
            )
            val amountCents = if (product.currencyCode == null || product.currencyCode == "AED") product.priceCents ?: 0 else 0

            // First share (nothing else pending): show the normal single-item "Ghost an
            // almost-buy" screen, pre-filled and editable, same as any other capture. Only once
            // a second share arrives while the first hasn't been confirmed yet does this become
            // a queue - see migratePendingCaptureSeedToQueue().
            val hasPendingShare = _uiState.value.shareQueue.isNotEmpty() ||
                _uiState.value.captureSeed?.sourceKind == "share"
            if (hasPendingShare) {
                migratePendingCaptureSeedToQueue()
                val item = ShareQueueItem(
                    id = UUID.randomUUID().toString(),
                    name = product.title,
                    amountCents = amountCents,
                    category = importedCategory(product.category, product.sourceDomain, product.sourceUrl),
                    imageUrl = product.imageUrl,
                    sourceUrl = product.sourceUrl,
                    brand = null,
                    sourceDomain = product.sourceDomain
                )
                appendToShareQueue(item)
            } else {
                _uiState.update {
                    it.copy(
                        productImportState = ProductImportState.Idle,
                        captureSeed = AlmostBuyDraft(
                            name = product.title,
                            amountCents = amountCents,
                            category = importedCategory(product.category, product.sourceDomain, product.sourceUrl),
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
    }

    /** Moves an in-progress single-share draft (if any) into the share queue as its first item,
     * so a second concurrent share can join it there instead of silently replacing it. */
    private fun migratePendingCaptureSeedToQueue() {
        val seed = _uiState.value.captureSeed?.takeIf { it.sourceKind == "share" } ?: return
        val item = ShareQueueItem(
            id = UUID.randomUUID().toString(),
            name = seed.name,
            amountCents = seed.amountCents,
            category = seed.category,
            imageUrl = seed.imageUrl,
            sourceUrl = seed.sourceUrl
        )
        val current = _uiState.value.shareQueue
        val finalItem = if (current.any { isLikelyDuplicate(it, item) }) item.copy(duplicateAction = "flagged") else item.copy(duplicateAction = "none")
        val updated = current + finalItem
        saveShareQueue(updated)
        _uiState.update { it.copy(shareQueue = updated, captureSeed = null) }
    }

    fun addListingItemsToCart(items: List<ListingProductStub>) {
        addListingItemsToShareQueue(items)
    }

    fun clearProductImport() {
        _uiState.update { it.copy(productImportState = ProductImportState.Idle, captureSeed = null) }
    }

    fun clearCaptureSeed() {
        _uiState.update { it.copy(captureSeed = null, productImportState = ProductImportState.Idle) }
    }

    /**
     * True when [a] and [b] are likely the same underlying product, checked in priority order:
     * normalized source URL match, then normalized title (+ brand tiebreaker when both known).
     * A "merchant product ID" tier isn't implemented - neither the catalog nor the community
     * data model carries one. An exact image-URL match is only ever used to corroborate a close
     * (not exact) title match, never as a duplicate signal by itself.
     */
    private fun isLikelyDuplicateProduct(a: MarketplaceProduct, b: MarketplaceProduct): Boolean {
        val urlA = normalizedUrlForDedup(a.sourceUrl)
        val urlB = normalizedUrlForDedup(b.sourceUrl)
        if (urlA != null && urlB != null && urlA == urlB) return true

        val titleA = normalizedTextForDedup(a.name)
        val titleB = normalizedTextForDedup(b.name)
        if (titleA != null && titleA == titleB) {
            val brandA = normalizedTextForDedup(a.brand)
            val brandB = normalizedTextForDedup(b.brand)
            return if (brandA != null && brandB != null) brandA == brandB else true
        }

        if (a.imageUrl != null && a.imageUrl == b.imageUrl && titleA != null && titleB != null) {
            val wordsA = titleA.split(" ").filter(String::isNotBlank).toSet()
            val wordsB = titleB.split(" ").filter(String::isNotBlank).toSet()
            val smaller = minOf(wordsA.size, wordsB.size)
            if (smaller > 0 && wordsA.intersect(wordsB).size.toDouble() / smaller >= 0.6) return true
        }

        return false
    }

    private fun normalizedTextForDedup(value: String?): String? =
        value?.lowercase()?.replace(Regex("[^a-z0-9]+"), " ")?.trim()?.takeIf { it.isNotBlank() }

    private fun normalizedUrlForDedup(value: String?): String? = value?.let { url ->
        runCatching {
            val parsed = java.net.URL(url)
            "${parsed.host.lowercase().removePrefix("www.")}${parsed.path.trimEnd('/')}"
        }.getOrNull()
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

    private fun importedCategory(category: String, sourceDomain: String?, sourceUrl: String?): String {
        val host = sourceDomain?.lowercase().orEmpty()
        val url = sourceUrl?.lowercase().orEmpty()
        val foodHost = listOf(
            "talabat.com",
            "deliveroo.ae",
            "deliveroo.com",
            "careem.com",
            "ubereats.com",
            "keeta.com",
            "keeta-global.com"
        ).any { root -> host == root || host.endsWith(".$root") }
        val noonFood = (host == "noon.com" || host.endsWith(".noon.com")) && "/food" in url
        return if (foodHost || noonFood) "Food & drinks" else normalizeCategory(category)
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
        // Persist server-side (not just this in-memory pick) so it survives a
        // reinstall/device swap and is available later as the default avatar.
        // Fire-and-forget: a failure here shouldn't block onboarding, since
        // the local selection already drives the picker's UI immediately.
        if (_uiState.value.authEmail != null) {
            viewModelScope.launch {
                CommunityProfileRepository.updateProfile(getApplication(), gender = profile.lowercase())
                    .onSuccess { updated -> _uiState.update { it.copy(profile = updated) } }
            }
        }
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
            persistCartQuantities(nextMap)
            current.copy(
                cartQuantities = nextMap,
                cartProductIds = nextMap.keys.toList()
            )
        }
        val product = findProduct(productId)
        Analytics.logAddToCart(getApplication(), productId, product?.category ?: "Other")
        showToast("Added ${product?.name ?: "item"} to Ghost Cart")
    }

    fun removeFromCart(productId: String) {
        _uiState.update { current ->
            val nextQty = (current.cartQuantities[productId] ?: 0) - 1
            val nextMap = if (nextQty <= 0) {
                current.cartQuantities - productId
            } else {
                current.cartQuantities + (productId to nextQty)
            }
            persistCartQuantities(nextMap)
            current.copy(
                cartQuantities = nextMap,
                cartProductIds = nextMap.keys.toList()
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
            persistCartQuantities(nextMap)
            current.copy(
                cartQuantities = nextMap,
                cartProductIds = nextMap.keys.toList()
            )
        }
    }

    fun clearCart() {
        persistCartQuantities(emptyMap())
        _uiState.update { it.copy(cartProductIds = emptyList(), cartQuantities = emptyMap()) }
    }

    fun refreshMostGhostedToday() {
        _uiState.update { it.copy(isMostGhostedLoading = true, isMostGhostedUnavailable = false) }
        viewModelScope.launch {
            GhostActivityRepository.activitySnapshot()
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            mostGhostedToday = snapshot.rankings,
                            ghostCountsByProductId = snapshot.itemCounts,
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

    // Called the moment POST_NOTIFICATIONS is actually granted from Home's ambient
    // first-run request (not the per-toggle asks in Profile, which already handle
    // their own single reminder). Without this, granting the permission did nothing -
    // lunch/dinner reminders stayed off until the user found and flipped them
    // manually in Profile, even though they'd just said yes to notifications.
    fun enableMealRemindersByDefault() {
        val config = _uiState.value.walletConfig
        if (!config.lunchReminderEnabled || !config.dinnerReminderEnabled) {
            updateWalletConfig { it.copy(lunchReminderEnabled = true, dinnerReminderEnabled = true) }
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

    private fun buildLastOrderInvoice(): InvoiceData {
        val current = _uiState.value
        return InvoiceData(
            orderId = current.lastOrderId,
            placedAtMillis = current.lastOrderPlacedAtMillis,
            customerName = current.profile?.displayName.orEmpty(),
            customerEmail = current.authEmail.orEmpty(),
            deliveryAddress = current.lastOrderDeliveryAddress,
            items = current.lastOrderItemsWithQty.map { (product, qty) ->
                InvoiceLineItem(name = product.name, quantity = qty, lineTotal = product.price * qty)
            },
            subtotal = current.lastOrderSubtotal,
            promoDiscount = current.lastOrderPromoDiscount,
            serviceFee = current.lastOrderServiceFee,
            vat = current.lastOrderVat,
            total = current.lastOrderTotal
        )
    }

    fun downloadInvoice() {
        showToast("Creating invoice…")
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) { InvoicePdfExporter.generate(context, buildLastOrderInvoice()) }
            result.onSuccess {
                showToast("Invoice saved to Downloads/Ghost Cart")
            }.onFailure {
                showToast("Couldn't create the invoice")
            }
        }
    }

    fun shareInvoice() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) { InvoicePdfExporter.generate(context, buildLastOrderInvoice()) }
            result.onSuccess { uri ->
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Your Ghost Cart invoice")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share invoice").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }.onFailure {
                showToast("Couldn't create the invoice")
            }
        }
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
        // A self-rescheduling one-time chain (each firing recomputes the exact delay to the
        // next occurrence of hourOfDay from the actual current time) instead of a bare 24h
        // PeriodicWorkRequest, which WorkManager/Doze can defer and then "catch up" on later,
        // drifting the effective fire time away from the configured hour over several days.
        // DailyGhostReminderWorker re-enqueues itself via this same unique work name after it runs.
        val request = OneTimeWorkRequestBuilder<DailyGhostReminderWorker>()
            .setInitialDelay(delayUntilHour(hourOfDay), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("meal" to meal, "hourOfDay" to hourOfDay))
            .addTag("ghost_daily_reminder")
            .build()
        workManager.enqueueUniqueWork(
            "ghost_daily_$meal",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Debug-only verification hook: fires the given reminder ~90 seconds out instead of
     * waiting for the configured hour, so scheduling behavior can be checked on a real
     * device in minutes rather than over a multi-day observation window. No-op in release
     * builds.
     */
    fun scheduleDailyGhostReminderForDebugVerification(meal: String, hourOfDay: Int) {
        if (!BuildConfig.DEBUG) return
        // The worker checks lunch_reminder_enabled/dinner_reminder_enabled before showing
        // anything (see DailyGhostReminderWorker) - without this, tapping the debug button
        // while the real toggle is off makes the worker silently no-op (still "succeeds",
        // just never posts), which looks identical to the notification failing to fire.
        // Route through updateWalletConfig (not a raw SharedPreferences write) so the in-memory
        // state and the visible toggle switch stay consistent with what actually got persisted.
        if (meal == "lunch") {
            updateWalletConfig { it.copy(lunchReminderEnabled = true) }
        } else {
            updateWalletConfig { it.copy(dinnerReminderEnabled = true) }
        }
        val workManager = WorkManager.getInstance(getApplication<Application>())
        val request = OneTimeWorkRequestBuilder<DailyGhostReminderWorker>()
            .setInitialDelay(90, TimeUnit.SECONDS)
            .setInputData(workDataOf("meal" to meal, "hourOfDay" to hourOfDay))
            .addTag("ghost_daily_reminder_debug")
            .build()
        workManager.enqueueUniqueWork("ghost_daily_debug_$meal", ExistingWorkPolicy.REPLACE, request)
    }

    private fun delayUntilHour(hourOfDay: Int): Long = delayUntilHourFrom(Calendar.getInstance(), hourOfDay)

    private fun loadWalletConfig(): WalletConfig {
        val simulatedBalance = if (!sharedPrefs.getBoolean("wallet_balance_v2_initialized", false)) {
            10000.also {
                sharedPrefs.edit()
                    .putInt("wallet_starting_balance", it)
                    .putBoolean("wallet_balance_v2_initialized", true)
                    .apply()
            }
        } else {
            sharedPrefs.getInt("wallet_starting_balance", 10000)
        }
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
            startingBalance = simulatedBalance,
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
        Analytics.logSignIn(getApplication(), "app")
        refreshProfile()
        viewModelScope.launch { registerCurrentFcmToken(getApplication()) }
        backfillUnsyncedHistory()
        hydrateFromServer()
    }

    fun signOut() {
        sharedPrefs.edit().remove("auth_email").remove("auth_token").apply()
        _uiState.update { it.copy(authEmail = null, profile = null) }
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

    fun createAlmostBuy(
        draft: AlmostBuyDraft,
        onCreated: (AlmostBuy) -> Unit = {},
        onServerSynced: suspend (String) -> Unit = {},
        onServerSyncFailed: () -> Unit = {}
    ) {
        if (!requireSignIn()) return
        viewModelScope.launch {
            val groupedDraft = if (draft.ghostOrderId.isNullOrBlank()) {
                draft.copy(ghostOrderId = "GO-${UUID.randomUUID()}")
            } else draft
            val item = almostBuyRepository.create(groupedDraft)
            Analytics.logCoolingStarted(getApplication(), groupedDraft.sourceKind, groupedDraft.category)
            Analytics.logGhostOrderEvent(
                getApplication(),
                "ghost_order_placed",
                item.ghostOrderId ?: item.id,
                GhostDeliveryState.PLACED.name,
                item.selectedDeliveryDurationMillis
            )
            GhostDeliveryScheduler.schedule(getApplication(), item)
            groupedDraft.activityKey?.takeIf { it.isNotBlank() }?.let { activityKey ->
                launch {
                    GhostActivityRepository.recordGhostStart(
                        eventId = item.id,
                        productIds = listOf(activityKey)
                    ).onSuccess { refreshMostGhostedToday() }
                }
            }
            if (groupedDraft.shareWithCommunity) {
                ProductImportRepository.publish(groupedDraft)
                    .onSuccess { refreshCommunityProducts() }
                    .onFailure { showToast("Item is cooling; anonymous sharing could not be completed") }
            }
            showToast("Ghost Order placed · ${item.name}")
            onCreated(item)
            // Best-effort: mirrors this cooldown to the backend so the server-side
            // sweep can push a notification when it expires. No-ops silently when
            // signed out; the local cooldown/notification already work either way.
            val context = getApplication<Application>()
            launch {
                val serverId = AlmostBuySync.syncCreate(context, item)
                if (serverId != null) {
                    almostBuyRepository.attachServerId(item.id, serverId)
                    onServerSynced(serverId)
                } else {
                    onServerSyncFailed()
                }
            }
        }
    }

    fun resolveAlmostBuy(id: String, resolution: AlmostBuyResolution) {
        viewModelScope.launch {
            val item = almostBuyRepository.resolve(id, resolution) ?: return@launch
            Analytics.logCooldownResolved(getApplication(), resolution.name.lowercase())
            WorkManager.getInstance(getApplication<Application>())
                .cancelUniqueWork("ghost_cooling_${item.id}")
            GhostDeliveryScheduler.cancel(getApplication(), item.id)
            item.serverId?.let { serverId ->
                launch { AlmostBuySync.syncResolve(getApplication(), serverId, resolution) }
            }
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
            Analytics.logGhostOrderEvent(
                getApplication(),
                "decision_restarted",
                item.ghostOrderId ?: item.id,
                GhostDeliveryState.RESTARTED.name,
                durationMillis
            )
            GhostDeliveryScheduler.schedule(getApplication(), item)
            item.serverId?.let { serverId ->
                launch { AlmostBuySync.syncExtend(getApplication(), serverId, item.coolingUntilMillis) }
            }
            showToast("Cooldown restarted")
        }
    }

    fun resolveGhostDelivery(id: String, resolution: GhostOrderResolution) {
        viewModelScope.launch {
            val item = almostBuyRepository.resolveDelivery(id, resolution) ?: return@launch
            Analytics.logGhostOrderEvent(
                getApplication(),
                when (resolution) {
                    GhostOrderResolution.SKIPPED -> "decision_skipped"
                    GhostOrderResolution.BUY_FROM_SOURCE -> "decision_buy_from_source"
                    GhostOrderResolution.BOUGHT_ALREADY -> "decision_bought_already"
                },
                item.ghostOrderId ?: item.id,
                item.deliveryState.name
            )
            GhostDeliveryScheduler.cancel(getApplication(), item.id)
            WorkManager.getInstance(getApplication<Application>()).cancelUniqueWork("ghost_cooling_${item.id}")
            item.serverId?.let { serverId ->
                val legacyResolution = if (resolution == GhostOrderResolution.SKIPPED) {
                    AlmostBuyResolution.SKIPPED
                } else {
                    AlmostBuyResolution.BOUGHT_INTENTIONALLY
                }
                launch { AlmostBuySync.syncResolve(getApplication(), serverId, legacyResolution) }
            }
            showToast(
                when (resolution) {
                    GhostOrderResolution.SKIPPED -> "Added to confirmed Money Kept"
                    GhostOrderResolution.BUY_FROM_SOURCE -> "Opening the original source"
                    GhostOrderResolution.BOUGHT_ALREADY -> "Decision recorded without adding Money Kept"
                }
            )
        }
    }

    fun findGhostOrder(id: String): AlmostBuy? =
        _uiState.value.almostBuys.firstOrNull { it.id == id || it.ghostOrderId == id }

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

    fun placeSimulatedOrder(
        deliveryDurationMillis: Long,
        checkoutTotal: Int = cartSubtotal(),
        ghostGift: GhostGiftDraft? = null,
        deliveryAddress: String = ""
    ): Boolean {
        if (!requireSignIn()) return false
        val total = checkoutTotal.coerceAtLeast(0)
        if (_uiState.value.cartQuantities.isEmpty() || total <= 0) {
            showToast("Add an item before checkout")
            return false
        }
        val simulatedBalance = _uiState.value.walletConfig.startingBalance
        if (total > simulatedBalance) {
            showToast("Not enough simulated balance. Add more in Ghost Wallet.")
            return false
        }
        ghostGift?.let { draft ->
            GhostGiftRepository.validationError(draft)?.let { error ->
                showToast(error)
                return false
            }
            if (cartProducts().none { it.id == draft.productId }) {
                showToast("Choose a Ghost Cart item for the gift")
                return false
            }
        }
        updateWalletConfig { it.copy(startingBalance = simulatedBalance - total) }
        val orderId = "GHOST-${10000 + Random.nextInt(90000)}"
        val placedAt = System.currentTimeMillis()
        val safeDeliveryDuration = deliveryDurationMillis.coerceAtLeast(TimeUnit.MINUTES.toMillis(15))
        val itemsWithQty = cartProductsWithQuantities()
        val ghostedProducts = itemsWithQty.map { it.first }.distinctBy { it.id }
        val ghostedItemCount = itemsWithQty.sumOf { it.second }
        val orderSubtotal = itemsWithQty.sumOf { (product, qty) -> product.price * qty }
        val orderPromoDiscount = (orderSubtotal * CHECKOUT_PROMO_RATE).toInt()
        val orderServiceFee = ((orderSubtotal - orderPromoDiscount) * CHECKOUT_SERVICE_FEE_RATE).toInt()
        val orderVat = ((orderSubtotal - orderPromoDiscount) * CHECKOUT_VAT_RATE).toInt()
        val orderGroupId = orderId
        Analytics.logGhostOrderEvent(
            getApplication(),
            "delivery_duration_selected",
            orderGroupId,
            GhostDeliveryState.PLACED.name,
            safeDeliveryDuration
        )
        ghostedProducts.forEach { product ->
            val giftForProduct = ghostGift?.takeIf { it.productId == product.id }
            createAlmostBuy(
                AlmostBuyDraft(
                    name = product.name,
                    amountCents = product.price.toLong() * 100,
                    category = normalizeCategory(product.category),
                    trigger = "FOMO",
                    coolingDurationMillis = safeDeliveryDuration,
                    sourceUrl = product.sourceUrl,
                    imageUrl = product.imageUrl,
                    sourceKind = "catalog",
                    ghostOrderId = orderGroupId,
                    activityKey = product.id,
                    productId = product.id,
                    sourceMerchant = product.sourceDomain ?: product.brand
                ),
                onServerSynced = { serverId ->
                    if (giftForProduct != null) {
                        GhostGiftRepository.create(getApplication(), serverId, giftForProduct)
                            .onSuccess { showToast("Gift email sent") }
                            .onFailure { showToast(it.message ?: "Ghosted, but the gift email could not be sent") }
                    }
                },
                onServerSyncFailed = {
                    if (giftForProduct != null) {
                        showToast("Item is cooling, but the gift email could not be sent")
                    }
                }
            )
        }
        val updatedCoolingPeriods = _uiState.value.coolingUntilByProductId +
            ghostedProducts.associate { it.id to (placedAt + safeDeliveryDuration) }
        persistCoolingPeriods(updatedCoolingPeriods)
        persistCartQuantities(emptyMap())
        _uiState.update {
            it.copy(
                lastOrderId = orderId,
                lastOrderTotal = total,
                lastOrderProducts = ghostedProducts,
                lastOrderPlacedAtMillis = placedAt,
                lastOrderItemsWithQty = itemsWithQty,
                lastOrderDeliveryAddress = deliveryAddress,
                lastOrderSubtotal = orderSubtotal,
                lastOrderPromoDiscount = orderPromoDiscount,
                lastOrderServiceFee = orderServiceFee,
                lastOrderVat = orderVat,
                deliveryStep = -1,
                coolingUntilByProductId = updatedCoolingPeriods,
                cartProductIds = emptyList(),
                cartQuantities = emptyMap()
            )
        }
        sharedPrefs.edit()
            .putString("last_order_id", orderId)
            .putInt("last_order_total", total)
            .putLong("last_order_placed_at", placedAt)
            .apply()
        Analytics.logCheckoutCompleted(getApplication(), ghostedItemCount, total * 100)
        showToast(if (ghostGift == null) "Fake Checkout complete" else "Fake Checkout complete. Sending gift…")

        refreshCommunityProducts()
        viewModelScope.launch { InvoiceEmailRepository.sendInvoiceEmail(getApplication(), buildLastOrderInvoice()) }
        return true
    }

    fun ghostRevealedGift(gift: RevealedGhostGift, onCreated: () -> Unit = {}) {
        createAlmostBuy(
            AlmostBuyDraft(
                name = gift.title,
                amountCents = gift.amountCents,
                category = normalizeCategory(gift.category),
                trigger = "Gift",
                coolingDurationMillis = 24L * 60L * 60L * 1000L,
                imageUrl = gift.imageUrl,
                sourceKind = "share"
            ),
            onCreated = { onCreated() }
        )
    }

    fun addSimulatedWalletBalance(amount: Int) {
        val safeAmount = amount.coerceIn(1, 1_000_000)
        updateWalletConfig { config ->
            config.copy(startingBalance = (config.startingBalance + safeAmount).coerceAtMost(1_000_000))
        }
        showToast("Added AED $safeAmount in simulated balance")
    }

    fun deleteAccountAndLocalData() {
        WorkManager.getInstance(getApplication<Application>()).cancelAllWorkByTag("ghost_cooling_reminder")
        WorkManager.getInstance(getApplication<Application>()).cancelAllWorkByTag("ghost_daily_reminder")
        GhostDeliveryScheduler.cancelAll(getApplication())
        sharedPrefs.edit().clear().commit()
        sharedCartProducts.clear()
        _uiState.value = AppUiState(walletConfig = loadWalletConfig())
        viewModelScope.launch { almostBuyRepository.clearAll() }
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

    fun importGhostShare(shareId: String) {
        _uiState.update { it.copy(productImportState = ProductImportState.Loading) }
        viewModelScope.launch {
            fetchSharedGhostItem(shareId)
                .onSuccess { item ->
                    prepareSharedGhostItem(
                        title = item.title,
                        priceCents = item.priceCents,
                        category = item.category,
                        imageUrl = item.imageUrl,
                        sourceUrl = item.sourceUrl
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(productImportState = ProductImportState.Error(error.message ?: "Shared item is unavailable"))
                    }
                }
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

    private fun loadShareQueue(): List<ShareQueueItem> = runCatching {
        val raw = sharedPrefs.getString("share_queue", null) ?: return@runCatching emptyList()
        val array = org.json.JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(ShareQueueItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    amountCents = obj.getLong("amountCents"),
                    category = obj.getString("category"),
                    imageUrl = obj.optString("imageUrl").takeIf { it.isNotBlank() },
                    sourceUrl = obj.optString("sourceUrl").takeIf { it.isNotBlank() },
                    brand = obj.optString("brand").takeIf { it.isNotBlank() },
                    sourceDomain = obj.optString("sourceDomain").takeIf { it.isNotBlank() },
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    duplicateAction = obj.optString("duplicateAction", "none")
                ))
            }
        }
    }.getOrDefault(emptyList())

    private fun saveShareQueue(queue: List<ShareQueueItem>) {
        val array = org.json.JSONArray()
        queue.forEach { item ->
            array.put(org.json.JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("amountCents", item.amountCents)
                put("category", item.category)
                item.imageUrl?.let { put("imageUrl", it) }
                item.sourceUrl?.let { put("sourceUrl", it) }
                item.brand?.let { put("brand", it) }
                item.sourceDomain?.let { put("sourceDomain", it) }
                put("timestamp", item.timestamp)
                put("duplicateAction", item.duplicateAction)
            })
        }
        sharedPrefs.edit().putString("share_queue", array.toString()).apply()
    }

    fun isLikelyDuplicate(a: ShareQueueItem, b: ShareQueueItem): Boolean {
        val pA = MarketplaceProduct(id = a.id, name = a.name, category = a.category, price = 0, iconName = "", brand = a.brand, sourceUrl = a.sourceUrl, imageUrl = a.imageUrl)
        val pB = MarketplaceProduct(id = b.id, name = b.name, category = b.category, price = 0, iconName = "", brand = b.brand, sourceUrl = b.sourceUrl, imageUrl = b.imageUrl)
        return isLikelyDuplicateProduct(pA, pB)
    }

    fun appendToShareQueue(item: ShareQueueItem) {
        val current = _uiState.value.shareQueue
        if (current.size >= 20) {
            showToast("Share queue is full (max 20 items)")
            _uiState.update { it.copy(productImportState = ProductImportState.Idle) }
            return
        }
        val hasDuplicate = current.any { isLikelyDuplicate(it, item) }
        val finalItem = if (hasDuplicate) {
            item.copy(duplicateAction = "flagged")
        } else {
            item.copy(duplicateAction = "none")
        }
        val updated = current + finalItem
        saveShareQueue(updated)
        _uiState.update {
            it.copy(
                shareQueue = updated,
                productImportState = ProductImportState.Idle
            )
        }
        showToast("Added to share queue")
    }

    fun addListingItemsToShareQueue(items: List<ListingProductStub>) {
        val current = _uiState.value.shareQueue.toMutableList()
        var addedCount = 0
        items.forEach { stub ->
            if (current.size >= 20) {
                return@forEach
            }
            val item = ShareQueueItem(
                id = UUID.randomUUID().toString(),
                name = stub.title,
                amountCents = if (stub.currencyCode == null || stub.currencyCode == "AED") stub.priceCents ?: 0 else 0,
                category = normalizeCategory(stub.category),
                imageUrl = stub.imageUrl,
                sourceUrl = stub.sourceUrl,
                sourceDomain = stub.sourceDomain
            )
            val hasDuplicate = current.any { isLikelyDuplicate(it, item) }
            val finalItem = if (hasDuplicate) {
                item.copy(duplicateAction = "flagged")
            } else {
                item.copy(duplicateAction = "none")
            }
            current.add(finalItem)
            addedCount++
        }
        saveShareQueue(current)
        _uiState.update {
            it.copy(
                shareQueue = current,
                productImportState = ProductImportState.Idle
            )
        }
        if (addedCount > 0) {
            showToast("Added $addedCount items to share queue")
        }
    }

    fun updateShareQueueItem(updated: ShareQueueItem) {
        val current = _uiState.value.shareQueue.map {
            if (it.id == updated.id) updated else it
        }
        saveShareQueue(current)
        _uiState.update { it.copy(shareQueue = current) }
    }

    fun removeShareQueueItem(id: String) {
        val current = _uiState.value.shareQueue.filterNot { it.id == id }
        saveShareQueue(current)
        _uiState.update { it.copy(shareQueue = current) }
    }

    fun clearShareQueue() {
        saveShareQueue(emptyList())
        _uiState.update { it.copy(shareQueue = emptyList()) }
    }

    fun bulkConfirmShareQueue(shareWithCommunity: Boolean) {
        val queue = _uiState.value.shareQueue
        val activeItems = queue.filter { it.duplicateAction != "remove" }
        
        if (activeItems.any { it.duplicateAction == "flagged" }) {
            showToast("Please resolve all duplicate items before confirming.")
            return
        }

        activeItems.forEach { item ->
            addDraftToCart(
                AlmostBuyDraft(
                    name = item.name,
                    amountCents = item.amountCents,
                    category = normalizeCategory(item.category),
                    trigger = "FOMO",
                    coolingDurationMillis = recommendedCooling(item.category),
                    sourceUrl = item.sourceUrl,
                    imageUrl = item.imageUrl,
                    sourceKind = "share",
                    shareWithCommunity = shareWithCommunity
                )
            )
        }

        clearShareQueue()
        showToast(
            if (activeItems.size == 1) "Added 1 item to Ghost Cart" else "Added ${activeItems.size} items to Ghost Cart"
        )
    }

    /** Bulk Ghost action; the primary flow supplies the canonical 24-hour default. */
    fun bulkCoolShareQueue(shareWithCommunity: Boolean, coolingDurationMillis: Long) {
        if (!requireSignIn()) return
        val queue = _uiState.value.shareQueue
        val activeItems = queue.filter { it.duplicateAction != "remove" }

        if (activeItems.any { it.duplicateAction == "flagged" }) {
            showToast("Please resolve all duplicate items before cooling.")
            return
        }

        val orderGroupId = "GO-${UUID.randomUUID()}"
        activeItems.forEach { item ->
            createAlmostBuy(
                AlmostBuyDraft(
                    name = item.name,
                    amountCents = item.amountCents,
                    category = normalizeCategory(item.category),
                    trigger = "FOMO",
                    coolingDurationMillis = coolingDurationMillis,
                    sourceUrl = item.sourceUrl,
                    imageUrl = item.imageUrl,
                    sourceKind = "share",
                    shareWithCommunity = shareWithCommunity,
                    ghostOrderId = orderGroupId
                )
            )
        }

        clearShareQueue()
    }
}
