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
    val serverId: String? = null
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
    val shareWithCommunity: Boolean = false
)

data class ProgressSummary(
    val totalAlmostSpentCents: Long,
    val activeCoolingCents: Long,
    val confirmedMoneyKeptCents: Long,
    val activeCount: Int,
    val resolvedCount: Int
)

fun List<AlmostBuy>.progressSummary(): ProgressSummary = ProgressSummary(
    totalAlmostSpentCents = sumOf { it.amountCents },
    activeCoolingCents = filter { it.status == AlmostBuyStatus.COOLING }.sumOf { it.amountCents },
    confirmedMoneyKeptCents = filter { it.status == AlmostBuyStatus.SKIPPED }.sumOf { it.amountCents },
    activeCount = count { it.status == AlmostBuyStatus.COOLING },
    resolvedCount = count { it.status != AlmostBuyStatus.COOLING }
)

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
            sharedAnonymously = draft.shareWithCommunity
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
                    resolvedAtMillis = now
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
                item.copy(coolingUntilMillis = base + durationMillis.coerceAtLeast(60_000L))
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
            if (local != null) remote.copy(id = local.id) else remote
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
                        serverId = value.optString("serverId").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val PREFS_NAME = "ghost_cart_almost_buys"
        const val ITEMS_KEY = "items_v2"
    }
}
