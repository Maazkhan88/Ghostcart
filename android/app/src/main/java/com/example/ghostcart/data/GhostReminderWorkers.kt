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
import com.example.ghostcart.R

private const val PREFS_NAME = "ghost_cart_prefs"
private const val NOTIFICATIONS_ENABLED_KEY = "wallet_notifications_enabled"

class CoolingReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (!notificationsEnabled(applicationContext)) return Result.success()

        val productName = inputData.getString("productName") ?: "Your item"
        GhostNotificationPublisher.show(
            context = applicationContext,
            channelId = "ghost_cooling_reminders",
            channelName = "Cooling reminders",
            channelDescription = "Updates when a Ghost Cart cooling period is complete",
            notificationId = 2200 + productName.hashCode().and(0x7FFF),
            title = "Cooling complete 👻",
            text = "$productName has cooled off. Do you still want it?"
        )
        return Result.success()
    }
}

class DailyGhostReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (!notificationsEnabled(applicationContext)) return Result.success()

        val meal = inputData.getString("meal") ?: "meal"
        val (title, text) = when (meal) {
            "lunch" -> "Lunch temptation? Ghost it first 👻" to
                "Build the lunch order, fake-checkout, and keep your money."
            "dinner" -> "Thinking about dinner delivery? 👻" to
                "Ghost the dinner order first and give the craving time to cool."
            else -> "Feeling tempted? Ghost it first 👻" to
                "Put the craving in Ghost Cart before you spend."
        }

        GhostNotificationPublisher.show(
            context = applicationContext,
            channelId = "ghost_daily_reminders",
            channelName = "Daily Ghost reminders",
            channelDescription = "Optional lunch and dinner reminders from Ghost Cart",
            notificationId = if (meal == "lunch") 2301 else 2302,
            title = title,
            text = text
        )
        return Result.success()
    }
}

private fun notificationsEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(NOTIFICATIONS_ENABLED_KEY, true)

private object GhostNotificationPublisher {
    fun show(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String,
        notificationId: Int,
        title: String,
        text: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = channelDescription }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ghost_cart_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Android 13+ notification permission has not been granted.
        }
    }
}
