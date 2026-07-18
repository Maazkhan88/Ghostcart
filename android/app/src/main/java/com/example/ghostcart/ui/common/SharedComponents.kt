package com.example.ghostcart.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.theme.DarkGray
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper

/** Small back-chevron, hand-drawn to avoid depending on a specific icon-pack symbol name. */
@Composable
fun BackChevron(modifier: Modifier = Modifier, color: Color = Ink) {
    Canvas(modifier = modifier.size(14.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.75f, 0f)
            lineTo(size.width * 0.15f, size.height * 0.5f)
            lineTo(size.width * 0.75f, size.height)
        }
        drawPath(path, color, style = Stroke(width = size.width * 0.16f))
    }
}

/** Small forward-chevron for row disclosure affordances. */
@Composable
fun ForwardChevron(modifier: Modifier = Modifier, color: Color = MutedText) {
    Canvas(modifier = modifier.size(12.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.25f, 0f)
            lineTo(size.width * 0.85f, size.height * 0.5f)
            lineTo(size.width * 0.25f, size.height)
        }
        drawPath(path, color, style = Stroke(width = size.width * 0.18f))
    }
}

@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Paper)
            .border(1.dp, FaintBorder, CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        BackChevron()
    }
}

/** Shared top bar: back button, centered title, optional trailing action. */
@Composable
fun GhostTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            BackButton(onBack = onBack)
        } else {
            Box(modifier = Modifier.size(38.dp))
        }
        Text(
            text = title,
            color = Ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        )
        trailing()
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Paper)
            .border(1.dp, FaintBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
    }
}

/** Compact disclosure used only at entry and sensitive checkout moments. */
@Composable
fun SimulationBadge(text: String = "Demo", modifier: Modifier = Modifier, dark: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFEFEFEF))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = if (dark) Paper else MutedText,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = text,
            color = if (dark) Paper else MutedText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** The recurring dark rounded "hero" card (wallet balance, statements, protection banners). */
@Composable
fun GhostHeroCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Ink,
    decorationColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val waveColor = decorationColor.copy(alpha = 0.08f)
            for (i in 0..3) {
                val yOffset = size.height * (0.2f + i * 0.22f)
                val path = Path().apply {
                    moveTo(size.width * 0.35f, yOffset)
                    quadraticTo(size.width * 0.72f, yOffset - 22f, size.width * 1.05f, yOffset + 14f)
                }
                drawPath(path, waveColor, style = Stroke(width = 1.5f))
            }
        }
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    containerColor: Color = Ink,
    contentColor: Color = Paper
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Box(modifier = Modifier.size(8.dp))
        }
        Text(text = text, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (trailingIcon != null) {
            Box(modifier = Modifier.size(8.dp))
            Icon(trailingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leadingIcon: ImageVector? = null) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Paper)
            .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
            Box(modifier = Modifier.size(8.dp))
        }
        Text(text = text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ThinProgressBar(progress: Float, modifier: Modifier = Modifier, trackColor: Color = FaintBorder, fillColor: Color = GhostGreen) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(fillColor)
        )
    }
}

@Composable
fun RowScope.StatColumn(label: String, value: String, valueColor: Color = Ink) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = label, color = MutedText, fontSize = 11.sp)
        Text(text = value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, background: Color = Color(0xFFF4F4F4), tint: Color = Ink) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    trailingTextColor: Color = GhostGreen,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 10.dp)
    ) {
        IconBadge(icon = icon)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(text = title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(text = subtitle, color = MutedText, fontSize = 11.sp)
            }
        }
        if (trailingText != null) {
            Text(text = trailingText, color = trailingTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.size(6.dp))
        }
        if (showChevron) {
            ForwardChevron()
        }
    }
}

@Composable
fun CircularGoalRing(percent: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(64.dp)) {
            val strokeWidth = size.width * 0.11f
            drawArc(
                color = FaintBorder,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = GhostGreen,
                startAngle = -90f,
                sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
        }
        Text(text = "$percent%", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Ink,
        fontSize = 17.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
    )
}
