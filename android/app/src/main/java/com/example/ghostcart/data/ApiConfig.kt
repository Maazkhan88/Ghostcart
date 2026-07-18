package com.example.ghostcart.data

object ApiConfig {
    // Canonical Cloudflare Worker for the whole app (auth, activity, product
    // import, sharing). Both constants point at the same deployment now —
    // previously BASE_URL pointed at a stale nameless-d98e deploy and
    // PRODUCT_API_BASE_URL pointed at a separate, non-synced ChatGPT Sites
    // deployment with its own database; that split meant a signed-in user's
    // account on one didn't exist on the other. Kept as two constants to
    // minimize call-site churn, not because they should ever diverge again.
    const val BASE_URL = "https://ghostcart-app.maaz-n-khan.workers.dev"
    const val PRODUCT_API_BASE_URL = "https://ghostcart-app.maaz-n-khan.workers.dev"
    const val CONNECT_TIMEOUT_MS = 8_000
    const val READ_TIMEOUT_MS = 8_000
}