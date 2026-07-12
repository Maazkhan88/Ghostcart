package com.example.ghostcart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ghostcart.theme.GhostGreen

@Composable
fun ProductIcon(name: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.06f

        when (name) {
            "sneaker" -> {
                // Drawing sneaker silhouette
                val path = Path().apply {
                    moveTo(w * 0.1f, h * 0.75f)
                    lineTo(w * 0.9f, h * 0.75f)
                    lineTo(w * 0.9f, h * 0.55f)
                    quadraticTo(w * 0.75f, h * 0.5f, w * 0.65f, h * 0.3f)
                    lineTo(w * 0.45f, h * 0.3f)
                    lineTo(w * 0.35f, h * 0.45f)
                    quadraticTo(w * 0.2f, h * 0.5f, w * 0.1f, h * 0.55f)
                    close()
                }
                drawPath(path, color, style = Stroke(width = strokeWidth))
                // Sole line
                drawLine(
                    color = color,
                    start = Offset(w * 0.1f, h * 0.68f),
                    end = Offset(w * 0.9f, h * 0.68f),
                    strokeWidth = strokeWidth
                )
            }
            "perfume" -> {
                // Perfume bottle
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.2f, h * 0.35f),
                    size = Size(w * 0.6f, h * 0.5f),
                    cornerRadius = CornerRadius(w * 0.1f),
                    style = Stroke(width = strokeWidth)
                )
                // Cap
                drawRect(
                    color = color,
                    topLeft = Offset(w * 0.4f, h * 0.15f),
                    size = Size(w * 0.2f, h * 0.2f),
                    style = Stroke(width = strokeWidth)
                )
                // Label
                drawRect(
                    color = color,
                    topLeft = Offset(w * 0.35f, h * 0.48f),
                    size = Size(w * 0.3f, h * 0.24f),
                    style = Stroke(width = strokeWidth * 0.6f)
                )
            }
            "burger" -> {
                // Bun top
                val pathTop = Path().apply {
                    moveTo(w * 0.15f, h * 0.4f)
                    quadraticTo(w * 0.5f, h * 0.1f, w * 0.85f, h * 0.4f)
                    close()
                }
                drawPath(pathTop, color, style = Stroke(width = strokeWidth))
                // Cheese/Meat middle layer
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.46f),
                    size = Size(w * 0.76f, h * 0.1f),
                    cornerRadius = CornerRadius(w * 0.04f),
                    style = Stroke(width = strokeWidth)
                )
                // Bun bottom
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.15f, h * 0.62f),
                    size = Size(w * 0.7f, h * 0.2f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "headphones" -> {
                // Headband arc
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.15f),
                    size = Size(w * 0.7f, h * 0.7f),
                    style = Stroke(width = strokeWidth)
                )
                // Left ear cup
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.5f),
                    size = Size(w * 0.14f, h * 0.3f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = Stroke(width = strokeWidth)
                )
                // Right ear cup
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.74f, h * 0.5f),
                    size = Size(w * 0.14f, h * 0.3f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "shield" -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    quadraticTo(w * 0.8f, h * 0.15f, w * 0.85f, h * 0.2f)
                    lineTo(w * 0.85f, h * 0.55f)
                    quadraticTo(w * 0.7f, h * 0.8f, w * 0.5f, h * 0.9f)
                    quadraticTo(w * 0.3f, h * 0.8f, w * 0.15f, h * 0.55f)
                    lineTo(w * 0.15f, h * 0.2f)
                    quadraticTo(w * 0.2f, h * 0.15f, w * 0.5f, h * 0.1f)
                }
                drawPath(path, color, style = Stroke(width = strokeWidth))
            }
            "lock" -> {
                // Body
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.2f, h * 0.45f),
                    size = Size(w * 0.6f, h * 0.45f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                // Shackle
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.3f, h * 0.18f),
                    size = Size(w * 0.4f, h * 0.5f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "leaf" -> {
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.85f)
                    quadraticTo(w * 0.15f, h * 0.45f, w * 0.55f, h * 0.15f)
                    quadraticTo(w * 0.85f, h * 0.45f, w * 0.85f, h * 0.85f)
                    quadraticTo(w * 0.55f, h * 0.85f, w * 0.15f, h * 0.85f)
                }
                drawPath(path, color, style = Stroke(width = strokeWidth))
                drawLine(
                    color = color,
                    start = Offset(w * 0.15f, h * 0.85f),
                    end = Offset(w * 0.55f, h * 0.45f),
                    strokeWidth = strokeWidth * 0.7f
                )
            }
            "wallet" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.15f, h * 0.25f),
                    size = Size(w * 0.7f, h * 0.55f),
                    cornerRadius = CornerRadius(w * 0.06f),
                    style = Stroke(width = strokeWidth)
                )
                // Flap
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.55f, h * 0.4f),
                    size = Size(w * 0.32f, h * 0.25f),
                    cornerRadius = CornerRadius(w * 0.03f),
                    style = Stroke(width = strokeWidth * 0.8f)
                )
            }
            "chart" -> {
                drawLine(color, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.2f, h * 0.4f), strokeWidth)
                drawLine(color, Offset(w * 0.5f, h * 0.8f), Offset(w * 0.5f, h * 0.2f), strokeWidth)
                drawLine(color, Offset(w * 0.8f, h * 0.8f), Offset(w * 0.8f, h * 0.5f), strokeWidth)
            }
        }
    }
}

@Composable
fun GhostMascotPose(poseName: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(54.dp)) {
            val w = size.width
            val h = size.height

            // Head and Body outline path
            val path = Path().apply {
                moveTo(w * 0.25f, h * 0.85f)
                lineTo(w * 0.25f, h * 0.45f)
                quadraticTo(w * 0.25f, h * 0.15f, w * 0.5f, h * 0.15f)
                quadraticTo(w * 0.75f, h * 0.15f, w * 0.75f, h * 0.45f)
                lineTo(w * 0.75f, h * 0.85f)
                // Ripples at bottom
                quadraticTo(w * 0.62f, h * 0.75f, w * 0.5f, h * 0.85f)
                quadraticTo(w * 0.38f, h * 0.75f, w * 0.25f, h * 0.85f)
            }
            drawPath(path, Color.White)

            // Eyes
            drawCircle(Color.Black, radius = w * 0.05f, center = Offset(w * 0.42f, h * 0.38f))
            drawCircle(Color.Black, radius = w * 0.05f, center = Offset(w * 0.58f, h * 0.38f))

            // Pose adjustments
            when (poseName) {
                "thumbsup" -> {
                    // Thumbs up drawing
                    drawCircle(GhostGreen, radius = w * 0.07f, center = Offset(w * 0.82f, h * 0.6f))
                }
                "cooldown" -> {
                    // Ice blue dot above head
                    drawCircle(Color(0xFF80D8FF), radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.05f))
                }
                "cart" -> {
                    // Draw tiny wheels on the body
                    drawCircle(Color.Gray, radius = w * 0.04f, center = Offset(w * 0.32f, h * 0.88f))
                    drawCircle(Color.Gray, radius = w * 0.04f, center = Offset(w * 0.68f, h * 0.88f))
                }
            }
        }
    }
}
