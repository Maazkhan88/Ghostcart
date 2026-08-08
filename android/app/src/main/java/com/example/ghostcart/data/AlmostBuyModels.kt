package com.example.ghostcart.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ShareQueueItem(
    val id: String,
    val name: String,
    val amountCents: Long,
    val category: String,
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val brand: String? = null,
    val sourceDomain: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val duplicateAction: String = "none" // "none", "flagged", "merge", "keep_both", "remove"
)

/**
 * The durable product record for Ghost Cart's core loop.
 *
 * Amounts are stored in fils (1/100 of a dirham) so UI rounding never changes
 * a user's progress. An item only contributes to money kept after the user
 * explicitly resolves it as [AlmostBuyStatus.SKIPPED].
 */
data class AlmostBuy(
    val id: String,
    val name: String,
    val amountCents: Long,
    val category: String,
    val trigger: String,
    val createdAtMillis: Long,
    val coolingUntilMillis: Long,
    val status: AlmostBuyStatus = AlmostBuyStatus.COOLING,
    val resolvedAtMillis: Long? = null,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val sourceKind: String = "manual",
    val sharedAnonymously: Boolean = false,
    val serverId: String? = null,
    /** Groups items Ghosted together so Orders can resolve a multi-item cart item-by-item. */
    val ghostOrderId: String? = null,
    /** Stable timing metadata for an accurate countdown bar, including restarted cooldowns. */
    val coolingStartedAtMillis: Long = createdAtMillis,
    val coolingDurationMillis: Long = (coolingUntilMillis - createdAtMillis).coerceAtLeast(60_000L),
    val productId: String? = null,
    val sourceMerchant: String? = null,
    val deliveryState: GhostDeliveryState = GhostDeliveryState.PLACED,
    val deliveryStartedAtMillis: Long = coolingStartedAtMillis,
    val deliveryEndsAtMillis: Long = coolingUntilMillis,
    val selectedDeliveryDurationMillis: Long = coolingDurationMillis,
    val deliveryResolution: GhostOrderResolution? = null,
    val deliveryResolutionAtMillis: Long? = null,
    val routeSeed: Long = id.hashCode().toLong(),
    val tutorialOnly: Boolean = false
)

enum class AlmostBuyStatus {
    COOLING,
    SKIPPED,
    BOUGHT_INTENTIONALLY
}

enum class AlmostBuyResolution {
    SKIPPED,
    BOUGHT_INTENTIONALLY
}

data class AlmostBuyDraft(
    val name: String,
    val amountCents: Long,
    val category: String,
    val trigger: String,
    val coolingDurationMillis: Long,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val sourceKind: String = "manual",
    val shareWithCommunity: Boolean = false,
    val ghostOrderId: String? = null,
    /** Catalog slug/id used only for anonymous aggregate Ghost counts. */
    val activityKey: String? = null,
    val productId: String? = activityKey,
    val sourceMerchant: String? = null,
    val tutorialOnly: Boolean = false
)

data class ProgressSummary(
    val totalAlmostSpentCents: Long,
    val activeCoolingCents: Long,
    val confirmedMoneyKeptCents: Long,
    val activeCount: Int,
    val resolvedCount: Int
)

fun List<AlmostBuy>.progressSummary(): ProgressSummary {
    val realItems = filterNot { it.tutorialOnly }
    return ProgressSummary(
        totalAlmostSpentCents = realItems.sumOf { it.amountCents },
        activeCoolingCents = realItems.filter { it.status == AlmostBuyStatus.COOLING }.sumOf { it.amountCents },
        confirmedMoneyKeptCents = realItems.filter { it.status == AlmostBuyStatus.SKIPPED }.sumOf { it.amountCents },
        activeCount = realItems.count { it.status == AlmostBuyStatus.COOLING },
        resolvedCount = realItems.count { it.status != AlmostBuyStatus.COOLING }
    )
}

/**
 * Repository boundary mirrors the production API contract:
 * GET/POST /api/almost-buys, PATCH /api/almost-buys/:id and
 * POST /api/almost-buys/:id/resolve. The local implementation keeps the app
 * useful offline and can be swapped for an authenticated remote adapter.
 */
