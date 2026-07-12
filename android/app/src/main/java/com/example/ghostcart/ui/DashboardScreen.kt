package com.example.ghostcart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.theme.DarkGray
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.Paper

@Composable
fun DashboardScreen(
    ghostedCount: Int,
    cooledCount: Int,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers
        Column {
            Text(
                text = "ALMOST BOUGHT",
                color = GhostGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Insights & metrics.",
                color = Paper,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Sample data · not a user claim",
                color = Color.Gray,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label = "Saved Urges",
                value = "$ghostedCount",
                disclaimer = "Item count",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Cooled Urges",
                value = "$cooledCount",
                disclaimer = "Demo pattern",
                modifier = Modifier.weight(1f)
            )
        }

        // Donut Chart Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkGray)
                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cravings by Category",
                color = Paper,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sample data · not a user claim",
                color = Color.Gray,
                fontSize = 8.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DonutChart(
                    slices = listOf(
                        DonutSlice(Color(0xFFD0E5FF), 0.35f), // Fashion
                        DonutSlice(Color(0xFFFFF3D6), 0.25f), // Beauty
                        DonutSlice(Color(0xFFFFD6D6), 0.20f), // Delivery
                        DonutSlice(Color(0xFFD6FFE5), 0.20f)  // Tech
                    ),
                    modifier = Modifier.size(120.dp)
                )

                // Legends
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LegendItem(Color(0xFFD0E5FF), "Fashion (35%)")
                    LegendItem(Color(0xFFFFF3D6), "Beauty (25%)")
                    LegendItem(Color(0xFFFFD6D6), "Delivery (20%)")
                    LegendItem(Color(0xFFD6FFE5), "Tech (20%)")
                }
            }
        }

        // Line Chart Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkGray)
                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Your weekly mood & mindset",
                color = Paper,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sample data · not a user claim",
                color = Color.Gray,
                fontSize = 8.sp
            )

            LineChart(
                points = listOf(4f, 6f, 3f, 8f, 5f, 9f, 6f), // Monday to Sunday values (out of 10)
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mon", color = Color.Gray, fontSize = 9.sp)
                Text("Tue", color = Color.Gray, fontSize = 9.sp)
                Text("Wed", color = Color.Gray, fontSize = 9.sp)
                Text("Thu", color = Color.Gray, fontSize = 9.sp)
                Text("Fri", color = Color.Gray, fontSize = 9.sp)
                Text("Sat", color = Color.Gray, fontSize = 9.sp)
                Text("Sun", color = Color.Gray, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    disclaimer: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkGray)
            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = GhostGreen, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(text = disclaimer, color = Color.Gray, fontSize = 8.sp)
    }
}

data class DonutSlice(val color: Color, val fraction: Float)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.18f
        var startAngle = -90f

        slices.forEach { slice ->
            val sweepAngle = slice.fraction * 360f
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(text = label, color = Color.LightGray, fontSize = 10.sp)
    }
}

@Composable
fun LineChart(
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxVal = 10f
        val stepX = width / (points.size - 1)
        val strokeWidth = 3.dp.toPx()

        // Draw horizontal grid lines
        for (i in 0..4) {
            val y = height * (i / 4f)
            drawLine(
                color = Color.DarkGray,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw polyline
        val path = Path().apply {
            points.forEachIndexed { index, point ->
                val x = index * stepX
                val y = height - (point / maxVal) * height
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = GhostGreen,
            style = Stroke(width = strokeWidth)
        )

        // Draw points
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = height - (point / maxVal) * height
            drawCircle(
                color = Paper,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
