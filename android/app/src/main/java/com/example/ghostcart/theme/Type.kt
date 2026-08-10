package com.example.ghostcart.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.ghostcart.app.R

// SIL Open Font License 1.1 - see android/licenses/GoogleSans-OFL.txt
val GoogleSansFamily = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_bold, FontWeight.Bold),
)

// Every Material3 type scale slot, on Google Sans - sizes/weights/spacing stay the M3 defaults,
// only the font family changes. Deliberately not used for the GhostCart wordmark, which is a
// logo image asset (GhostCartWordmark), not rendered text.
val Typography = Typography().let { default ->
    Typography(
        displayLarge = default.displayLarge.copy(fontFamily = GoogleSansFamily),
        displayMedium = default.displayMedium.copy(fontFamily = GoogleSansFamily),
        displaySmall = default.displaySmall.copy(fontFamily = GoogleSansFamily),
        headlineLarge = default.headlineLarge.copy(fontFamily = GoogleSansFamily),
        headlineMedium = default.headlineMedium.copy(fontFamily = GoogleSansFamily),
        headlineSmall = default.headlineSmall.copy(fontFamily = GoogleSansFamily),
        titleLarge = default.titleLarge.copy(fontFamily = GoogleSansFamily),
        titleMedium = default.titleMedium.copy(fontFamily = GoogleSansFamily),
        titleSmall = default.titleSmall.copy(fontFamily = GoogleSansFamily),
        bodyLarge = default.bodyLarge.copy(fontFamily = GoogleSansFamily),
        bodyMedium = default.bodyMedium.copy(fontFamily = GoogleSansFamily),
        bodySmall = default.bodySmall.copy(fontFamily = GoogleSansFamily),
        labelLarge = default.labelLarge.copy(fontFamily = GoogleSansFamily),
        labelMedium = default.labelMedium.copy(fontFamily = GoogleSansFamily),
        labelSmall = default.labelSmall.copy(fontFamily = GoogleSansFamily),
    )
}
