package com.example.ghostcart.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin wrapper around Firebase Analytics with a fixed, named event taxonomy -
 * call sites use these typed methods instead of raw logEvent() calls
 * scattered around, so the full list of what's tracked is always visible in
 * one file.
 *
 * Every event name/param mirrors the website's GoogleAnalytics.tsx
 * (trackEvent) taxonomy where they overlap (e.g. "download_clicked" isn't
 * relevant here, but "sign_in"/"leaderboard_opt_in" are conceptually the
 * same shape on both platforms) so cross-platform funnels line up in GA4.
 */
object Analytics {
    private fun analytics(context: Context) = FirebaseAnalytics.getInstance(context)

    fun logSignIn(context: Context, method: String) {
        analytics(context).logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        })
    }

    fun logCaptureCompleted(context: Context, sourceKind: String, category: String) {
        analytics(context).logEvent("capture_completed", Bundle().apply {
            putString("source_kind", sourceKind)
            putString("category", category)
        })
    }

    fun logCooldownResolved(context: Context, outcome: String) {
        analytics(context).logEvent("cooldown_resolved", Bundle().apply {
            putString("outcome", outcome)
        })
    }

    fun logNotificationReceived(context: Context, type: String) {
        analytics(context).logEvent("notification_received", Bundle().apply {
            putString("type", type)
        })
    }

    fun logNotificationOpened(context: Context, type: String) {
        analytics(context).logEvent("notification_opened", Bundle().apply {
            putString("type", type)
        })
    }

    fun logLeaderboardViewed(context: Context) {
        analytics(context).logEvent("leaderboard_viewed", null)
    }

    fun logLeaderboardOptIn(context: Context, optedIn: Boolean) {
        analytics(context).logEvent(if (optedIn) "leaderboard_opt_in" else "leaderboard_opt_out", null)
    }

    fun logStoryViewed(context: Context, index: Int) {
        analytics(context).logEvent("story_viewed", Bundle().apply {
            putInt("index", index)
        })
    }

    fun logShare(context: Context, contentType: String) {
        analytics(context).logEvent(FirebaseAnalytics.Event.SHARE, Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
        })
    }

    /**
     * Fired on every navigation-destination change (see Navigation.kt) using
     * the standard SCREEN_VIEW event/params - this is what unlocks Firebase's
     * built-in "time on screen" engagement reporting. A single-Activity Compose
     * app doesn't get this for free the way multi-Activity apps do, since
     * Firebase's automatic screen tracking is Activity-lifecycle-based and
     * every in-app destination shares the one Activity.
     */
    fun logScreenView(context: Context, screenName: String) {
        analytics(context).logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        })
    }

    // Fired from AppViewModel.createAlmostBuy() - the single funnel every
    // "Cool it"/"Start cooling" button in the app goes through (product
    // cards, product detail, cart, manual capture), so this one call site
    // covers all of them consistently.
    fun logCoolingStarted(context: Context, sourceKind: String, category: String) {
        analytics(context).logEvent("cooling_started", Bundle().apply {
            putString("source_kind", sourceKind)
            putString("category", category)
        })
    }

    // Fired from AppViewModel.addToCart() - the single funnel every "Add to
    // cart"/"Cool it instead" cart-add action goes through.
    fun logAddToCart(context: Context, productId: String, category: String) {
        analytics(context).logEvent(FirebaseAnalytics.Event.ADD_TO_CART, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, productId)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, category)
        })
    }

    // Deliberately not FirebaseAnalytics.Event.PURCHASE - this is a simulated
    // checkout with no real money or IAP involved, and the PURCHASE event
    // carries revenue-reporting semantics in GA4/Play Console that would
    // misrepresent it.
    fun logCheckoutCompleted(context: Context, itemCount: Int, totalCents: Int) {
        analytics(context).logEvent("simulated_checkout_completed", Bundle().apply {
            putInt("item_count", itemCount)
            putInt("total_cents", totalCents)
        })
    }

    fun logNotificationActionTapped(context: Context, action: String) {
        analytics(context).logEvent("notification_action_tapped", Bundle().apply {
            putString("action", action)
        })
    }
}
