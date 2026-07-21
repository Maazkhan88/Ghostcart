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
            val payload = JSONObject().apply {
                put("title", item.name)
                put("category", item.category)
                if (item.trigger.isNotBlank()) put("trigger", item.trigger)
                put("almostSpentCents", item.amountCents)
                put("sourceKind", if (item.sourceUrl != null) "share" else "manual")
                item.sourceUrl?.let { put("sourceUrl", it) }
                item.imageUrl?.let { put("imageUrl", it) }
                put("coolOffUntil", epochMillisToIso(item.coolingUntilMillis))
            }
            val response = authorizedRequest("/api/almost-buys", "POST", token, payload)
            response.optJSONObject("almostBuy")?.optString("id")?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    suspend fun syncResolve(context: Context, serverId: String, resolution: AlmostBuyResolution): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = AuthRepository.getToken(context) ?: return@withContext false
                val outcome = when (resolution) {
                    AlmostBuyResolution.SKIPPED -> "skipped"
                    AlmostBuyResolution.BOUGHT_INTENTIONALLY -> "bought"
                }
                authorizedRequest("/api/almost-buys/$serverId/resolve", "POST", token, JSONObject().put("outcome", outcome))
                true
            }.getOrDefault(false)
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
