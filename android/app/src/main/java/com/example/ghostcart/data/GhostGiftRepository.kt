package com.example.ghostcart.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GhostGiftDraft(
    val productId: String,
    val recipientName: String,
    val recipientEmail: String,
    val recipientConsentConfirmed: Boolean
)

data class RevealedGhostGift(
    val id: String,
    val senderName: String,
    val title: String,
    val category: String,
    val imageUrl: String?,
    val amountCents: Long,
    val disclosure: String
)

object GhostGiftRepository {
    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun validationError(draft: GhostGiftDraft): String? = when {
        draft.productId.isBlank() -> "Choose an item to send as a Ghost Gift"
        draft.recipientName.trim().isEmpty() -> "Enter the recipient's name"
        draft.recipientName.trim().length > 80 -> "Recipient name is too long"
        !EMAIL_PATTERN.matches(draft.recipientEmail.trim()) -> "Enter a valid recipient email"
        !draft.recipientConsentConfirmed -> "Confirm that the recipient expects this email"
        else -> null
    }

    suspend fun create(
        context: Context,
        serverAlmostBuyId: String,
        draft: GhostGiftDraft
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            validationError(draft)?.let { throw IllegalArgumentException(it) }
            val token = AuthRepository.getToken(context) ?: error("Sign in to send a Ghost Gift")
            val payload = JSONObject()
                .put("almostBuyId", serverAlmostBuyId)
                .put("recipientName", draft.recipientName.trim())
                .put("recipientEmail", draft.recipientEmail.trim().lowercase())
                .put("recipientConsentConfirmed", true)
            authorizedRequest("/api/ghost-gifts", "POST", token, payload)
            Unit
        }
    }

    suspend fun reveal(token: String, acceptedPrivacy: Boolean): Result<RevealedGhostGift> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(token.matches(Regex("^[A-Za-z0-9_-]{43}$"))) { "This Ghost Gift link is invalid" }
                require(acceptedPrivacy) { "Accept the privacy notice to reveal this Ghost Gift" }
                val response = request(
                    "/api/ghost-gifts/reveal",
                    "POST",
                    null,
                    JSONObject().put("token", token).put("acceptedPrivacy", true)
                )
                val gift = response.getJSONObject("ghostGift")
                RevealedGhostGift(
                    id = gift.getString("id"),
                    senderName = gift.optString("senderName", "A Ghost Cart member"),
                    title = gift.getString("title"),
                    category = gift.optString("category", "Other"),
                    imageUrl = gift.optString("imageUrl").takeIf { it.isNotBlank() && it != "null" },
                    amountCents = gift.optLong("amountCents", 0L).coerceAtLeast(0L),
                    disclosure = gift.optString(
                        "disclosure",
                        "Simulation only. No gift was purchased or sent. No real payment or delivery occurred."
                    )
                )
            }
        }

    private fun authorizedRequest(path: String, method: String, token: String, body: JSONObject): JSONObject =
        request(path, method, token, body)

    private fun request(path: String, method: String, token: String?, body: JSONObject?): JSONObject {
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            readTimeout = ApiConfig.READ_TIMEOUT_MS
            if (body != null) {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
        }
        return try {
            body?.let { value -> connection.outputStream.use { it.write(value.toString().toByteArray()) } }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status !in 200..299) throw Exception(json.optString("error", "Request failed ($status)"))
            json
        } finally {
            connection.disconnect()
        }
    }
}
