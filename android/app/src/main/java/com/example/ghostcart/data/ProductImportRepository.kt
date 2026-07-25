package com.example.ghostcart.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private val URL_PATTERN = Regex("""https://[^\s]+""", RegexOption.IGNORE_CASE)
private val BLOCKED_LINK_SUFFIXES = listOf(".localhost", ".local", ".internal", ".home", ".lan", ".onion")
private const val PRODUCT_IMPORT_LOG_TAG = "ProductImport"

// Same bot-challenge heuristic as the server-side scraper
// (lib/product-link-preview.ts) - Amazon can return a normal-looking 200 with
// no product image markup at all when it decides a request looks automated.
private val BOT_CHALLENGE_MARKERS = Regex(
    """enter the characters you see below|api-services-support@amazon|robot check|to discuss automated access""",
    RegexOption.IGNORE_CASE
)
private fun looksLikeBotChallenge(html: String): Boolean = BOT_CHALLENGE_MARKERS.containsMatchIn(html.take(20_000))

/**
 * Parses a `Date().toISOString()`-style timestamp (e.g. "2026-07-19T15:30:00.000Z") from the
 * backend into epoch millis. Uses SimpleDateFormat rather than java.time since minSdk 24 predates
 * java.time without core library desugaring.
 */
fun parseIsoTimestampMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return runCatching {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        format.parse(iso)?.time
    }.getOrNull()
}

private fun isSafePublicHttpsUrl(value: String): Boolean = runCatching {
    val url = URL(value)
    val host = url.host.lowercase().trimEnd('.')
    val ipLiteral = Regex("""[0-9]{1,3}(?:[.][0-9]{1,3}){3}""").matches(host) || host.contains(':')
    url.protocol.equals("https", ignoreCase = true) &&
        url.userInfo == null &&
        (url.port == -1 || url.port == 443) &&
        host.contains('.') &&
        host != "localhost" &&
        !ipLiteral &&
        BLOCKED_LINK_SUFFIXES.none(host::endsWith)
}.getOrDefault(false)

fun extractSharedUrl(sharedText: String?): String? = sharedText
    ?.let {
        URL_PATTERN.findAll(it)
            .map { match -> match.value.trimEnd('.', ',', ')', ']', '}', '>', '"') }
            .firstOrNull(::isSafePublicHttpsUrl)
    }

@Deprecated("Use extractSharedUrl; generic public HTTPS links are supported.")
fun extractSupportedRetailerUrl(sharedText: String?): String? = extractSharedUrl(sharedText)

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

data class ListingProductStub(
    val title: String,
    val priceCents: Long?,
    val currencyCode: String?,
    val category: String,
    val imageUrl: String?,
    val sourceUrl: String,
    val sourceDomain: String,
    val retailer: String
)

sealed interface LinkImportResult {
    data class Product(val product: ImportedProduct) : LinkImportResult
    data class Listing(val sourceDomain: String, val retailer: String, val items: List<ListingProductStub>) : LinkImportResult
}

data class CommunityProduct(
    val id: String,
    val title: String,
    val priceCents: Long,
    val currencyCode: String,
    val category: String,
    val imageUrl: String?,
    val sourceDomain: String,
    val sourceUrl: String?,
    val ghostCount: Int,
    val activityTag: String = "User Ghosted",
    /** Epoch millis parsed from the server's lastGhostedAt; null if missing/unparseable. */
    val lastGhostedAtMillis: Long? = null
)

/** An admin-managed home banner or "Ghost Cart Story" image, fetched from /api/content-blocks. */
data class ContentBlockItem(
    val id: Int,
    val type: String,
    val imageUrl: String,
    val sortOrder: Int,
    val mediaType: String = "image"
)

