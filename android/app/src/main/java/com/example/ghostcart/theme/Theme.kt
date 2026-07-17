package com.example.ghostcart.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = GhostGreen,
  secondary = Color(0xFFB9B9B3),
  background = Color(0xFF0C0C0C),
  surface = Color(0xFF1B1B1B),
  onPrimary = Color(0xFF050505),
  onSecondary = Color(0xFF0C0C0C),
  onBackground = Color(0xFFF7F7F5),
  onSurface = Color(0xFFF7F7F5)
)

private val LightColorScheme = lightColorScheme(
  primary = GhostGreen,
  secondary = Color(0xFF050505),
  background = Color(0xFFFFFFFF),
  surface = Color(0xFFF4F4F4),
  onPrimary = Color(0xFF050505),
  onSecondary = Color(0xFFFFFFFF),
  onBackground = Color(0xFF050505),
  onSurface = Color(0xFF050505)
)

@Composable
fun GhostCartTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to enforce brand palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  SideEffect { GhostPaletteState.darkMode = darkTheme }
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
