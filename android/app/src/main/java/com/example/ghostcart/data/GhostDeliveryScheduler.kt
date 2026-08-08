package com.example.ghostcart.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Locale
import java.util.concurrent.TimeUnit

object GhostDeliveryScheduler {
    fun schedule(context: Context, item: AlmostBuy, nowMillis: Long = System.currentTimeMillis()) {
        if (item.tutorialOnly || item.status != AlmostBuyStatus.COOLING) return
        ensureGhostNotificationChannels(context)
        val workManager = WorkManager.getInstance(context)
        cancel(context, item.id)
        val orderId = item.ghostOrderId ?: item.id
        val durationLabel = formatGhostDeliveryDuration(item.selectedDeliveryDurationMillis)
        GhostDeliveryLiveUpdate.start(context, item.id, item.name, item.deliveryStartedAtMillis, item.deliveryEndsAtMillis)
        ghostStageSchedule(item.deliveryStartedAtMillis, item.deliveryEndsAtMillis)
            .filter {
                it.triggerAtMillis >= nowMillis ||
                    (it.state == GhostDeliveryState.PLACED && nowMillis - item.deliveryStartedAtMillis < 30_000L)
            }
            .forEach { scheduled ->
                val triggerAt = if (scheduled.state == GhostDeliveryState.PLACED && scheduled.triggerAtMillis < nowMillis) {
                    nowMillis + 3_000L
                } else scheduled.triggerAtMillis
                val request = OneTimeWorkRequestBuilder<DeliveryStepWorker>()
                    .setInitialDelay((triggerAt - nowMillis).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            DeliveryStepWorker.KEY_ITEM_ID to item.id,
                            DeliveryStepWorker.KEY_ORDER_ID to orderId,
                            DeliveryStepWorker.KEY_PRODUCT_NAME to item.name,
                            DeliveryStepWorker.KEY_PRODUCT_IMAGE_URL to item.imageUrl,
                            DeliveryStepWorker.KEY_DURATION_LABEL to durationLabel,
                            DeliveryStepWorker.KEY_STATE to scheduled.state.name,
                            DeliveryStepWorker.KEY_START_MILLIS to item.deliveryStartedAtMillis,
                            DeliveryStepWorker.KEY_END_MILLIS to item.deliveryEndsAtMillis
                        )
                    )
                    .addTag(GHOST_DELIVERY_WORK_TAG)
                    .addTag(tagForItem(item.id))
                    .build()
                workManager.enqueueUniqueWork(
                    uniqueWorkName(item.id, scheduled.state),
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
    }

    fun cancel(context: Context, itemId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagForItem(itemId))
        GhostDeliveryLiveUpdate.cancel(context, itemId)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(GHOST_DELIVERY_WORK_TAG)
        GhostDeliveryLiveUpdate.cancelAll(context)
    }

    fun restore(context: Context, items: List<AlmostBuy>, nowMillis: Long = System.currentTimeMillis()) {
        items.asSequence()
            .filter { it.status == AlmostBuyStatus.COOLING && !it.tutorialOnly }
            .forEach { schedule(context, it, nowMillis) }
    }

    private fun uniqueWorkName(itemId: String, state: GhostDeliveryState): String =
        "ghost_delivery_${itemId}_${state.name.lowercase(Locale.US)}"

    private fun tagForItem(itemId: String): String = "ghost_delivery_item_$itemId"
}

fun formatGhostDeliveryDuration(durationMillis: Long): String = when {
    durationMillis % TimeUnit.DAYS.toMillis(7) == 0L && durationMillis >= TimeUnit.DAYS.toMillis(7) -> {
        val weeks = durationMillis / TimeUnit.DAYS.toMillis(7)
        "$weeks ${if (weeks == 1L) "week" else "weeks"}"
    }
    durationMillis % TimeUnit.DAYS.toMillis(1) == 0L && durationMillis >= TimeUnit.DAYS.toMillis(1) -> {
        val days = durationMillis / TimeUnit.DAYS.toMillis(1)
        "$days ${if (days == 1L) "day" else "days"}"
    }
    durationMillis % TimeUnit.HOURS.toMillis(1) == 0L && durationMillis >= TimeUnit.HOURS.toMillis(1) -> {
        val hours = durationMillis / TimeUnit.HOURS.toMillis(1)
        "$hours ${if (hours == 1L) "hour" else "hours"}"
    }
    else -> {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis).coerceAtLeast(1)
        "$minutes ${if (minutes == 1L) "minute" else "minutes"}"
    }
}