sealed interface ProductImportState {
    data object Idle : ProductImportState
    data object Loading : ProductImportState
    data class Ready(val product: ImportedProduct) : ProductImportState
    data class ListingDetected(val sourceDomain: String, val retailer: String, val items: List<ListingProductStub>) : ProductImportState
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
    val title = if (product.status == "needs_input" || titleLooksLikeFallback(product.title)) {
        cleanTitle ?: product.title
    } else {
        product.title
    }
    val imageUrl = product.imageUrl ?: cleanImage
    val complete = !titleLooksLikeFallback(title) && product.priceCents != null && imageUrl != null
    return product.copy(
        title = title,
        imageUrl = imageUrl,
        status = if (complete) "complete" else if (!titleLooksLikeFallback(title) || imageUrl != null) "partial" else product.status,
        note = if (complete) null else product.note
    )
}
internal fun mergeDeviceMetadata(
    product: ImportedProduct,
    metadata: DeviceLinkMetadata
): ImportedProduct {
    val currentTitleIsFallback = product.status == "needs_input" || titleLooksLikeFallback(product.title)
    val mergedTitle = if (currentTitleIsFallback) metadata.title ?: product.title else product.title
    val mergedPrice = product.priceCents ?: metadata.priceCents
    val mergedImage = product.imageUrl ?: metadata.imageUrl
    val mergedCurrency = product.currencyCode ?: metadata.currencyCode
    val complete = !titleLooksLikeFallback(mergedTitle) && mergedPrice != null && mergedImage != null
    return product.copy(
        title = mergedTitle,
        priceCents = mergedPrice,
        currencyCode = mergedCurrency,
        imageUrl = mergedImage,
        status = if (complete) "complete" else "partial",
        note = if (complete) null else "Some details could not be read automatically. Check and edit them before ghosting."
    )
}

private val GENERIC_SHARE_CAPTION_REGEX = Regex("""^check (this|it) out\b""", RegexOption.IGNORE_CASE)

private fun titleLooksLikeFallback(value: String): Boolean =
    value == "Shared product" ||
        value.startsWith("Shared item from ", ignoreCase = true) ||
        GENERIC_SHARE_CAPTION_REGEX.containsMatchIn(value.trim()) ||
        Regex("""^[A-Z0-9_-]{10,}$""", RegexOption.IGNORE_CASE).matches(value)
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

private fun allowedRetailerImage(value: String?): String? = value
    ?.trim()
    ?.takeIf(::isSafePublicHttpsUrl)

private fun looksLikePrice(text: String): Boolean {
    val normalized = text.replace(Regex("[^0-9.,]"), "").replace(",", "")
    if (normalized.isBlank()) return false
    val amount = normalized.toDoubleOrNull() ?: return false
    return amount in 0.0..100_000_000.0
}

// Mirrors lib/product-link-preview.ts's amazonPagePrice() - same underlying
// bug (a page's *first* a-offscreen span can be an empty screen-reader
// artifact, which permanently kills price extraction if trusted
// unconditionally) fixed the same way: score every price-shaped candidate,
// don't just take the first one.
private fun amazonOffscreenPrice(html: String): String? {
    val pattern = Regex(
        """class=["'][^"']*\ba-offscreen\b[^"']*["'][^>]*>\s*([^<]+)<""",
        RegexOption.IGNORE_CASE
    )
    return pattern.findAll(html)
        .mapNotNull { match ->
            val text = decodeRetailerHtml(match.groupValues[1])
            if (!looksLikePrice(text)) return@mapNotNull null
            val start = (match.range.first - 700).coerceAtLeast(0)
            val end = (match.range.last + 101).coerceAtMost(html.length)
            val context = html.substring(start, end)
            var score = 0
            if (Regex(
                    """tp_price_block_total_price_ww|apex-pricetopay-value|priceToPay|reinventPricePriceToPayMargin""",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(context)
            ) score += 10_000
            if (Regex("""data-a-strike=["']true["']|basisPrice|\ba-text-price\b""", RegexOption.IGNORE_CASE).containsMatchIn(context)) score -= 8_000
            if (Regex("""compare new|frequently bought together|customers who bought|sponsored""", RegexOption.IGNORE_CASE).containsMatchIn(context)) score -= 8_000
            Triple(text, score, match.range.first)
        }
        .sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }.thenBy { it.third })
        .firstOrNull()?.first
}

