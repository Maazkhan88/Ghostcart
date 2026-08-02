package com.example.ghostcart.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ghostcart.MainActivity
import com.ghostcart.app.R
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val NOTIFICATION_IMAGE_TIMEOUT_MS = 4_000L
const val GHOST_DELIVERY_WORK_TAG = "ghost_delivery_simulation"
const val GHOST_DELIVERY_CHANNEL_ID = "ghost_delivery_updates"

class DeliveryStepWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()
        val orderId = inputData.getString(KEY_ORDER_ID) ?: itemId
        val productName = inputData.getString(KEY_PRODUCT_NAME).orEmpty().ifBlank { "Your item" }
        val durationLabel = inputData.getString(KEY_DURATION_LABEL).orEmpty().ifBlank { "the selected time" }
        val state = inputData.getString(KEY_STATE)
            ?.let { runCatching { GhostDeliveryState.valueOf(it) }.getOrNull() }
            ?: return Result.failure()

        val copy = notificationCopy(state, productName, durationLabel) ?: return Result.success()
        Analytics.logGhostOrderEvent(applicationContext, "ghost_stage_reached", orderId, state.name)
        val image = loadRemoteProductImage(applicationContext, inputData.getString(KEY_PRODUCT_IMAGE_URL))
            ?: decodeLocalPlaceholder(applicationContext)
        showNotification(itemId, orderId, state, copy.first, copy.second, image)
        return Result.success()
    }

    private fun notificationCopy(
        state: GhostDeliveryState,
        productName: String,
        durationLabel: String
    ): Pair<String, String>? = when (state) {
        GhostDeliveryState.PLACED -> "Ghost Order placed" to
            "$productName is cooling for $durationLabel. Nothing has been purchased."
        GhostDeliveryState.PREPARING -> "Your Ghost Order is being prepared" to
            "Giving this almost-buy some space."
        GhostDeliveryState.RIDER_PICKING_UP -> "Ghost Rider is picking up your order" to
            "Your decision is still cooling."
        GhostDeliveryState.OUT_FOR_DELIVERY -> "Out for Ghost Delivery" to
            "Your $productName decision is on the way."
        GhostDeliveryState.RIDER_NEARBY -> "Your Ghost Rider is nearby" to
            "Your decision will be ready soon."
        GhostDeliveryState.DELIVERED -> "Your Ghost Order has arrived" to
            "Skip it, buy it, cool it again or ask a friend."
        else -> null
    }

    private suspend fun loadRemoteProductImage(context: Context, imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                withTimeoutOrNull(NOTIFICATION_IMAGE_TIMEOUT_MS) {
                    val cacheFile = File(context.cacheDir, "delivery_${imageUrl.hashCode()}.png")
                    if (cacheFile.exists()) {
                        BitmapFactory.decodeFile(cacheFile.absolutePath)
                    } else {
                        val connection = URL(imageUrl).openConnection() as HttpURLConnection
                        connection.connectTimeout = NOTIFICATION_IMAGE_TIMEOUT_MS.toInt()
                        connection.readTimeout = NOTIFICATION_IMAGE_TIMEOUT_MS.toInt()
                        connection.inputStream.use { stream ->
                            val bytes = stream.readBytes()
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.also {
                                runCatching { cacheFile.outputStream().use { out -> out.write(bytes) } }
                            }
                        }
                    }
                }
            }.getOrNull()
        }
    }

    private fun decodeLocalPlaceholder(context: Context): Bitmap? =
        runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.mascot_cart) }.getOrNull()

    private fun letterboxToSquare(source: Bitmap): Bitmap {
        if (source.width == source.height) return source
        val size = maxOf(source.width, source.height)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { square ->
            Canvas(square).apply {
                drawColor(Color.WHITE)
                drawBitmap(source, (size - source.width) / 2f, (size - source.height) / 2f, null)
            }
        }
    }

    private fun showNotification(
        itemId: String,
        orderId: String,
        state: GhostDeliveryState,
        title: String,
        text: String,
        largeIcon: Bitmap?
    ) {
        val context = applicationContext
        ensureGhostNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("cooldownId", itemId)
            putExtra("ghostOrderId", orderId)
            putExtra("ghostDeliveryState", state.name)
        }
        val notificationId = stableNotificationId(orderId, state)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, GHOST_DELIVERY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ghost_cart_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("ghost_delivery_$orderId")

        largeIcon?.let { builder.setLargeIcon(letterboxToSquare(it)) }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Android 13+ permission is intentionally requested only after the first real order.
        }
    }

    companion object {
        const val KEY_ITEM_ID = "itemId"
        const val KEY_ORDER_ID = "orderId"
        const val KEY_PRODUCT_NAME = "productName"
        const val KEY_PRODUCT_IMAGE_URL = "productImageUrl"
        const val KEY_DURATION_LABEL = "durationLabel"
        const val KEY_STATE = "state"

        fun stableNotificationId(orderId: String, state: GhostDeliveryState): Int =
            ("$orderId:${state.name}".hashCode() and 0x7FFFFFFF).coerceAtLeast(1)
    }
}
