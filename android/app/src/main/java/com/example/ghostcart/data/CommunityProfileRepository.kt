package com.example.ghostcart.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Thrown by [CommunityProfileRepository.fetchLeaderboardDetail] specifically for a 404 - the
 * member has never opted into (or has since opted out of) the public leaderboard, as opposed to
 * a transient network/server failure. */
class LeaderboardMemberNotFoundException(message: String) : Exception(message)

data class UserProfile(
    val email: String,
    val displayName: String?,
    val username: String?,
    val avatarUrl: String?,
    val communityConsent: Boolean,
    val gender: String?,
    val avatarPresetId: String?,
    val showRecentActivityPublicly: Boolean
)

data class LeaderboardRecentItem(
    val name: String,
    val category: String,
    val amountCents: Long,
    val capturedAt: String,
    val imageUrl: String?
)

data class LeaderboardActivityEntry(
    val label: String,
    val amountCents: Long,
    val createdAt: String
)

data class LeaderboardMonthStats(
    val ghostedCount: Int,
    val savedCents: Long,
    val coolingCount: Int,
    val ghostedCountChangePercent: Int?,
    val savedCentsChangePercent: Int?
)

data class LeaderboardDetail(
    val username: String,
    val avatarUrl: String?,
    val avatarPresetId: String?,
    val rank: Int,
    val moneyKeptCents: Long,
    val savedCount: Int,
    val ghostedCount: Int,
    val ghostedAmountCents: Long,
    val coolingCount: Int,
    val activityPrivate: Boolean,
    val currentStreakDays: Int,
    val achievements: List<String>,
    val recentGhostedItems: List<LeaderboardRecentItem>,
    val recentActivity: List<LeaderboardActivityEntry>,
    val thisMonth: LeaderboardMonthStats?
)

// "Cooled & saved" = an almost-buy explicitly resolved "skipped" after
// cooling off (the impulse-resistance win this board ranks by). "Ghosted" =
// the separate, unrelated case of actually finishing checkout on an
// almost-buy (resolved_bought) - do not conflate the two.
data class LeaderboardEntry(
    val username: String,
    val avatarUrl: String?,
    val avatarPresetId: String?,
    val moneyKeptCents: Long,
    val savedCount: Int,
    val ghostedCount: Int,
    val ghostedAmountCents: Long,
    val coolingCount: Int
)

private fun JSONObject.nullableStringOrNull(key: String): String? =
    if (isNull(key) || !has(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun parseProfile(json: JSONObject): UserProfile = UserProfile(
    email = json.optString("email"),
    displayName = json.nullableStringOrNull("displayName"),
    username = json.nullableStringOrNull("username"),
    avatarUrl = json.nullableStringOrNull("avatarUrl")?.let { "${ApiConfig.BASE_URL}$it" },
    communityConsent = json.optBoolean("communityConsent", false),
    gender = json.nullableStringOrNull("gender"),
    avatarPresetId = json.nullableStringOrNull("avatarPresetId"),
    showRecentActivityPublicly = json.optBoolean("showRecentActivityPublicly", false)
)

private fun optIntOrNull(json: JSONObject, key: String): Int? = if (json.isNull(key) || !json.has(key)) null else json.optInt(key)

private fun parseLeaderboardDetail(json: JSONObject): LeaderboardDetail {
    val profile = json.getJSONObject("profile")
    val activityPrivate = json.optBoolean("activityPrivate", true)
    val thisMonthJson = json.optJSONObject("thisMonth")
    return LeaderboardDetail(
        username = profile.getString("username"),
        avatarUrl = profile.nullableStringOrNull("avatarUrl")?.let { "${ApiConfig.BASE_URL}$it" },
        avatarPresetId = profile.nullableStringOrNull("avatarPresetId"),
        rank = profile.optInt("rank", 0),
        moneyKeptCents = profile.optLong("moneyKeptCents", 0),
        savedCount = profile.optInt("savedCount", 0),
        ghostedCount = profile.optInt("ghostedCount", 0),
        ghostedAmountCents = profile.optLong("ghostedAmountCents", 0),
        coolingCount = profile.optInt("coolingCount", 0),
        activityPrivate = activityPrivate,
        currentStreakDays = json.optInt("currentStreakDays", 0),
        achievements = json.optJSONArray("achievements")?.let { array ->
            buildList { for (i in 0 until array.length()) add(array.getString(i)) }
        } ?: emptyList(),
        recentGhostedItems = json.optJSONArray("recentGhostedItems")?.let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        LeaderboardRecentItem(
                            name = item.getString("name"),
                            category = item.optString("category", "Other"),
                            amountCents = item.optLong("amountCents", 0),
                            capturedAt = item.optString("capturedAt"),
                            imageUrl = item.nullableStringOrNull("imageUrl")
                        )
                    )
                }
            }
        } ?: emptyList(),
        recentActivity = json.optJSONArray("recentActivity")?.let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        LeaderboardActivityEntry(
                            label = item.getString("label"),
                            amountCents = item.optLong("amountCents", 0),
                            createdAt = item.optString("createdAt")
                        )
                    )
                }
            }
        } ?: emptyList(),
        thisMonth = thisMonthJson?.let {
            LeaderboardMonthStats(
                ghostedCount = it.optInt("ghostedCount", 0),
                savedCents = it.optLong("savedCents", 0),
                coolingCount = it.optInt("coolingCount", 0),
                ghostedCountChangePercent = optIntOrNull(it, "ghostedCountChangePercent"),
                savedCentsChangePercent = optIntOrNull(it, "savedCentsChangePercent")
            )
        }
    )
}