// Extracts the JSON-string value of a top-level "key":"..." field, decoding
// real JSON escapes (\/, \", \uXXXX) via org.json rather than keeping them
// literal - needed so this matches keys decoded the same way out of a parsed
// JSON object (see extractBalancedJsonObject below).
private fun extractJsonStringField(html: String, key: String): String? {
    val pattern = Regex("\"$key\"\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\")")
    val match = pattern.find(html) ?: return null
    return runCatching { JSONObject("{\"v\":${match.groupValues[1]}}").getString("v") }.getOrNull()
}

// "someKey":{...} can nest arbitrarily deep (Amazon's colorImages does) - a
// regex can't safely capture a balanced JSON object, so walk the string
// counting brace depth (ignoring braces inside quoted strings) until it closes.
private fun extractBalancedJsonObject(html: String, key: String): JSONObject? {
    val marker = "\"$key\":{"
    val start = html.indexOf(marker)
    if (start == -1) return null
    val braceStart = start + marker.length - 1
    var depth = 0
    var inString = false
    var escaped = false
    val backslash = 92.toChar()
    for (i in braceStart until html.length) {
        val ch = html[i]
        if (inString) {
            when {
                escaped -> escaped = false
                ch == backslash -> escaped = true
                ch == '"' -> inString = false
            }
            continue
        }
        when (ch) {
            '"' -> inString = true
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) {
                    return runCatching { JSONObject(html.substring(braceStart, i + 1)) }.getOrNull()
                }
            }
        }
    }
    return null
}

// Mirrors lib/product-link-preview.ts's pickLandingColorImage() - Amazon
// states its own answer to "which color variant is this canonical URL
// actually showing" (landingAsinColor) right in the page; the keyword-scored
// amazonPrimaryImage() below has no concept of color variants and can (and
// did, confirmed against a real amazon.ae multi-color listing) land on a
// different color's gallery image. Try Amazon's stated default first; fall
// back to the keyword scorer if this data isn't present.
private fun pickLandingColorImage(html: String): String? = runCatching {
    val landingColor = extractJsonStringField(html, "landingAsinColor") ?: return@runCatching null
    val colorImages = extractBalancedJsonObject(html, "colorImages") ?: return@runCatching null
    val entries = colorImages.optJSONArray(landingColor) ?: return@runCatching null
    val entry = if (entries.length() > 0) entries.optJSONObject(0) else null
    entry ?: return@runCatching null
    val hiRes = entry.optString("hiRes").takeIf { it.isNotBlank() }
    val large = entry.optString("large").takeIf { it.isNotBlank() }
    val main = entry.optJSONObject("main")
    val firstMainKey = main?.keys()?.asSequence()?.firstOrNull()
    hiRes ?: large ?: firstMainKey
}.getOrNull()

private fun amazonPrimaryImage(html: String): String? {
    val imageRegex = Regex(
        """https://m\.media-amazon\.com/images/I/[A-Za-z0-9%_.+|~,-]+?\.(?:jpg|jpeg|png)""",
        RegexOption.IGNORE_CASE
    )
    return imageRegex.findAll(html)
        .map { match ->
            val start = (match.range.first - 700).coerceAtLeast(0)
            val end = (match.range.last + 701).coerceAtMost(html.length)
            val context = html.substring(start, end)
            var score = 0
            if (Regex("""id=["']landingImage["']|data-a-image-name=["']landingImage["']""", RegexOption.IGNORE_CASE).containsMatchIn(context)) score += 20_000
            if (Regex("""imageBlockATF|colorImages|imageGalleryData""", RegexOption.IGNORE_CASE).containsMatchIn(context)) score += 8_000
            if (Regex("""["']hiRes["']\s*:|data-old-hires""", RegexOption.IGNORE_CASE).containsMatchIn(context)) score += 3_000
            if (Regex("""["']large["']\s*:""", RegexOption.IGNORE_CASE).containsMatchIn(context)) score += 1_000
            val resolution = Regex("""_(?:AC_)?SL(\d+)""", RegexOption.IGNORE_CASE)
                .find(match.value)?.groupValues?.getOrNull(1)?.toIntOrNull()
            score += resolution?.coerceAtMost(3_000) ?: 0
            if (Regex(
                    """warranty|insurance|protection\s*plan|service\s*plan|extended\s*warranty|salama\s*care|attachdss""",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(context)
            ) score -= 30_000
            Triple(match.value, score, match.range.first)
        }
        .filter { (_, score, _) -> score > -10_000 }
        .sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }.thenBy { it.third })
        .firstOrNull()?.first
}

