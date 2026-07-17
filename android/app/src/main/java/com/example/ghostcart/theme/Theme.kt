package com.example.ghostcart.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = GhostGreen,
  secondary = SoftGray,
  background = Ink,
  surface = DarkGray,
  onPrimary = Ink,
  onSecondary = Paper,
  onBackground = Paper,
  onSurface = Paper
)

private val LightColorScheme = lightColorScheme(
  primary = GhostGreen,
  secondary = Ink,
  background = Paper,
  surface = SoftGray,
  onPrimary = Ink,
  onSecondary = Paper,
  onBackground = Ink,
  onSurface = Ink
)

@Composable
fun GhostCartTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to enforce brand palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
