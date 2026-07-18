package com.example.ghostcart.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.theme.Ink

/**
 * Renders a monetary value prefixed with the official UAE Dirham glyph in
 * place of any "AED"/"dirhams" wording. Pass only the numeric portion
 * (e.g. "1,299.00"); the glyph carries the currency.
 */
@Composable
fun DirhamAmount(
    amount: String,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
    fontSize: TextUnit = 13.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    glyphSize: Dp = 12.dp,
    maxLines: Int = 1
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        DirhamGlyph(tint = tint, modifier = Modifier.size(glyphSize))
        Text(
            text = amount,
            color = tint,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            modifier = Modifier.padding(start = 3.dp)
        )
    }
}

/** Formats minor units (cents) as a grouped decimal with no currency word. */
fun formatDirhamAmount(amountCents: Long): String =
    java.text.DecimalFormat("#,##0.00").format(amountCents / 100.0)
