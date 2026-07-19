package com.example.ghostcart.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
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

class DeliveryStepWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val orderId = inputData.getString("orderId") ?: "GHOST-00000"
        val amountSaved = inputData.getInt("amountSaved", 0)
        val stepIndex = inputData.getInt("stepIndex", 1)
        val productImageUrl = inputData.getString("productImageUrl")

        val steps = listOf(
            "Preparing imaginary order" to "Our team is carefully doing nothing.",
            "Ghost Rider is on the way" to "Zooming through the void.",
            "Rider left absolutely nothing at your doorstep" to "Yep, nothing's there.",
            "Fake delivery complete" to "Thanks for choosing smart savings. You avoided spending AED $amountSaved!"
        )

        val (title, text) = if (stepIndex in 1..4) {
            steps[stepIndex - 1]
        } else {
            "Ghost Cart Status" to "Update on order $orderId"
        }

        // Image loading must never delay or block the notification itself: the plain text
        // notification always posts below, and a picture is only *additionally* attached when
        // one is ready within a short timeout, falling back to a local placeholder rather than
        // no image at all when the remote fetch fails or times out.
        val largeIcon = if (stepIndex == 4) {
            // "Delivered" is about the whole order arriving on the doorstep, not a specific
            // product - use a local placeholder stand-in until a real doorstep photo is
            // supplied (swap decodeLocalPlaceholder's drawable resource for the real asset).
            decodeLocalPlaceholder(applicationContext)
        } else {
            loadRemoteProductImage(applicationContext, productImageUrl)
                ?: decodeLocalPlaceholder(applicationContext)
        }

        showNotification(title, text, largeIcon)

        return Result.success()
    }

    private suspend fun loadRemoteProductImage(context: Context, imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                withTimeoutOrNull(NOTIFICATION_IMAGE_TIMEOUT_MS) {
                    // Cache the decoded bytes on disk keyed by URL so the later delivery steps
                    // for the same order/product reuse the download instead of refetching.
                    val cacheFile = File(context.cacheDir, "notif_img_${imageUrl.hashCode()}.png")
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

    private fun showNotification(title: String, text: String, largeIcon: Bitmap?) {
        val channelId = "ghost_delivery_channel"
        val notificationId = 1001 + inputData.getInt("stepIndex", 1)

        val context = applicationContext

        // Create notification channel if on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Ghost Cart Delivery Updates"
            val descriptionText = "Notifications for the simulated ghost order delivery steps"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ghost_cart_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder
                .setLargeIcon(largeIcon)
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(largeIcon))
        }

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }
}
