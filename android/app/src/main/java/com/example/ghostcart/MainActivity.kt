package com.example.ghostcart

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.example.ghostcart.data.extractSupportedRetailerUrl
import com.example.ghostcart.theme.GhostCartTheme

class MainActivity : ComponentActivity() {
  private val notificationCooldownId = mutableStateOf<String?>(null)
  private val sharedProductRequest = mutableStateOf<Pair<String, Long>?>(null)

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
          MainNavigation(
            initialCooldownId = notificationCooldownId.value,
            initialSharedUrl = sharedProductRequest.value?.first,
            sharedRequestKey = sharedProductRequest.value?.second
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
    if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return
    val url = extractSupportedRetailerUrl(intent.getStringExtra(Intent.EXTRA_TEXT)) ?: return
    sharedProductRequest.value = url to System.nanoTime()
  }
}