interface AlmostBuyRepository {
    val items: Flow<List<AlmostBuy>>
    suspend fun create(draft: AlmostBuyDraft): AlmostBuy
    suspend fun resolve(id: String, resolution: AlmostBuyResolution): AlmostBuy?
    suspend fun resolveDelivery(id: String, resolution: GhostOrderResolution): AlmostBuy?
    suspend fun extendCooling(id: String, durationMillis: Long): AlmostBuy?
    suspend fun clearAll()
    suspend fun attachServerId(id: String, serverId: String)
    /**
     * Merges the account's real server-side history into local state -
     * updates any local item whose serverId matches (server wins once an
     * item is synced, so a resolution made on another device shows up here
     * too), and adds any server item with no local counterpart at all (a
     * fresh install, a new device, or a differently-signed build that lost
     * local storage). Never touches or removes a local item that has no
     * serverId yet - that just means it hasn't been pushed up yet, not that
     * it doesn't belong.
     */
    suspend fun mergeFromServer(remoteItems: List<AlmostBuy>)
}

class LocalAlmostBuyRepository(context: Context) : AlmostBuyRepository {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())

    override val items: Flow<List<AlmostBuy>> = state.asStateFlow()

    override suspend fun create(draft: AlmostBuyDraft): AlmostBuy {
        val now = System.currentTimeMillis()
        val item = AlmostBuy(
            id = UUID.randomUUID().toString(),
            name = draft.name.trim(),
            amountCents = draft.amountCents.coerceAtLeast(0),
            category = draft.category,
            trigger = draft.trigger,
            createdAtMillis = now,
coolingUntilMillis = now + draft.coolingDurationMillis.coerceAtLeast(60_000L),
            sourceUrl = draft.sourceUrl,
            imageUrl = draft.imageUrl,
            sourceKind = draft.sourceKind,
            sharedAnonymously = draft.shareWithCommunity,
            ghostOrderId = draft.ghostOrderId,
            coolingStartedAtMillis = now,
            coolingDurationMillis = draft.coolingDurationMillis.coerceAtLeast(60_000L),
            productId = draft.productId,
            sourceMerchant = draft.sourceMerchant,
            deliveryState = GhostDeliveryState.PLACED,
            deliveryStartedAtMillis = now,
            deliveryEndsAtMillis = now + draft.coolingDurationMillis.coerceAtLeast(60_000L),
            selectedDeliveryDurationMillis = draft.coolingDurationMillis.coerceAtLeast(60_000L),
            tutorialOnly = draft.tutorialOnly
        )
        update(listOf(item) + state.value)
        return item
    }

    override suspend fun resolve(id: String, resolution: AlmostBuyResolution): AlmostBuy? {
        val now = System.currentTimeMillis()
        var result: AlmostBuy? = null
        val next = state.value.map { item ->
            if (item.id != id || item.status != AlmostBuyStatus.COOLING) item else {
                item.copy(
                    status = when (resolution) {
                        AlmostBuyResolution.SKIPPED -> AlmostBuyStatus.SKIPPED
                        AlmostBuyResolution.BOUGHT_INTENTIONALLY -> AlmostBuyStatus.BOUGHT_INTENTIONALLY
                    },
                    resolvedAtMillis = now,
                    deliveryState = when (resolution) {
                        AlmostBuyResolution.SKIPPED -> GhostDeliveryState.RESOLVED_SKIPPED
                        AlmostBuyResolution.BOUGHT_INTENTIONALLY -> GhostDeliveryState.RESOLVED_BOUGHT_ALREADY
                    },
                    deliveryResolution = when (resolution) {
                        AlmostBuyResolution.SKIPPED -> GhostOrderResolution.SKIPPED
                        AlmostBuyResolution.BOUGHT_INTENTIONALLY -> GhostOrderResolution.BOUGHT_ALREADY
                    },
                    deliveryResolutionAtMillis = now
                ).also { result = it }
            }
        }
        if (result != null) update(next)
        return result
    }

    override suspend fun extendCooling(id: String, durationMillis: Long): AlmostBuy? {
        val base = System.currentTimeMillis()
        var result: AlmostBuy? = null
        val next = state.value.map { item ->
            if (item.id != id || item.status != AlmostBuyStatus.COOLING) item else {
                item.copy(
                    coolingUntilMillis = base + durationMillis.coerceAtLeast(60_000L),
                    coolingStartedAtMillis = base,
                    coolingDurationMillis = durationMillis.coerceAtLeast(60_000L),
                    deliveryState = GhostDeliveryState.PLACED,
                    deliveryStartedAtMillis = base,
                    deliveryEndsAtMillis = base + durationMillis.coerceAtLeast(60_000L),
                    selectedDeliveryDurationMillis = durationMillis.coerceAtLeast(60_000L),
                    deliveryResolution = null,
                    deliveryResolutionAtMillis = null
                )
                    .also { result = it }
            }
        }
        if (result != null) update(next)
        return result
    }

    override suspend fun clearAll() {
        update(emptyList())
    }

    override suspend fun attachServerId(id: String, serverId: String) {
        val next = state.value.map { item -> if (item.id == id) item.copy(serverId = serverId) else item }
        update(next)
    }

    override suspend fun mergeFromServer(remoteItems: List<AlmostBuy>) {
        val byServerId = state.value.associateBy { it.serverId }
        val untouched = state.value.filter { it.serverId == null }
        val merged = remoteItems.map { remote ->
            val local = byServerId[remote.serverId]
            // Keep the local item's own id (its identity for notifications/
            // WorkManager tags) even when refreshing its fields from the
            // server; a brand-new item (no local counterpart) uses the
            // server's id directly since nothing local references it yet.
            if (local != null) remote.copy(
                id = local.id,
                productId = local.productId,
                sourceMerchant = local.sourceMerchant,
                deliveryState = if (remote.status == AlmostBuyStatus.COOLING) {
                    ghostDeliverySnapshot(
                        nowMillis = System.currentTimeMillis(),
                        startMillis = remote.coolingStartedAtMillis,
                        endMillis = remote.coolingUntilMillis
                    ).state
                } else remote.deliveryState,
                deliveryStartedAtMillis = remote.coolingStartedAtMillis,
                deliveryEndsAtMillis = remote.coolingUntilMillis,
                selectedDeliveryDurationMillis = remote.coolingDurationMillis,
                routeSeed = local.routeSeed,
                tutorialOnly = false
            ) else remote
        }
        update(untouched + merged)
    }

    private fun update(items: List<AlmostBuy>) {
        state.value = items
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("amountCents", item.amountCents)
                put("category", item.category)
                put("trigger", item.trigger)
                put("createdAtMillis", item.createdAtMillis)
                put("coolingUntilMillis", item.coolingUntilMillis)
                put("status", item.status.name)
item.resolvedAtMillis?.let { put("resolvedAtMillis", it) }
                item.sourceUrl?.let { put("sourceUrl", it) }
                item.imageUrl?.let { put("imageUrl", it) }
                put("sourceKind", item.sourceKind)
                put("sharedAnonymously", item.sharedAnonymously)
                item.serverId?.let { put("serverId", it) }
                item.ghostOrderId?.let { put("ghostOrderId", it) }
                put("coolingStartedAtMillis", item.coolingStartedAtMillis)
                put("coolingDurationMillis", item.coolingDurationMillis)
                item.productId?.let { put("productId", it) }
                item.sourceMerchant?.let { put("sourceMerchant", it) }
                put("deliveryState", item.deliveryState.name)
                put("deliveryStartedAtMillis", item.deliveryStartedAtMillis)
                put("deliveryEndsAtMillis", item.deliveryEndsAtMillis)
                put("selectedDeliveryDurationMillis", item.selectedDeliveryDurationMillis)
                item.deliveryResolution?.let { put("deliveryResolution", it.name) }
                item.deliveryResolutionAtMillis?.let { put("deliveryResolutionAtMillis", it) }
                put("routeSeed", item.routeSeed)
                put("tutorialOnly", item.tutorialOnly)
            })
        }
        preferences.edit().putString(ITEMS_KEY, array.toString()).apply()
    }

    private fun load(): List<AlmostBuy> = runCatching {
        val raw = preferences.getString(ITEMS_KEY, null) ?: return@runCatching emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val value = array.getJSONObject(index)
                add(
                    AlmostBuy(
                        id = value.getString("id"),
                        name = value.getString("name"),
                        amountCents = value.getLong("amountCents"),
                        category = value.getString("category"),
                        trigger = value.getString("trigger"),
                        createdAtMillis = value.getLong("createdAtMillis"),
                        coolingUntilMillis = value.getLong("coolingUntilMillis"),
                        status = AlmostBuyStatus.valueOf(value.optString("status", AlmostBuyStatus.COOLING.name)),
resolvedAtMillis = value.optLong("resolvedAtMillis").takeIf { value.has("resolvedAtMillis") },
                        sourceUrl = value.optString("sourceUrl").takeIf { it.isNotBlank() },
                        imageUrl = value.optString("imageUrl").takeIf { it.isNotBlank() },
                        sourceKind = value.optString("sourceKind", "manual"),
                        sharedAnonymously = value.optBoolean("sharedAnonymously", false),
                        serverId = value.optString("serverId").takeIf { it.isNotBlank() },
                        ghostOrderId = value.optString("ghostOrderId").takeIf { it.isNotBlank() },
                        coolingStartedAtMillis = value.optLong(
                            "coolingStartedAtMillis",
                            value.getLong("createdAtMillis")
                        ),
                        coolingDurationMillis = value.optLong(
                            "coolingDurationMillis",
                            (value.getLong("coolingUntilMillis") - value.getLong("createdAtMillis")).coerceAtLeast(60_000L)
                        ),
                        productId = value.optString("productId").takeIf { it.isNotBlank() },
                        sourceMerchant = value.optString("sourceMerchant").takeIf { it.isNotBlank() },
                        deliveryState = readDeliveryState(value),
                        deliveryStartedAtMillis = value.optLong(
                            "deliveryStartedAtMillis",
                            value.optLong("coolingStartedAtMillis", value.getLong("createdAtMillis"))
                        ),
                        deliveryEndsAtMillis = value.optLong("deliveryEndsAtMillis", value.getLong("coolingUntilMillis")),
                        selectedDeliveryDurationMillis = value.optLong(
                            "selectedDeliveryDurationMillis",
                            value.optLong("coolingDurationMillis", 60_000L)
                        ),
                        deliveryResolution = value.optString("deliveryResolution").takeIf { it.isNotBlank() }
                            ?.let { runCatching { GhostOrderResolution.valueOf(it) }.getOrNull() },
                        deliveryResolutionAtMillis = value.optLong("deliveryResolutionAtMillis")
                            .takeIf { value.has("deliveryResolutionAtMillis") },
                        routeSeed = value.optLong("routeSeed", value.getString("id").hashCode().toLong()),
                        tutorialOnly = value.optBoolean("tutorialOnly", false)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val PREFS_NAME = "ghost_cart_almost_buys"
        const val ITEMS_KEY = "items_v2"
    }

    override suspend fun resolveDelivery(id: String, resolution: GhostOrderResolution): AlmostBuy? {
        val now = System.currentTimeMillis()
        var result: AlmostBuy? = null
        val next = state.value.map { item ->
            if (item.id != id || item.status != AlmostBuyStatus.COOLING) item else {
                item.copy(
                    status = if (resolution == GhostOrderResolution.SKIPPED) {
                        AlmostBuyStatus.SKIPPED
                    } else {
                        AlmostBuyStatus.BOUGHT_INTENTIONALLY
                    },
                    resolvedAtMillis = now,
                    deliveryState = when (resolution) {
                        GhostOrderResolution.SKIPPED -> GhostDeliveryState.RESOLVED_SKIPPED
                        GhostOrderResolution.BUY_FROM_SOURCE -> GhostDeliveryState.RESOLVED_BUY_FROM_SOURCE
                        GhostOrderResolution.BOUGHT_ALREADY -> GhostDeliveryState.RESOLVED_BOUGHT_ALREADY
                    },
                    deliveryResolution = resolution,
                    deliveryResolutionAtMillis = now
                ).also { result = it }
            }
        }
        if (result != null) update(next)
        return result
    }
}

private fun readDeliveryState(value: JSONObject): GhostDeliveryState = runCatching {
    GhostDeliveryState.valueOf(value.optString("deliveryState"))
}.getOrElse {
    when (AlmostBuyStatus.valueOf(value.optString("status", AlmostBuyStatus.COOLING.name))) {
        AlmostBuyStatus.COOLING -> if (value.getLong("coolingUntilMillis") <= System.currentTimeMillis()) {
            GhostDeliveryState.DELIVERED
        } else GhostDeliveryState.PLACED
        AlmostBuyStatus.SKIPPED -> GhostDeliveryState.RESOLVED_SKIPPED
        AlmostBuyStatus.BOUGHT_INTENTIONALLY -> GhostDeliveryState.RESOLVED_BOUGHT_ALREADY
    }
}