// Every call here requires the bearer token AuthRepository persists on
// sign-in - these endpoints 401 without it, unlike the rest of
// ProductImportRepository's mostly-anonymous calls.
object CommunityProfileRepository {
    suspend fun fetchProfile(context: Context): Result<UserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val token = AuthRepository.getToken(context) ?: throw IllegalStateException("Not signed in")
            val json = authorizedRequest("/api/me/profile", "GET", token, null)
            parseProfile(json.getJSONObject("profile"))
        }
    }

    suspend fun updateProfile(
        context: Context,
        displayName: String? = null,
        username: String? = null,
        communityConsent: Boolean? = null,
        gender: String? = null,
        avatarPresetId: String? = null,
        showRecentActivityPublicly: Boolean? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val token = AuthRepository.getToken(context) ?: throw IllegalStateException("Not signed in")
            val payload = JSONObject().apply {
                if (displayName != null) put("displayName", displayName)
                if (username != null) put("username", username)
                if (communityConsent != null) put("communityConsent", communityConsent)
                if (gender != null) put("gender", gender)
                if (avatarPresetId != null) put("avatarPresetId", avatarPresetId)
                if (showRecentActivityPublicly != null) put("showRecentActivityPublicly", showRecentActivityPublicly)
            }
            val json = authorizedRequest("/api/me/profile", "PATCH", token, payload)
            parseProfile(json.getJSONObject("profile"))
        }
    }

    suspend fun fetchLeaderboardDetail(username: String): Result<LeaderboardDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("${ApiConfig.BASE_URL}/api/community/leaderboard/${java.net.URLEncoder.encode(username, "UTF-8")}")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            conn.readTimeout = ApiConfig.READ_TIMEOUT_MS

            val responseCode = conn.responseCode
            val text = (if (responseCode in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (responseCode == 404) {
                throw LeaderboardMemberNotFoundException(
                    json.optString("error", "This member is not on the public leaderboard.")
                )
            }
            if (responseCode !in 200..299) {
                throw Exception(json.optString("error", "This member's details are unavailable"))
            }
            parseLeaderboardDetail(json)
        }
    }

    suspend fun uploadAvatar(context: Context, bytes: ByteArray, mimeType: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = AuthRepository.getToken(context) ?: throw IllegalStateException("Not signed in")
                val boundary = "GhostCartAvatar${UUID.randomUUID()}"
                val extension = if (mimeType == "image/png") "png" else "jpg"
                val url = URL("${ApiConfig.BASE_URL}/api/me/avatar")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
                conn.readTimeout = ApiConfig.READ_TIMEOUT_MS
                conn.doOutput = true

                conn.outputStream.use { output ->
                    writeMultipartFile(output, boundary, "file", "avatar.$extension", mimeType, bytes)
                }

                val responseCode = conn.responseCode
                val body = if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error code $responseCode"
                    val message = try { JSONObject(error).getString("error") } catch (e: Exception) { error }
                    throw Exception(message)
                }
                val relativeUrl = JSONObject(body).getString("avatarUrl")
                "${ApiConfig.BASE_URL}$relativeUrl"
            }
        }

    suspend fun fetchLeaderboard(): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("${ApiConfig.BASE_URL}/api/community/leaderboard")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            conn.readTimeout = ApiConfig.READ_TIMEOUT_MS

            val responseCode = conn.responseCode
            val body = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("Leaderboard is temporarily unavailable")
            }
            val entries = JSONObject(body).getJSONArray("leaderboard")
            buildList {
                for (i in 0 until entries.length()) {
                    val item = entries.getJSONObject(i)
                    add(
                        LeaderboardEntry(
                            username = item.getString("username"),
                            avatarUrl = item.nullableStringOrNull("avatarUrl")?.let { "${ApiConfig.BASE_URL}$it" },
                            avatarPresetId = item.nullableStringOrNull("avatarPresetId"),
                            moneyKeptCents = item.optLong("moneyKeptCents", 0),
                            savedCount = item.optInt("savedCount", 0),
                            ghostedCount = item.optInt("ghostedCount", 0),
                            ghostedAmountCents = item.optLong("ghostedAmountCents", 0),
                            coolingCount = item.optInt("coolingCount", 0)
                        )
                    )
                }
            }
        }
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

    private fun writeMultipartFile(
        output: OutputStream,
        boundary: String,
        fieldName: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray
    ) {
        val header = buildString {
            append("--$boundary\r\n")
            append("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$filename\"\r\n")
            append("Content-Type: $mimeType\r\n\r\n")
        }
        output.write(header.toByteArray())
        output.write(bytes)
        output.write("\r\n--$boundary--\r\n".toByteArray())
    }
}
