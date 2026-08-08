package com.example.ghostcart.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private fun JSONObject.nullableStringOrNull(key: String): String? =
    if (isNull(key) || !has(key)) null else optString(key).takeIf { it.isNotBlank() }

private const val NOTIFICATIONS_PREFS_NAME = "ghost_cart_prefs"
private const val LAST_SEEN_CREATED_AT_KEY = "notifications_last_seen_created_at"

data class NotificationItem(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val link: String?,
    val createdAt: String
)

// Backend-synced notifications feed: personal rows (delivery, gift) plus
// global/broadcast rows (announcements). There is no server-side read
// state - unread/read is derived locally from a last-seen cursor, see
// AppViewModel. Never call postDeliveryUpdate for lunch/dinner/cooling
// reminders - those stay device-local, per product decision.
object NotificationsRepository {
    suspend fun fetchNotifications(context: Context): Result<List<NotificationItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = AuthRepository.getToken(context) ?: return@runCatching emptyList()
                val conn = (URL("${ApiConfig.BASE_URL}/api/me/notifications").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
                    readTimeout = ApiConfig.READ_TIMEOUT_MS
                }
                val responseCode = conn.responseCode
                val body = if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw Exception("Notifications are temporarily unavailable")
                }
                val rows = JSONObject(body).getJSONArray("notifications")
                buildList {
                    for (i in 0 until rows.length()) {
                        val item = rows.getJSONObject(i)
                        add(
                            NotificationItem(
                                id = item.getString("id"),
                                type = item.getString("type"),
                                title = item.getString("title"),
                                body = item.getString("body"),
                                link = item.nullableStringOrNull("link"),
                                createdAt = item.getString("createdAt")
                            )
                        )
                    }
                }
            }
        }

    // There is no server-side read state (see the schema comment). Unread is
    // "the newest notification's createdAt is later than the last time the
    // user opened this screen on this device" - a local cursor, not synced.
    fun hasUnread(context: Context, notifications: List<NotificationItem>): Boolean {
        val newest = notifications.maxOfOrNull { it.createdAt } ?: return false
        val lastSeen = lastSeenCreatedAt(context) ?: return true
        return newest > lastSeen
    }

    fun markSeen(context: Context, notifications: List<NotificationItem>) {
        val newest = notifications.maxOfOrNull { it.createdAt } ?: return
        context.getSharedPreferences(NOTIFICATIONS_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(LAST_SEEN_CREATED_AT_KEY, newest).apply()
    }

    private fun lastSeenCreatedAt(context: Context): String? =
        context.getSharedPreferences(NOTIFICATIONS_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(LAST_SEEN_CREATED_AT_KEY, null)

    // Best-effort, fire-and-forget: called right after a Ghost Delivery
    // stage-update local notification fires, so the feed has an entry for
    // it too. Never blocks the local notification itself - failures (not
    // signed in, offline) are silently swallowed, matching this codebase's
    // existing sync philosophy elsewhere (AlmostBuySync).
    suspend fun postDeliveryUpdate(context: Context, title: String, body: String, link: String?) {
        withContext(Dispatchers.IO) {
            runCatching {
                val token = AuthRepository.getToken(context) ?: return@runCatching
                val conn = (URL("${ApiConfig.BASE_URL}/api/me/notifications").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
                    readTimeout = ApiConfig.READ_TIMEOUT_MS
                    doOutput = true
                }
                val payload = JSONObject().apply {
                    put("type", "delivery")
                    put("title", title)
                    put("body", body)
                    if (link != null) put("link", link)
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                conn.responseCode
            }
        }
    }
}
