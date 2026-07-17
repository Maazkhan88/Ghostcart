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

private fun JSONObject.nullableString(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

internal fun nonJsonProductApiFallback(text: String): String? =
    if (text.isNotBlank() && !text.trimStart().startsWith("{")) {
        "Ghost Cart could not read product details right now. Try again, or enter the details manually."
    } else {
        null
    }

internal fun parseProductApiResponse(code: Int, text: String): JSONObject {
    nonJsonProductApiFallback(text)?.let { throw IllegalStateException(it) }
    val json = if (text.isBlank()) {
        JSONObject()
    } else {
        runCatching { JSONObject(text) }.getOrElse {
            throw IllegalStateException(
                "Ghost Cart could not read product details right now. Try again, or enter the details manually."
            )
        }
    }
    if (code !in 200..299) {
        throw IllegalStateException(
            json.optString("error").takeIf { it.isNotBlank() }
                ?: "Product capture is temporarily unavailable. Enter the details manually or try again."
        )
    }
    return json
}

internal data class RetailerHtmlMetadata(
    val title: String?,
    val priceCents: Long?,
    val currencyCode: String?,
    val imageUrl: String?
)

internal fun mergeSharedMetadata(
    product: ImportedProduct,
    sharedTitle: String?,
    sharedImageUrl: String?
): ImportedProduct {
    val cleanTitle = sharedTitle?.trim()?.take(160)?.takeIf { it.isNotBlank() }
    val cleanImage = sharedImageUrl?.trim()?.takeIf { it.isNotBlank() }
    val title = if (product.status == "needs_input" || product.title == "Shared product") {
        cleanTitle ?: product.title
    } else {
        product.title
    }
    val imageUrl = product.imageUrl ?: cleanImage
    val complete = title != "Shared product" && product.priceCents != null && imageUrl != null
    return product.copy(
        title = title,
        imageUrl = imageUrl,
        status = if (complete) "complete" else if (title != "Shared product" || imageUrl != null) "partial" else product.status,
        note = if (complete) null else product.note
    )
}
private fun decodeRetailerHtml(value: String): String = value
    .replace("&nbsp;", " ", ignoreCase = true)
    .replace("&#160;", " ", ignoreCase = true)
    .replace("&amp;", "&", ignoreCase = true)
    .replace("&quot;", "\"", ignoreCase = true)
    .replace("&#39;", "'", ignoreCase = true)
    .replace(Regex("\\s+"), " ")
    .trim()

private fun retailerAttribute(tag: String, name: String): String? =
    Regex("""\b$name\s*=\s*(["'])(.*?)\1""", RegexOption.IGNORE_CASE)
        .find(tag)?.groupValues?.getOrNull(2)?.let(::decodeRetailerHtml)

private fun retailerMeta(html: String, keys: Set<String>): String? {
    val wanted = keys.map { it.lowercase() }.toSet()
    return Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
        .findAll(html)
        .map { it.value }
        .firstNotNullOfOrNull { tag ->
            val key = retailerAttribute(tag, "property")
                ?: retailerAttribute(tag, "name")
                ?: retailerAttribute(tag, "itemprop")
            val content = retailerAttribute(tag, "content")
            content?.takeIf { key?.lowercase() in wanted }
        }
}

private fun allowedRetailerImage(value: String?): String? = value?.let { candidate ->
    runCatching {
        val url = URL(candidate)
        val host = url.host.lowercase().trimEnd('.')
        val allowed = listOf("media-amazon.com", "ssl-images-amazon.com", "nooncdn.com", "nordcdn.com")
            .any { host == it || host.endsWith(".$it") }
        candidate.takeIf { url.protocol == "https" && allowed }
    }.getOrNull()
}

internal fun extractRetailerHtmlMetadata(html: String): RetailerHtmlMetadata {
    val rawTitle = retailerMeta(html, setOf("og:title", "twitter:title"))
        ?: Regex("""<title\b[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.let(::decodeRetailerHtml)
    val title = rawTitle
        ?.replace(Regex("""\s*:\s*Buy Online.*$""", RegexOption.IGNORE_CASE), "")
        ?.replace(Regex("""\s*[|–-]\s*(Amazon(?:\.ae)?|noon).*$""", RegexOption.IGNORE_CASE), "")
        ?.trim()?.take(160)?.takeIf { it.isNotBlank() }
    val rawPrice = retailerMeta(
        html,
        setOf("product:price:amount", "og:price:amount", "price")
    ) ?: Regex(
        """class=["'][^"']*\ba-offscreen\b[^"']*["'][^>]*>\s*([^<]+)<""",
        RegexOption.IGNORE_CASE
    ).find(html)?.groupValues?.getOrNull(1)?.let(::decodeRetailerHtml)
    val amount = rawPrice?.replace(",", "")?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull()
    val priceCents = amount?.takeIf { it > 0.0 && it <= 100_000_000.0 }?.let { kotlin.math.round(it * 100).toLong() }
    val metaImage = retailerMeta(html, setOf("og:image", "twitter:image"))
    val amazonImage = Regex(
        """https://m\.media-amazon\.com/images/I/[A-Za-z0-9%_.+|~,-]+?\.(?:jpg|jpeg|png)""",
        RegexOption.IGNORE_CASE
    ).findAll(html).map { it.value }.firstOrNull {
        Regex("""_(?:AC_)?SL\d+""", RegexOption.IGNORE_CASE).containsMatchIn(it)
    }
    val imageUrl = allowedRetailerImage(metaImage) ?: allowedRetailerImage(amazonImage)
    val currency = retailerMeta(
        html,
        setOf("product:price:currency", "og:price:currency", "pricecurrency")
    )?.uppercase()?.take(3)
    return RetailerHtmlMetadata(
        title = title,
        priceCents = priceCents,
        currencyCode = when {
            priceCents == null -> null
            currency == "AED" -> "AED"
            rawPrice?.contains("AED", ignoreCase = true) == true -> "AED"
            else -> null
        },
        imageUrl = imageUrl
    )
}
object ProductImportRepository {
    suspend fun preview(sourceUrl: String, sharedTitle: String? = null, sharedImageUrl: String? = null): Result<ImportedProduct> = withContext(Dispatchers.IO) {
        runCatching {
            val response = request("/api/link-preview", "POST", JSONObject().apply { put("url", sourceUrl) })
            val product = response.getJSONObject("product")
            val imported = ImportedProduct(
                title = product.optString("title", "Shared product"),
                priceCents = product.optLong("priceCents").takeIf { !product.isNull("priceCents") },
                currencyCode = product.nullableString("currencyCode"),
                category = product.optString("category", "Other"),
                imageUrl = product.nullableString("imageUrl"),
                sourceUrl = product.getString("canonicalUrl"),
                sourceDomain = product.getString("sourceDomain"),
                retailer = product.getString("retailer"),
                status = product.getString("status"),
                note = product.nullableString("note")
            )
            enrichFromRetailer(mergeSharedMetadata(imported, sharedTitle, sharedImageUrl))
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
                            imageUrl = item.nullableString("imageUrl"),
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
                    imageUrl = item.nullableString("imageUrl"),
                    sourceDomain = item.optString("sourceDomain"),
                    ghostCount = item.optInt("ghostCount", 1),
                    activityTag = item.optString("activityTag", "User Ghosted")
                )
            }
        }
    }

    private fun enrichFromRetailer(product: ImportedProduct): ImportedProduct {
        if (product.priceCents != null && product.imageUrl != null) return product
        return runCatching {
            val connection = (URL(product.sourceUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "text/html,application/xhtml+xml;q=0.9")
                setRequestProperty("Accept-Language", "en-AE,en;q=0.9")
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36"
                )
            }
            try {
                val code = connection.responseCode
                val finalUrl = connection.url.toString()
                if (code !in 200..299 || extractSupportedRetailerUrl(finalUrl) == null) return@runCatching product
                val metadata = extractRetailerHtmlMetadata(readRetailerHtml(connection))
                val mergedTitle = if (product.title == "Shared product") metadata.title ?: product.title else product.title
                val mergedPrice = product.priceCents ?: metadata.priceCents
                val mergedImage = product.imageUrl ?: metadata.imageUrl
                val mergedCurrency = product.currencyCode ?: metadata.currencyCode
                val complete = mergedTitle != "Shared product" && mergedPrice != null && mergedImage != null
                product.copy(
                    title = mergedTitle,
                    priceCents = mergedPrice,
                    currencyCode = mergedCurrency,
                    imageUrl = mergedImage,
                    status = if (complete) "complete" else "partial",
                    note = if (complete) null else "Some details could not be read. Check and edit them before ghosting."
                )
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(product)
    }

    private fun readRetailerHtml(connection: HttpURLConnection): String {
        val output = StringBuilder()
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val buffer = CharArray(8_192)
            while (output.length < 1_200_000) {
                val count = reader.read(buffer, 0, minOf(buffer.size, 1_200_000 - output.length))
                if (count <= 0) break
                output.append(buffer, 0, count)
            }
        }
        return output.toString()
    }

    private fun request(path: String, method: String, body: JSONObject? = null): JSONObject {
        val connection = (URL("${ApiConfig.PRODUCT_API_BASE_URL}$path").openConnection() as HttpURLConnection).apply {
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
            return parseProductApiResponse(code, text)
        } finally {
            connection.disconnect()
        }
    }
}