internal fun extractRetailerHtmlMetadata(html: String): RetailerHtmlMetadata {
    val rawTitle = retailerMeta(html, setOf("og:title", "twitter:title"))
        ?: Regex("""<title\b[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.let(::decodeRetailerHtml)
    val title = rawTitle
        ?.replace(Regex("""\s*:\s*Buy Online.*$""", RegexOption.IGNORE_CASE), "")
        ?.trim()?.take(160)?.takeIf { it.isNotBlank() }
    val rawPrice = retailerMeta(
        html,
        setOf("product:price:amount", "og:price:amount", "price")
    ) ?: amazonOffscreenPrice(html)
    val amount = rawPrice?.replace(",", "")?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull()
    val priceCents = amount?.takeIf { it > 0.0 && it <= 100_000_000.0 }?.let { kotlin.math.round(it * 100).toLong() }
    val metaImage = retailerMeta(html, setOf("og:image", "twitter:image"))
    val amazonImage = pickLandingColorImage(html) ?: amazonPrimaryImage(html)
    val imageUrl = allowedRetailerImage(amazonImage) ?: allowedRetailerImage(metaImage)
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
    suspend fun previewLink(sourceUrl: String, sharedTitle: String? = null, sharedImageUrl: String? = null): Result<LinkImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val response = request("/api/link-preview", "POST", JSONObject().apply { put("url", sourceUrl) })
            val listing = response.optJSONObject("listing")
            if (listing != null) {
                val items = listing.optJSONArray("items")
                val stubs = buildList {
                    if (items != null) for (index in 0 until items.length()) {
                        val item = items.getJSONObject(index)
                        add(
                            ListingProductStub(
                                title = item.optString("title", "Shared product"),
                                priceCents = item.optLong("priceCents").takeIf { !item.isNull("priceCents") },
                                currencyCode = item.nullableString("currencyCode"),
                                category = item.optString("category", "Other"),
                                imageUrl = item.nullableString("imageUrl"),
                                sourceUrl = item.getString("canonicalUrl"),
                                sourceDomain = item.getString("sourceDomain"),
                                retailer = item.getString("retailer")
                            )
                        )
                    }
                }
                LinkImportResult.Listing(
                    sourceDomain = listing.getString("sourceDomain"),
                    retailer = listing.getString("retailer"),
                    items = stubs
                )
            } else {
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
                LinkImportResult.Product(enrichFromRetailer(mergeSharedMetadata(imported, sharedTitle, sharedImageUrl)))
            }
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
                            sourceUrl = item.nullableString("sourceUrl"),
                            ghostCount = item.optInt("ghostCount", 1),
                            activityTag = item.optString("activityTag", "User Ghosted"),
                            lastGhostedAtMillis = parseIsoTimestampMillis(item.nullableString("lastGhostedAt"))
                        )
                    )
                }
            }
        }
    }

    /**
     * The admin-managed demo/marketplace catalog (Products tab in /admin). Replaces the
     * previously-hardcoded MarketplaceModels.kt catalogs at the call site - this repository
     * function is the only thing that needs to change if the fetch shape ever changes, not
     * every screen that reads AppViewModel.allProducts.
     */
    suspend fun fetchCatalogProducts(): Result<List<MarketplaceProduct>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = request("/api/products", "GET")
            val items = response.optJSONArray("products")
            buildList {
                if (items != null) for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    if (!item.optBoolean("isActive", true)) continue
                    add(
                        MarketplaceProduct(
                            id = "catalog_${item.getInt("id")}",
                            name = item.getString("name"),
                            category = item.optString("category", "Other"),
                            price = (item.optLong("priceCents", 0) / 100L).toInt(),
                            iconName = "bag",
                            description = item.optString("description", ""),
                            imageUrl = item.nullableString("imageUrl")
                        )
                    )
                }
            }
        }
    }

    /** Admin-managed home banners and "Ghost Cart Stories" (Content tab in /admin). */
    suspend fun fetchContentBlocks(type: String): Result<List<ContentBlockItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = request("/api/content-blocks", "GET")
            val items = response.optJSONArray("contentBlocks")
            buildList {
                if (items != null) for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    if (item.optString("type") != type) continue
                    if (!item.optBoolean("isActive", true)) continue
                    val key = item.getString("imageKey")
                    add(
                        ContentBlockItem(
                            id = item.getInt("id"),
                            type = type,
                            imageUrl = "${ApiConfig.BASE_URL}/api/content-blocks/image/$key",
                            sortOrder = item.optInt("sortOrder", 0),
                            mediaType = item.optString("mediaType", "image")
                        )
                    )
                }
            }.sortedBy { it.sortOrder }
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
                    sourceUrl = item.nullableString("sourceUrl"),
                    ghostCount = item.optInt("ghostCount", 1),
                    activityTag = item.optString("activityTag", "User Ghosted"),
                    lastGhostedAtMillis = parseIsoTimestampMillis(item.nullableString("lastGhostedAt"))
                )
            }
        }
    }

    private fun enrichFromRetailer(product: ImportedProduct): ImportedProduct {
        val amazonSource = runCatching {
            URL(product.sourceUrl).host.lowercase().let { host ->
                host == "amazon.ae" || host.endsWith(".amazon.ae") ||
                    host == "amazon.com" || host.endsWith(".amazon.com")
            }
        }.getOrDefault(false)
        // Amazon can return a complete-looking response whose image belongs to
        // an embedded warranty/insurance add-on. Re-read Amazon pages on the
        // device so the scored landing image can replace that artwork.
        if (!amazonSource && product.priceCents != null && product.imageUrl != null) return product
        Log.d(
            PRODUCT_IMPORT_LOG_TAG,
            "device re-fetch starting amazonSource=$amazonSource serverImage=${product.imageUrl != null} serverPrice=${product.priceCents != null}"
        )
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
                if (code !in 200..299 || !isSafePublicHttpsUrl(finalUrl)) {
                    Log.w(PRODUCT_IMPORT_LOG_TAG, "device re-fetch bad response code=$code safeUrl=${isSafePublicHttpsUrl(finalUrl)}")
                    return@runCatching product
                }
                val html = readRetailerHtml(connection)
                val metadata = extractRetailerHtmlMetadata(html)
                val mergedTitle = if (titleLooksLikeFallback(product.title)) metadata.title ?: product.title else product.title
                val mergedPrice = product.priceCents ?: metadata.priceCents
                val mergedImage = if (amazonSource) metadata.imageUrl ?: product.imageUrl else product.imageUrl ?: metadata.imageUrl
                val mergedCurrency = product.currencyCode ?: metadata.currencyCode
                val complete = !titleLooksLikeFallback(mergedTitle) && mergedPrice != null && mergedImage != null
                if (mergedImage == null) {
                    Log.w(
                        PRODUCT_IMPORT_LOG_TAG,
                        "device re-fetch image MISSING code=$code botChallenge=${looksLikeBotChallenge(html)} htmlChars=${html.length}"
                    )
                } else {
                    Log.d(PRODUCT_IMPORT_LOG_TAG, "device re-fetch image found (fromDeviceFetch=${metadata.imageUrl != null})")
                }
                product.copy(
                    title = mergedTitle,
                    priceCents = mergedPrice,
                    currencyCode = mergedCurrency,
                    imageUrl = mergedImage,
                    status = if (complete) "complete" else "partial",
                    note = if (complete) null else "Some details could not be read automatically. Check and edit them before ghosting."
                )
            } finally {
                connection.disconnect()
            }
        }.onFailure { error ->
            Log.w(PRODUCT_IMPORT_LOG_TAG, "device re-fetch threw, keeping server product: ${error::class.simpleName}: ${error.message}")
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
