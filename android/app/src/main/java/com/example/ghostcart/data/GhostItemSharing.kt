package com.example.ghostcart.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

const val GHOST_SHARE_BASE_URL = "${ApiConfig.BASE_URL}/ghost"
private const val GHOST_SHARE_API_URL = "${ApiConfig.BASE_URL}/api/share-items"

data class GhostShareItem(
    val title: String,
    val priceCents: Long,
    val category: String,
    val imageUrl: String? = null,
    val sourceUrl: String? = null
)

fun MarketplaceProduct.toGhostShareItem() = GhostShareItem(
    title = name,
    priceCents = price.toLong().coerceAtLeast(0) * 100L,
    category = category,
    imageUrl = imageUrl,
    sourceUrl = sourceUrl
)

fun AlmostBuy.toGhostShareItem() = GhostShareItem(
    title = name,
    priceCents = amountCents.coerceAtLeast(0),
    category = category,
    imageUrl = imageUrl,
    sourceUrl = sourceUrl
)

fun buildGhostShareUrl(item: GhostShareItem): String = Uri.parse(GHOST_SHARE_BASE_URL)
    .buildUpon()
    .appendQueryParameter("title", item.title.trim().take(160))
    .appendQueryParameter("price", item.priceCents.coerceAtLeast(0).toString())
    .appendQueryParameter("category", item.category.trim().take(80))
    .apply {
        item.imageUrl.safePublicHttpsUrl()?.let { appendQueryParameter("image", it) }
        item.sourceUrl.safePublicHttpsUrl()?.let { appendQueryParameter("source", it) }
    }
    .build()
    .toString()

fun shareGhostItem(context: Context, item: GhostShareItem) {
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        val shareUrl = createShortGhostShareUrl(item).getOrElse { buildGhostShareUrl(item) }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Ghost this with me")
            putExtra(
                Intent.EXTRA_TEXT,
                "I put ${item.title.trim().take(160)} in Ghost Cart instead of impulse-buying it. Ghost it with me:\n$shareUrl"
            )
        }
        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(shareIntent, "Share this Ghost item"))
        }
    }
}

suspend fun fetchSharedGhostItem(shareId: String): Result<GhostShareItem> = withContext(Dispatchers.IO) {
    runCatching {
        require(shareId.matches(Regex("^[23456789A-HJ-NP-Za-km-z]{8}$")))
        val connection = (URL("$GHOST_SHARE_API_URL/$shareId").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            readTimeout = ApiConfig.READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = JSONObject(stream?.bufferedReader()?.use { it.readText() }.orEmpty())
            if (code !in 200..299) error(response.optString("error", "Shared item is unavailable"))
            val item = response.getJSONObject("item")
            GhostShareItem(
                title = item.getString("title"),
                priceCents = item.optLong("priceCents", 0L).coerceAtLeast(0L),
                category = item.optString("category", "Other"),
                imageUrl = item.optString("imageUrl").takeIf { !item.isNull("imageUrl") && it.isNotBlank() },
                sourceUrl = item.optString("sourceUrl").takeIf { !item.isNull("sourceUrl") && it.isNotBlank() }
            )
        } finally {
            connection.disconnect()
        }
    }
}

private suspend fun createShortGhostShareUrl(item: GhostShareItem): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(GHOST_SHARE_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            readTimeout = ApiConfig.READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }
        try {
            val payload = JSONObject().apply {
                put("title", item.title.trim().take(160))
                put("priceCents", item.priceCents.coerceAtLeast(0L))
                put("category", item.category.trim().take(80))
                put("imageUrl", item.imageUrl.safePublicHttpsUrl() ?: JSONObject.NULL)
                put("sourceUrl", item.sourceUrl.safePublicHttpsUrl() ?: JSONObject.NULL)
            }
            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = JSONObject(stream?.bufferedReader()?.use { it.readText() }.orEmpty())
            if (code !in 200..299) error(response.optString("error", "Unable to shorten share link"))
            response.getString("url")
        } finally {
            connection.disconnect()
        }
    }
}

fun openProductSource(context: Context, sourceUrl: String) {
    val safeUrl = sourceUrl.safePublicHttpsUrl() ?: return
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
}

private fun String?.safePublicHttpsUrl(): String? = this
    ?.trim()
    ?.takeIf { value ->
        runCatching {
            val uri = Uri.parse(value)
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                !uri.host.equals("localhost", ignoreCase = true)
        }.getOrDefault(false)
    }
