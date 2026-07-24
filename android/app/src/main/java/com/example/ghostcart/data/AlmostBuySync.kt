package com.example.ghostcart.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private fun epochMillisToIso(millis: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(java.util.Date(millis))

// org.json.JSONObject.optString(key) returns the literal string "null" for a
// JSON null value (as opposed to a missing key, which returns "") - callers
// that need a real nullable string must filter that out explicitly.
private fun jsonNullableString(value: JSONObject, key: String): String? =
    value.optString(key).takeIf { it.isNotBlank() && it != "null" }

private fun isoToEpochMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return runCatching {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse(iso)?.time
    }.getOrNull()
}

// Best-effort background sync of on-device AlmostBuy items to the real
// backend, purely so the server-side cooldown-expiry cron (which sends the
// FCM push) has something to find. The local SharedPreferences copy stays
// the source of truth for the UI either way - this never blocks capture or
// resolution, and silently no-ops when signed out (push requires an
// account; anonymous/offline captures are never synced or pushed to).
object AlmostBuySync {
    suspend fun syncCreate(context: Context, item: AlmostBuy): String? = withContext(Dispatchers.IO) {
        runCatching {
            val token = AuthRepository.getToken(context) ?: return@withContext null
            createRemote(token, item, coolOffUntilMillis = item.coolingUntilMillis)
        }.getOrNull()
    }

