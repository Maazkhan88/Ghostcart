package com.example.ghostcart.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ghostcart.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.roundToLong

data class DeviceLinkMetadata(
    val title: String?,
    val imageUrl: String?,
    val priceCents: Long?,
    val currencyCode: String?
)

private fun cleanDeviceTitle(value: String?): String? = value
    ?.replace(Regex("""\s+"""), " ")
    ?.trim()
    ?.take(160)
    ?.takeIf {
        it.length >= 3 &&
            !it.equals("Access Denied", ignoreCase = true) &&
            !it.equals("Page not found", ignoreCase = true) &&
            !Regex("""^[A-Z0-9_-]{10,}$""", RegexOption.IGNORE_CASE).matches(it)
    }

internal fun parseDevicePrice(value: String?): Long? {
    val raw = value?.replace(Regex("""[^0-9.,]"""), "")?.trim().orEmpty()
    if (raw.isBlank()) return null
    val normalized = when {
        raw.contains('.') && raw.contains(',') -> raw.replace(",", "")
        raw.count { it == ',' } == 1 && Regex(""",[0-9]{1,2}$""").containsMatchIn(raw) -> raw.replace(',', '.')
        else -> raw.replace(",", "")
    }
    val amount = normalized.toDoubleOrNull() ?: return null
    return amount.takeIf { it > 0.0 && it <= 100_000_000.0 }?.times(100)?.roundToLong()
}

internal fun parseDeviceLinkMetadata(rawResult: String): DeviceLinkMetadata? = runCatching {
    val unwrapped = when (val value = JSONTokener(rawResult).nextValue()) {
        is String -> value
        else -> rawResult
    }
    val json = JSONObject(unwrapped)
    val title = cleanDeviceTitle(json.optString("title").takeIf { it.isNotBlank() })
    val image = json.optString("image").takeIf { candidate ->
        candidate.isNotBlank() && extractSharedUrl(candidate) != null
    }
    val priceText = json.optString("price").takeIf { it.isNotBlank() }
    val currencyText = json.optString("currency").uppercase()
    val currency = when {
        currencyText.contains("AED") || priceText?.contains("AED", ignoreCase = true) == true -> "AED"
        currencyText.matches(Regex("""[A-Z]{3}""")) -> currencyText
        else -> null
    }
    DeviceLinkMetadata(
        title = title,
        imageUrl = image,
        priceCents = parseDevicePrice(priceText),
        currencyCode = currency
    ).takeIf { it.title != null || it.imageUrl != null || it.priceCents != null }
}.getOrNull()

object DeviceLinkPreview {
    private const val TIMEOUT_MS = 18_000L
    private const val PAGE_SETTLE_MS = 3_000L

    private val extractionScript = """
        (function() {
          const meta = (keys) => {
            for (const key of keys) {
              const node = document.querySelector('meta[property="' + key + '"],meta[name="' + key + '"],meta[itemprop="' + key + '"]');
              const value = node && node.getAttribute('content');
              if (value) return value;
            }
            return '';
          };
          let product = {};
          for (const node of document.querySelectorAll('script[type="application/ld+json"]')) {
            try {
              const parsed = JSON.parse(node.textContent || '{}');
              const queue = Array.isArray(parsed) ? parsed.slice() : [parsed];
              while (queue.length) {
                const candidate = queue.shift();
                if (!candidate || typeof candidate !== 'object') continue;
                if (Array.isArray(candidate['@graph'])) queue.push(...candidate['@graph']);
                if (candidate.mainEntity) queue.push(candidate.mainEntity);
                const type = candidate['@type'];
                if (type === 'Product' || (Array.isArray(type) && type.includes('Product'))) {
                  product = candidate;
                  queue.length = 0;
                  break;
                }
              }
            } catch (_) {}
            if (Object.keys(product).length) break;
          }
          const first = (value) => {
            if (typeof value === 'string') return value;
            if (Array.isArray(value)) return first(value[0]);
            if (value && typeof value === 'object') return first(value.url || value.contentUrl);
            return '';
          };
          const offersValue = Array.isArray(product.offers) ? product.offers[0] : product.offers;
          const offers = offersValue && typeof offersValue === 'object' ? offersValue : {};
          const imageNode = document.querySelector('[data-qa*="product-image" i] img,img[itemprop="image"],img[fetchpriority="high"],picture img');
          const imageRaw = first(product.image) || meta(['og:image:secure_url','og:image','twitter:image','twitter:image:src','image']) || (imageNode && (imageNode.currentSrc || imageNode.src || imageNode.getAttribute('data-src'))) || '';
          let image = '';
          try { image = imageRaw ? new URL(imageRaw, location.href).href : ''; } catch (_) {}
          const priceNode = document.querySelector('[itemprop="price"],meta[property="product:price:amount"],.a-offscreen,[data-qa*="price" i],[class*="priceNow"],[class*="salePrice"]');
          const price = String(offers.price || offers.lowPrice || meta(['product:price:amount','og:price:amount','price','sale_price']) || (priceNode && (priceNode.getAttribute('content') || priceNode.textContent)) || '');
          return JSON.stringify({
            title: first(product.name) || meta(['og:title','twitter:title']) || ((document.querySelector('[data-qa*="product-title" i],[data-qa*="product-name" i],h1,[itemprop="name"]') || {}).textContent || '') || document.title || '',
            image: image,
            price: price,
            currency: first(offers.priceCurrency) || meta(['product:price:currency','og:price:currency','pricecurrency']) || ''
          });
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun read(context: Context, url: String): DeviceLinkMetadata? = withContext(Dispatchers.Main) {
        if (extractSharedUrl(url) == null) return@withContext null
        suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)
            val handler = Handler(Looper.getMainLooper())
            val themedContext = ContextThemeWrapper(context, R.style.Theme_GhostCart)
            val webView = WebView(themedContext)
            val finish: (DeviceLinkMetadata?) -> Unit = finish@{ metadata ->
                if (!completed.compareAndSet(false, true)) return@finish
                handler.removeCallbacksAndMessages(null)
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
                if (continuation.isActive) continuation.resume(metadata)
            }
            val timeout = Runnable { finish(null) }
            handler.postDelayed(timeout, TIMEOUT_MS)
            continuation.invokeOnCancellation { handler.post { finish(null) } }

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mediaPlaybackRequiresUserGesture = true
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                userAgentString = userAgentString.replace("; wv", "").replace("Version/4.0 ", "")
            }
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val candidate = request.url.toString()
                    return request.isForMainFrame && extractSharedUrl(candidate) == null
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    handler.postDelayed({
                        if (!completed.get()) {
                            view.evaluateJavascript(extractionScript) { result ->
                                finish(parseDeviceLinkMetadata(result))
                            }
                        }
                    }, PAGE_SETTLE_MS)
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) finish(null)
                }

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    finish(null)
                    return true
                }
            }
            webView.loadUrl(url)
        }
    }
}
