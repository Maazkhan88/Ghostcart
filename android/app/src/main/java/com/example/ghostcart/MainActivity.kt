package com.example.ghostcart

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.example.ghostcart.data.extractSharedUrl
import com.example.ghostcart.theme.GhostCartTheme
import java.io.File

private data class SharedProductRequest(
  val url: String,
  val title: String?,
  val imageUrl: String?,
  val requestKey: Long
)

class MainActivity : ComponentActivity() {
  private val notificationCooldownId = mutableStateOf<String?>(null)
  private val sharedProductRequest = mutableStateOf<SharedProductRequest?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    notificationCooldownId.value = intent.getStringExtra("cooldownId")
    captureSharedProduct(intent)

    enableEdgeToEdge()

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 102)
    }

    setContent {
      GhostCartTheme(darkTheme = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          val shared = sharedProductRequest.value
          MainNavigation(
            initialCooldownId = notificationCooldownId.value,
            initialSharedUrl = shared?.url,
            initialSharedTitle = shared?.title,
            initialSharedImageUrl = shared?.imageUrl,
            sharedRequestKey = shared?.requestKey
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    notificationCooldownId.value = intent.getStringExtra("cooldownId")
    captureSharedProduct(intent)
  }

  private fun captureSharedProduct(intent: Intent) {
    if (intent.action != Intent.ACTION_SEND) return
    val sharedText = buildList {
      intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.let(::add)
      intent.getStringExtra(Intent.EXTRA_HTML_TEXT)?.let(::add)
      intent.clipData?.let { clip ->
        for (index in 0 until clip.itemCount) {
          clip.getItemAt(index).text?.toString()?.let(::add)
          clip.getItemAt(index).htmlText?.let(::add)
        }
      }
    }.joinToString(separator = "\n")
    val url = extractSharedUrl(sharedText) ?: return
    val title = intent.getCharSequenceExtra(Intent.EXTRA_TITLE)
      ?.toString()
      ?.trim()
      ?.take(160)
      ?.takeIf { it.isNotBlank() }
    sharedProductRequest.value = SharedProductRequest(
      url = url,
      title = title,
      imageUrl = persistSharedThumbnail(intent),
      requestKey = System.nanoTime()
    )
  }

  private fun persistSharedThumbnail(intent: Intent): String? {
    val candidates = buildList {
      IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let(::add)
      intent.data?.let(::add)
      intent.clipData?.let { clip ->
        for (index in 0 until clip.itemCount) clip.getItemAt(index).uri?.let(::add)
      }
    }.distinct()

    return candidates.firstNotNullOfOrNull { uri ->
      runCatching {
        val mimeType = contentResolver.getType(uri).orEmpty().lowercase()
        if (!mimeType.startsWith("image/")) return@runCatching null
        val extension = when {
          mimeType.contains("png") -> "png"
          mimeType.contains("webp") -> "webp"
          else -> "jpg"
        }
        val directory = File(filesDir, "shared-product-images").apply { mkdirs() }
        directory.listFiles()
          ?.sortedByDescending { it.lastModified() }
          ?.drop(20)
          ?.forEach { it.delete() }
        val destination = File(directory, "shared-" + System.currentTimeMillis() + "." + extension)
        val copied = contentResolver.openInputStream(uri)?.use { input ->
          destination.outputStream().use { output ->
            val buffer = ByteArray(16_384)
            var total = 0L
            while (true) {
              val count = input.read(buffer)
              if (count <= 0) break
              total += count
              if (total > 8_000_000L) {
                destination.delete()
                return@use false
              }
              output.write(buffer, 0, count)
            }
            true
          }
        } ?: false
        if (copied && destination.length() > 0L) Uri.fromFile(destination).toString() else null
      }.getOrNull()
    }
  }
}