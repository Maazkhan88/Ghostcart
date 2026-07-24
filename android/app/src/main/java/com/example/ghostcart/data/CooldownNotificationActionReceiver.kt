package com.example.ghostcart.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Handles the three action buttons on a "Cooling complete" notification
 * (I skipped it / Bought it / More time) without requiring the app to be
 * opened - same three outcomes as CooldownDecisionCard's buttons on the
 * Cooldowns screen, reusing the same repository/sync calls AppViewModel
 * uses so both paths stay consistent.
 *
 * The notification's cooldownId extra may be either the item's local id
 * (from the on-device CoolingReminderWorker reminder) or its serverId
 * (from the backend's push - see cooldown-push-sweep.ts, which only knows
 * the server-side almost_buys.id). Matched against both so either
 * notification source resolves the right item.
 */
class CooldownNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cooldownId = intent.getStringExtra(EXTRA_COOLDOWN_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val action = intent.action ?: return
        val appContext = context.applicationContext

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                Analytics.logNotificationActionTapped(appContext, action)
                val repository = LocalAlmostBuyRepository(appContext)
                val localId = repository.items.first()
                    .firstOrNull { it.id == cooldownId || it.serverId == cooldownId }
                    ?.id
                if (localId != null) {
                    when (action) {
                        ACTION_SKIPPED -> resolve(appContext, repository, localId, AlmostBuyResolution.SKIPPED)
                        ACTION_BOUGHT -> resolve(appContext, repository, localId, AlmostBuyResolution.BOUGHT_INTENTIONALLY)
                        ACTION_MORE_TIME -> extend(appContext, repository, localId)
                    }
                }
            } finally {
                if (notificationId != -1) {
                    NotificationManagerCompat.from(appContext).cancel(notificationId)
                }
                pendingResult.finish()
            }
        }
    }

    private suspend fun resolve(
        context: Context,
        repository: AlmostBuyRepository,
        id: String,
        resolution: AlmostBuyResolution
    ) {
        val item = repository.resolve(id, resolution) ?: return
        Analytics.logCooldownResolved(context, resolution.name.lowercase())
        WorkManager.getInstance(context).cancelUniqueWork("ghost_cooling_${item.id}")
        item.serverId?.let { serverId -> AlmostBuySync.syncResolve(context, serverId, resolution) }
    }

    private suspend fun extend(context: Context, repository: AlmostBuyRepository, id: String) {
        val item = repository.extendCooling(id, TimeUnit.HOURS.toMillis(24)) ?: return
        val delayMillis = (item.coolingUntilMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
        val request = OneTimeWorkRequestBuilder<CoolingReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("productName" to item.name, "productId" to item.id))
            .addTag("ghost_cooling_reminder")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "ghost_cooling_${item.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val ACTION_SKIPPED = "com.example.ghostcart.action.COOLDOWN_SKIPPED"
        const val ACTION_BOUGHT = "com.example.ghostcart.action.COOLDOWN_BOUGHT"
        const val ACTION_MORE_TIME = "com.example.ghostcart.action.COOLDOWN_MORE_TIME"
        const val EXTRA_COOLDOWN_ID = "cooldownId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }
}
