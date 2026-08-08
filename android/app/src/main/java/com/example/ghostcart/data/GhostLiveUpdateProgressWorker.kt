package com.example.ghostcart.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

const val GHOST_LIVE_PROGRESS_WORK_TAG = "ghost_live_update_progress"

// Purely refreshes the live-update notification's progress bar fill level
// between the six delivery-stage notifications (which can be hours apart on
// a long cooldown, leaving the bar visibly frozen). Deliberately lightweight
// - no image loading, no new system notification sound/copy, just a
// re-post with the current progress percentage. Scheduled as a fixed,
// finite list of one-shot jobs (see GhostDeliveryScheduler), never a
// recurring PeriodicWorkRequest, to keep this bounded and battery-conscious.
class GhostLiveUpdateProgressWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()
        val productName = inputData.getString(KEY_PRODUCT_NAME).orEmpty().ifBlank { "Your item" }
        val startMillis = inputData.getLong(KEY_START_MILLIS, 0L)
        val endMillis = inputData.getLong(KEY_END_MILLIS, 0L)
        if (startMillis <= 0L || endMillis <= 0L) return Result.failure()

        val snapshot = ghostDeliverySnapshot(System.currentTimeMillis(), startMillis, endMillis)
        GhostDeliveryLiveUpdate.updateStage(applicationContext, itemId, productName, snapshot.state, startMillis, endMillis)
        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "itemId"
        const val KEY_PRODUCT_NAME = "productName"
        const val KEY_START_MILLIS = "startMillis"
        const val KEY_END_MILLIS = "endMillis"
    }
}
