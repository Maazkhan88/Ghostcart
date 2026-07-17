package com.example.ghostcart.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ghostcart.MainActivity
import com.ghostcart.app.R

class DeliveryStepWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val orderId = inputData.getString("orderId") ?: "GHOST-00000"
        val amountSaved = inputData.getInt("amountSaved", 0)
        val stepIndex = inputData.getInt("stepIndex", 1)

        val steps = listOf(
            "Order placed" to "We've received your imaginary order.",
            "Preparing imaginary order" to "Our team is carefully doing nothing.",
            "Ghost Rider is on the way" to "Zooming through the void.",
            "Rider left absolutely nothing at your doorstep" to "Yep, nothing's there.",
            "Fake delivery complete" to "Thanks for choosing smart savings. You avoided spending AED $amountSaved!"
        )

        val (title, text) = if (stepIndex in 1..5) {
            steps[stepIndex - 1]
        } else {
            "Ghost Cart Status" to "Update on order $orderId"
        }

        showNotification(title, text)

        return Result.success()
    }

    private fun showNotification(title: String, text: String) {
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

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }
}
