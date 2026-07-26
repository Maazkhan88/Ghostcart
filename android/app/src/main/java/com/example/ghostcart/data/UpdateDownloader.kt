package com.example.ghostcart.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Downloads the release APK via the system DownloadManager (progress +
// notification handled by the OS, same as any browser download) and, once
// complete, launches the system package installer against the downloaded
// file's content:// URI. No FileProvider needed - DownloadManager already
// returns a grantable content URI for its own downloads.
object UpdateDownloader {

    suspend fun downloadAndPromptInstall(context: Context, apkUrl: String, versionName: String): Result<Unit> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            error("Allow Ghost Cart to install updates, then try again")
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Ghost Cart update")
            .setDescription("Downloading version $versionName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "ghost-cart-update.apk")
            .setMimeType("application/vnd.android.package-archive")

        val downloadId = downloadManager.enqueue(request)

        val downloadedUri = awaitDownloadComplete(context, downloadManager, downloadId)
            ?: error("The update download did not complete")

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(downloadedUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    private suspend fun awaitDownloadComplete(context: Context, downloadManager: DownloadManager, downloadId: Long): Uri? =
        suspendCancellableCoroutine { continuation ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (completedId != downloadId) return
                    runCatching { context.unregisterReceiver(this) }
                    if (continuation.isActive) {
                        continuation.resume(runCatching { downloadManager.getUriForDownloadedFile(downloadId) }.getOrNull())
                    }
                }
            }
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
            continuation.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
        }
}
