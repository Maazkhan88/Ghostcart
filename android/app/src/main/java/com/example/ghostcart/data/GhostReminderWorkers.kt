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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.ghostcart.MainActivity
import com.example.ghostcart.ui.app.delayUntilHourFrom
import com.ghostcart.app.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val PREFS_NAME = "ghost_cart_prefs"
private const val COOLING_NOTIFICATIONS_KEY = "cooling_notifications_enabled"

class CoolingReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (!preferenceEnabled(applicationContext, COOLING_NOTIFICATIONS_KEY, true)) return Result.success()

        val productName = inputData.getString("productName") ?: "Your item"
        GhostNotificationPublisher.show(
            context = applicationContext,
            channelId = "ghost_cooling_reminders",
            channelName = "Cooling reminders",
            channelDescription = "Updates when a Ghost Cart cooling period is complete",
            notificationId = 2200 + productName.hashCode().and(0x7FFF),
            title = "Cooling complete 👻",
            text = "$productName has cooled off. Do you still want it?",
            cooldownId = inputData.getString("productId")
        )
        return Result.success()
    }
}

class DailyGhostReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val meal = inputData.getString("meal") ?: "meal"
        val hourOfDay = inputData.getInt("hourOfDay", if (meal == "lunch") 13 else 20)
        val preferenceKey = if (meal == "lunch") "lunch_reminder_enabled" else "dinner_reminder_enabled"
        if (!preferenceEnabled(applicationContext, preferenceKey, false)) return Result.success()
        val (title, text) = when (meal) {
            "lunch" -> "Lunch temptation? Ghost it first 👻" to
                "Capture the lunch order and give the craving time to cool."
            "dinner" -> "Thinking about dinner delivery? 👻" to
                "Ghost the dinner order first, then decide intentionally."
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

        // Re-anchor the next firing from the actual current time rather than letting a
        // WorkManager PeriodicWorkRequest drift after a Doze-deferred run. Only reschedules
        // while the preference is still enabled; toggling it off cancels this unique work
        // from AppViewModel.syncDailyGhostReminder instead.
        val nextRequest = OneTimeWorkRequestBuilder<DailyGhostReminderWorker>()
            .setInitialDelay(delayUntilHourFrom(Calendar.getInstance(), hourOfDay), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("meal" to meal, "hourOfDay" to hourOfDay))
            .addTag("ghost_daily_reminder")
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "ghost_daily_$meal",
            ExistingWorkPolicy.REPLACE,
            nextRequest
        )
        return Result.success()
    }
}

private fun preferenceEnabled(context: Context, key: String, default: Boolean): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(key, default)

object GhostNotificationPublisher {
    fun show(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String,
        notificationId: Int,
        title: String,
        text: String,
        cooldownId: String? = null
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            cooldownId?.let { putExtra("cooldownId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ghost_cart_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Same three outcomes as the Cooldowns screen's resolve buttons,
        // available right from the notification - only makes sense when this
        // notification is actually about a specific cooldown.
        if (cooldownId != null) {
            builder.addAction(actionFor(context, notificationId, cooldownId, CooldownNotificationActionReceiver.ACTION_SKIPPED, "I skipped it"))
            builder.addAction(actionFor(context, notificationId, cooldownId, CooldownNotificationActionReceiver.ACTION_BOUGHT, "Bought it"))
            builder.addAction(actionFor(context, notificationId, cooldownId, CooldownNotificationActionReceiver.ACTION_MORE_TIME, "More time"))
        }
        val notification = builder.build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Android 13+ notification permission has not been granted.
        }
    }

    private fun actionFor(
        context: Context,
        notificationId: Int,
        cooldownId: String,
        action: String,
        label: String
    ): NotificationCompat.Action {
        val intent = Intent(context, CooldownNotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(CooldownNotificationActionReceiver.EXTRA_COOLDOWN_ID, cooldownId)
            putExtra(CooldownNotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        // Request code mixes the action into the notification id so all three
        // buttons on the same notification get distinct PendingIntents rather
        // than overwriting each other (FLAG_UPDATE_CURRENT keys off it).
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + when (action) {
                CooldownNotificationActionReceiver.ACTION_SKIPPED -> 1
                CooldownNotificationActionReceiver.ACTION_BOUGHT -> 2
                else -> 3
            },
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action.Builder(0, label, pendingIntent).build()
    }
}