    suspend fun syncResolve(context: Context, serverId: String, resolution: AlmostBuyResolution): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = AuthRepository.getToken(context) ?: return@withContext false
                resolveRemote(token, serverId, resolution)
                true
            }.getOrDefault(false)
        }

    // One-time backfill for items resolved locally before this account ever
    // had a working auth token (or before sync existed at all) - without
    // this, real ghost/skip history a user built up on-device would never
    // show up server-side, and the Community Leaderboard would show 0 for
    // someone who has genuinely ghosted many items. Recreates the item then
    // immediately resolves it with the same outcome it already has locally;
    // never invents or guesses an outcome.
    suspend fun syncResolvedBackfill(context: Context, item: AlmostBuy): String? = withContext(Dispatchers.IO) {
        val outcome = when (item.status) {
            AlmostBuyStatus.SKIPPED -> AlmostBuyResolution.SKIPPED
            AlmostBuyStatus.BOUGHT_INTENTIONALLY -> AlmostBuyResolution.BOUGHT_INTENTIONALLY
            AlmostBuyStatus.COOLING -> return@withContext null
        }
        runCatching {
            val token = AuthRepository.getToken(context) ?: return@withContext null
            // No coolOffUntil here: the backend requires it to be in the
            // future when present, but this item's cooling window is long
            // over - omitting it just creates as "captured", which resolve
            // accepts exactly the same as "cooling".
            val serverId = createRemote(token, item, coolOffUntilMillis = null) ?: return@withContext null
            resolveRemote(token, serverId, outcome)
            serverId
        }.getOrNull()
    }

    private fun createRemote(token: String, item: AlmostBuy, coolOffUntilMillis: Long?): String? {
        val payload = JSONObject().apply {
            put("title", item.name)
            put("category", item.category)
            if (item.trigger.isNotBlank()) put("trigger", item.trigger)
            put("almostSpentCents", item.amountCents)
            put("sourceKind", if (item.sourceUrl != null) "share" else "manual")
            item.sourceUrl?.let { put("sourceUrl", it) }
            item.imageUrl?.let { put("imageUrl", it) }
            coolOffUntilMillis?.let { put("coolOffUntil", epochMillisToIso(it)) }
        }
        val response = authorizedRequest("/api/almost-buys", "POST", token, payload)
        return response.optJSONObject("almostBuy")?.optString("id")?.takeIf { it.isNotBlank() }
    }

    private fun resolveRemote(token: String, serverId: String, resolution: AlmostBuyResolution) {
        val outcome = when (resolution) {
            AlmostBuyResolution.SKIPPED -> "skipped"
            AlmostBuyResolution.BOUGHT_INTENTIONALLY -> "bought"
        }
        authorizedRequest("/api/almost-buys/$serverId/resolve", "POST", token, JSONObject().put("outcome", outcome))
    }

    // Pulls this account's real history down from the server - the counterpart
    // to syncCreate/syncResolve/syncResolvedBackfill, which only ever push
    // local state up. Without this, local storage (SharedPreferences, tied to
    // this specific app install) is the only place cooldown/ghost history
    // ever lives, so a reinstall, a new device, or - as happened this session -
    // switching between differently-signed builds (each one a distinct
    // install to Android) silently loses everything even though the account
    // itself is unchanged. Returns null on any failure (offline, signed out,
    // server error) so callers can treat this as best-effort, same as every
    // other sync method here - local state is never blocked or cleared by a
    // failed pull.
    suspend fun fetchRemote(context: Context): List<AlmostBuy>? = withContext(Dispatchers.IO) {
        runCatching {
            val token = AuthRepository.getToken(context) ?: return@withContext null
            val response = authorizedRequest("/api/almost-buys?limit=100", "GET", token, null)
            val items = response.optJSONArray("almostBuys") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until items.length()) {
                    val value = items.getJSONObject(index)
                    val status = when (value.optString("state")) {
                        "resolved_skipped" -> AlmostBuyStatus.SKIPPED
                        "resolved_bought" -> AlmostBuyStatus.BOUGHT_INTENTIONALLY
                        else -> AlmostBuyStatus.COOLING // captured, cooling, snoozed, expired
                    }
                    val coolOffMillis = isoToEpochMillis(jsonNullableString(value, "coolOffUntil"))
                    val capturedAtMillis = isoToEpochMillis(jsonNullableString(value, "capturedAt"))
                        ?: System.currentTimeMillis()
                    add(
                        AlmostBuy(
                            id = value.getString("id"),
                            name = value.getString("title"),
                            amountCents = value.optLong("almostSpentCents", 0),
                            category = value.optString("category", "Other"),
                            trigger = value.optString("trigger", ""),
                            createdAtMillis = capturedAtMillis,
                            coolingUntilMillis = coolOffMillis ?: capturedAtMillis,
                            status = status,
                            resolvedAtMillis = isoToEpochMillis(jsonNullableString(value, "resolvedAt")),
                            sourceUrl = value.optString("sourceUrl").takeIf { it.isNotBlank() },
                            imageUrl = value.optString("imageUrl").takeIf { it.isNotBlank() },
                            sourceKind = value.optString("sourceKind", "manual"),
                            serverId = value.getString("id")
                        )
                    )
                }
            }
        }.getOrNull()
    }

    suspend fun registerDeviceToken(context: Context, fcmToken: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val token = AuthRepository.getToken(context) ?: return@withContext false
            val payload = JSONObject().apply {
                put("token", fcmToken)
                put("platform", "android")
            }
            authorizedRequest("/api/me/device-tokens", "POST", token, payload)
            true
        }.getOrDefault(false)
    }

    // Records a completed marketplace-cart simulated checkout against the
    // signed-in account so it counts toward "Ghosted" on the Community
    // Leaderboard. Anonymous/signed-out checkouts are never synced here -
    // they're already covered separately (and anonymously) by
    // GhostActivityRepository.recordCheckout for the "Most Ghosted Today"
    // trend, which must stay unattributable to any account.
    suspend fun syncSimulatedOrder(context: Context, orderId: String, totalCents: Int, itemCount: Int): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = AuthRepository.getToken(context) ?: return@withContext false
                val payload = JSONObject().apply {
                    put("orderId", orderId)
                    put("totalCents", totalCents)
                    put("itemCount", itemCount)
                }
                authorizedRequest("/api/me/simulated-orders", "POST", token, payload)
                true
            }.getOrDefault(false)
        }

    private fun authorizedRequest(path: String, method: String, token: String, body: JSONObject?): JSONObject {
        val conn = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            readTimeout = ApiConfig.READ_TIMEOUT_MS
            if (body != null) {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
        }
        try {
            if (body != null) conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val responseCode = conn.responseCode
            val text = (if (responseCode in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (responseCode !in 200..299) {
                throw Exception(json.optString("error", "Request failed ($responseCode)"))
            }
            return json
        } finally {
            conn.disconnect()
        }
    }
}
