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
  tertiary = Color(0xFF92E27F),
  background = Color(0xFF080909),
  surface = Color(0xFF151716),
  surfaceVariant = Color(0xFF202320),
  surfaceContainer = Color(0xFF151716),
  surfaceContainerHigh = Color(0xFF202320),
  outline = Color(0xFF3A3D39),
  outlineVariant = Color(0xFF2A2D2A),
  onPrimary = Color(0xFF050505),
  onSecondary = Color(0xFF0C0C0C),
  onBackground = Color(0xFFF7F7F5),
  onSurface = Color(0xFFF7F7F5),
  onSurfaceVariant = Color(0xFFB9BDB7)
)

private val LightColorScheme = lightColorScheme(
  primary = GhostGreen,
  secondary = Color(0xFF050505),
  tertiary = Color(0xFF1F8F3A),
  background = Color(0xFFF7F8F6),
  surface = Color(0xFFFFFFFF),
  surfaceVariant = Color(0xFFF0F2EE),
  surfaceContainer = Color(0xFFFFFFFF),
  surfaceContainerHigh = Color(0xFFF0F2EE),
  outline = Color(0xFF7A7F78),
  outlineVariant = Color(0xFFDDE0DA),
  onPrimary = Color(0xFF050505),
  onSecondary = Color(0xFFFFFFFF),
  onBackground = Color(0xFF050505),
  onSurface = Color(0xFF050505),
  onSurfaceVariant = Color(0xFF5F645E)
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
