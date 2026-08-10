package com.example.ghostcart.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** App-wide semantic palette. Existing screens use these role names directly. */
object GhostPaletteState {
    var darkMode by mutableStateOf(false)
}

val Ink: Color get() = if (GhostPaletteState.darkMode) Color(0xFFF7F7F5) else Color(0xFF050505)
val Paper: Color get() = if (GhostPaletteState.darkMode) Color(0xFF0C0C0C) else Color(0xFFFFFFFF)
val SoftGray: Color get() = if (GhostPaletteState.darkMode) Color(0xFF1B1B1B) else Color(0xFFF4F4F4)
val DarkGray = Color(0xFF161616)
val GhostGreen = Color(0xFF64D64A)
val ShadowGray: Color get() = if (GhostPaletteState.darkMode) Color(0xFF30302F) else Color(0xFFD8D8D8)

// Light-app palette (Ghost Wallet / marketplace screens, matching the mobile-ui references)
val MutedText: Color get() = if (GhostPaletteState.darkMode) Color(0xFFB9B9B3) else Color(0xFF454541)
val FaintBorder: Color get() = if (GhostPaletteState.darkMode) Color(0xFF343432) else Color(0xFFE8E8E3)
val GreenTint: Color get() = if (GhostPaletteState.darkMode) Color(0xFF18301A) else Color(0xFFE3F6DE)
val DangerRed = Color(0xFFE0453C)

// App-specific semantic surface roles, independent of the M3 ColorScheme.
// These stay separate from MaterialTheme.colorScheme's roles so existing
// call sites keep working unchanged as the app adopts real M3E APIs.
val ExpressiveBackground: Color get() = if (GhostPaletteState.darkMode) Color(0xFF080909) else Color(0xFFF7F8F6)
val ExpressiveSurface: Color get() = if (GhostPaletteState.darkMode) Color(0xFF121413) else Color(0xFFFFFFFF)
val ExpressiveSurfaceHigh: Color get() = if (GhostPaletteState.darkMode) Color(0xFF1A1C1B) else Color(0xFFF0F2EF)
val GhostGlass: Color get() = if (GhostPaletteState.darkMode) Color(0xD1161817) else Color(0xEFFFFFFF)
val GhostGlassHighlight: Color get() = if (GhostPaletteState.darkMode) Color(0x1FFFFFFF) else Color(0xFFFFFFFF)
val GhostSubtleBorder: Color get() = if (GhostPaletteState.darkMode) Color(0x14FFFFFF) else Color(0x14050505)
val ExpressivePrimaryText: Color get() = if (GhostPaletteState.darkMode) Color(0xFFF7F8F6) else Color(0xFF080909)
val ExpressiveSecondaryText: Color get() = if (GhostPaletteState.darkMode) Color(0xFFAEB3AE) else Color(0xFF626762)
val GhostOnGreen = Color(0xFF071006)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
