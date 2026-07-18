package com.example.ghostcart.data

import android.content.Context
import android.content.Intent
import android.net.Uri

const val GHOST_SHARE_BASE_URL = "https://ghost-cart-preview.maaz-n-khan.chatgpt.site/ghost"

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
    val shareUrl = buildGhostShareUrl(item)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Ghost this with me")
        putExtra(
            Intent.EXTRA_TEXT,
            "I put ${item.title.trim().take(160)} in Ghost Cart instead of impulse-buying it. Ghost it with me:\n$shareUrl"
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share this Ghost item"))
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
