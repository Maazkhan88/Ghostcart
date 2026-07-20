package com.example.ghostcart.data

import android.content.Context
import java.util.UUID

/**
 * A random, per-install identifier used only to scope anonymous server-side state
 * (currently: simulation-consent acceptance) to this device. Never derived from
 * any hardware/advertising identifier, and never sent anywhere except as the
 * X-Ghost-Cart-Installation-Id header on Ghost Cart's own API.
 */
object InstallationId {
    private const val PREFS_NAME = "ghost_cart_prefs"
    private const val KEY = "installation_id"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { return it }
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY, generated).apply()
        return generated
    }
}
