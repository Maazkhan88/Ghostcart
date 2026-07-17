package com.example.ghostcart.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private val URL_PATTERN = Regex("https://[^\\s]+", RegexOption.IGNORE_CASE)

fun extractSupportedRetailerUrl(sharedText: String?): String? = sharedText
    ?.let { URL_PATTERN.findAll(it).map { match -> match.value.trimEnd('.', ',', ')', ']') }.firstOrNull { url ->
        runCatching {
            val host = URL(url).host.lowercase()
            host == "amazon.ae" || host.endsWith(".amazon.ae") ||
                host == "amazon.com" || host.endsWith(".amazon.com") ||
                host == "amzn.eu" || host.endsWith(".amzn.eu") ||
                host == "noon.com" || host.endsWith(".noon.com")
        }.getOrDefault(false)
    }
}

data class ImportedProduct(
    val title: String,
    val priceCents: Long?,
    val currencyCode: String?,
    val category: String,
    val imageUrl: String?,
    val sourceUrl: String,
    val sourceDomain: String,
    val retailer: String,
    val status: String,
    val note: String?
)

data class CommunityProduct(
    val id: String,
    val title: String,
    val priceCents: Long,
    val currencyCode: String,
    val category: String,
    val imageUrl: String?,
    val sourceDomain: String,
    val ghostCount: Int,
    val activityTag: String = "User Ghosted"
)

sealed interface ProductImportState {
    data object Idle : ProductImportState
    data object Loading : ProductImportState
    data class Ready(val product: ImportedProduct) : ProductImportState
    data class Error(val message: String) : ProductImportState
}

object ProductImportRepository {
    suspend fun preview(sourceUrl: String): Result<ImportedProduct> = withContext(Dispatchers.IO) {
        runCatching {
            val response = request("/api/link-preview", "POST", JSONObject().apply { put("url", sourceUrl) })
            val product = response.getJSONObject("product")
            ImportedProduct(
                title = product.optString("title", "Shared product"),
                priceCents = product.optLong("priceCents").takeIf { !product.isNull("priceCents") },
                currencyCode = product.optString("currencyCode").takeIf { it.isNotBlank() },
                category = product.optString("category", "Other"),
                imageUrl = product.optString("imageUrl").takeIf { it.isNotBlank() },
                sourceUrl = product.getString("canonicalUrl"),
                sourceDomain = product.getString("sourceDomain"),
                retailer = product.getString("retailer"),
                status = product.getString("status"),
                note = product.optString("note").takeIf { it.isNotBlank() }
            )
        }
    }

    suspend fun communityFeed(limit: Int = 12): Result<List<CommunityProduct>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = request("/api/community-products?limit=${limit.coerceIn(1, 30)}", "GET")
            val items = response.optJSONArray("products")
            buildList {
                if (items != null) for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    add(
                        CommunityProduct(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            priceCents = item.optLong("priceCents", 0),
                            currencyCode = item.optString("currencyCode", "AED"),
                            category = item.optString("category", "Other"),
                            imageUrl = item.optString("imageUrl").takeIf { it.isNotBlank() },
                            sourceDomain = item.optString("sourceDomain"),
                            ghostCount = item.optInt("ghostCount", 1),
                            activityTag = item.optString("activityTag", "User Ghosted")
                        )
                    )
                }
            }
        }
    }

    suspend fun publish(draft: AlmostBuyDraft): Result<CommunityProduct?> = withContext(Dispatchers.IO) {
        runCatching {
            if (!draft.shareWithCommunity || draft.sourceUrl.isNullOrBlank()) return@runCatching null
            val response = request(
                "/api/community-products",
                "POST",
                JSONObject().apply {
                    put("shareWithCommunity", true)
                    put("sourceUrl", draft.sourceUrl)
                    put("title", draft.name)
                    put("category", draft.category)
                    put("priceCents", draft.amountCents)
                    put("currencyCode", "AED")
                    put("imageUrl", draft.imageUrl ?: JSONObject.NULL)
                    put("source", "android")
                    put("eventId", UUID.randomUUID().toString())
                }
            )
            response.optJSONObject("product")?.let { item ->
                CommunityProduct(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    priceCents = item.optLong("priceCents", 0),
                    currencyCode = item.optString("currencyCode", "AED"),
                    category = item.optString("category", "Other"),
                    imageUrl = item.optString("imageUrl").takeIf { it.isNotBlank() },
                    sourceDomain = item.optString("sourceDomain"),
                    ghostCount = item.optInt("ghostCount", 1),
                    activityTag = item.optString("activityTag", "User Ghosted")
                )
            }
        }
    }

    private fun request(path: String, method: String, body: JSONObject? = null): JSONObject {
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            readTimeout = ApiConfig.READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
        }
        try {
            if (body != null) OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (code !in 200..299) throw IllegalStateException(json.optString("error", "Request failed with status $code"))
            return json
        } finally {
            connection.disconnect()
        }
    }
}