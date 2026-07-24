package com.example.ghostcart.data

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Receives the cooldown-expiry push the backend's cron sweep sends
 * (lib/cooldown-push-sweep.ts). A message with both a `notification` and a
 * `data` payload is auto-displayed by the system while the app is
 * backgrounded/killed - this class only has to build the notification itself
 * for the foreground case, where FCM never auto-displays anything.
 */
class GhostFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        scope.launch { AlmostBuySync.registerDeviceToken(applicationContext, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: return
        val cooldownId = message.data["cooldownId"]
        Analytics.logNotificationReceived(applicationContext, message.data["type"] ?: "cooldown_resolved")
        GhostNotificationPublisher.show(
            context = applicationContext,
            channelId = "ghost_cooling_reminders",
            channelName = "Cooling reminders",
            channelDescription = "Updates when a Ghost Cart cooling period is complete",
            notificationId = 2200 + (cooldownId?.hashCode() ?: title.hashCode()).and(0x7FFF),
            title = title,
            text = body,
            cooldownId = cooldownId
        )
    }
}

private suspend fun fetchCurrentFcmToken(): String? = suspendCancellableCoroutine { continuation ->
    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token -> if (continuation.isActive) continuation.resume(token) }
        .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
}

// Registers (or re-registers) this device's current FCM token against the
// signed-in account. Call after sign-in and on app launch if already signed
// in - onNewToken alone only fires on Firebase's own refresh schedule, which
// could be long after the user actually signs in on a fresh install.
suspend fun registerCurrentFcmToken(context: Context) {
    val token = runCatching { fetchCurrentFcmToken() }.getOrNull() ?: return
    AlmostBuySync.registerDeviceToken(context, token)
}
