package com.example.ghostcart.data

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ghostcart.MainActivity
import com.ghostcart.app.R

private const val GHOST_LIVE_UPDATE_TAG = "ghost_delivery_live"
private const val GHOST_LIVE_UPDATE_COLOR = 0xFF64D64A.toInt() // GhostGreen

// One continuous, live-ticking ongoing notification per almost-buy item,
// spanning the whole cooling/delivery window (order placed -> delivered) -
// distinct from DeliveryStepWorker's six discrete stage notifications,
// which still fire alongside this. Uses NotificationCompat's chronometer
// (native OS-driven countdown text, no app-side ticking needed) and
// ProgressStyle, plus setRequestPromotedOngoing so Android 16+ / One UI 7
// can surface it as a status-bar live update / Now Bar chip. Silently
// degrades to a normal ongoing progress notification pre-API 36 - every
// call here is best-effort and must never block the caller.
object GhostDeliveryLiveUpdate {
    fun start(
        context: Context,
        itemId: String,
        productName: String,
        startMillis: Long,
        endMillis: Long
    ) = post(context, itemId, productName, GhostDeliveryState.PLACED, startMillis, endMillis)

    fun updateStage(
        context: Context,
        itemId: String,
        productName: String,
        state: GhostDeliveryState,
        startMillis: Long,
        endMillis: Long
    ) {
        if (state.isTerminal) {
            cancel(context, itemId)
            return
        }
        post(context, itemId, productName, state, startMillis, endMillis)
    }

    fun cancel(context: Context, itemId: String) {
        runCatching { NotificationManagerCompat.from(context).cancel(GHOST_LIVE_UPDATE_TAG, notificationId(itemId)) }
    }

    // Rare full-reset path (e.g. sign-out) with no per-item id list on hand -
    // enumerate this app's own active notifications and cancel only the ones
    // tagged as live-delivery updates, rather than clearing every notification.
    fun cancelAll(context: Context) {
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            manager.activeNotifications
                .filter { it.tag == GHOST_LIVE_UPDATE_TAG }
                .forEach { manager.cancel(it.tag, it.id) }
        }
    }

    private fun notificationId(itemId: String): Int =
        ("ghost_live_$itemId".hashCode() and 0x7FFFFFFF).coerceAtLeast(1)

    private fun stageLabel(state: GhostDeliveryState): String = when (state) {
        GhostDeliveryState.PLACED -> "Ghosting"
        GhostDeliveryState.PREPARING -> "Giving it space"
        GhostDeliveryState.RIDER_PICKING_UP -> "Ghost Rider picking up"
        GhostDeliveryState.OUT_FOR_DELIVERY -> "Out for Ghost Delivery"
        GhostDeliveryState.RIDER_NEARBY -> "Almost there"
        else -> "Ghosting"
    }

    private fun post(
        context: Context,
        itemId: String,
        productName: String,
        state: GhostDeliveryState,
        startMillis: Long,
        endMillis: Long
    ) {
        ensureGhostNotificationChannels(context)
        val now = System.currentTimeMillis()
        val total = (endMillis - startMillis).coerceAtLeast(1L)
        val elapsed = (now - startMillis).coerceIn(0L, total)
        val progressPercent = ((elapsed * 100L) / total).toInt().coerceIn(0, 100)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("cooldownId", itemId)
        }
        val requestCode = notificationId(itemId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val progressStyle = NotificationCompat.ProgressStyle()
            .setStyledByProgress(false)
            .setProgress(progressPercent)
            .addProgressSegment(NotificationCompat.ProgressStyle.Segment(100).setColor(GHOST_LIVE_UPDATE_COLOR))

        // Declaring POST_PROMOTED_NOTIFICATIONS in the manifest is necessary
        // but not sufficient - the OS/user still gate actual promotion at
        // runtime. Only request it when this returns true; otherwise this
        // still posts as a normal ongoing notification (everything else on
        // the builder is unaffected), it just never asks to be promoted.
        val canPromote = runCatching {
            NotificationManagerCompat.from(context).canPostPromotedNotifications()
        }.getOrDefault(false)

        val builder = NotificationCompat.Builder(context, GHOST_DELIVERY_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_ghost_icon)
            .setContentTitle(stageLabel(state))
            .setContentText(productName)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pendingIntent)
            .setColorized(true)
            .setColor(GHOST_LIVE_UPDATE_COLOR)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endMillis)
            .setStyle(progressStyle)
            .apply { if (canPromote) setRequestPromotedOngoing(true) }

        try {
            NotificationManagerCompat.from(context).notify(GHOST_LIVE_UPDATE_TAG, notificationId(itemId), builder.build())
        } catch (_: SecurityException) {
            // Android 13+ permission is intentionally requested only after the first real order.
        }
    }
}
