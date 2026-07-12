package com.example.ghostcart.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.theme.DarkGray
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.Paper

@Composable
fun CheckoutScreen(
    deliveryStep: Int,
    onViewReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timelineSteps = listOf(
        "Placed order",
        "Order accepted",
        "Order getting prepared",
        "Ghost rider picking up the order",
        "Ghost rider on the way to deliver",
        "Ghost rider has delivered your ghost order"
    )

    val currentMascotPose = when (deliveryStep) {
        0 -> "cart"
        1 -> "wave"
        2 -> "combo"
        3 -> "cooldown" // phoneList map
        4 -> "cooldown" // checkoutPhone map
        else -> "thumbsup"
    }

    val progressFraction by animateFloatAsState(
        targetValue = (deliveryStep.coerceIn(0, 5) / 5f),
        animationSpec = tween(durationMillis = 400),
        label = "progressBar"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Simulated Ghost Delivery".uppercase(),
                color = GhostGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )

            Text(
                text = if (deliveryStep == 5) "Ghost order\nhas arrived!" else "Following your\nghost order...",
                color = Paper,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Mascot circle
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkGray)
                    .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                GhostMascotPose(
                    poseName = currentMascotPose,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Progress bar track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2C2C2C))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .background(GhostGreen)
                )
            }

            // Steps list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                timelineSteps.forEachIndexed { index, stepText ->
                    val isCompleted = deliveryStep > index
                    val isActive = deliveryStep == index
                    val stepColor = when {
                        isActive -> Paper
                        isCompleted -> GhostGreen
                        else -> Color.Gray
                    }
                    val bulletText = when {
                        isCompleted -> "✓"
                        isActive -> "●"
                        else -> "○"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bulletText,
                            color = stepColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stepText,
                            color = stepColor,
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Bottom view button & disclaimer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (deliveryStep == 5) {
                Button(
                    onClick = onViewReceipt,
                    colors = ButtonDefaults.buttonColors(containerColor = GhostGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "View Ghost Receipt",
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Text(
                text = "Simulation only · No real courier is dispatched.",
                color = Color.Gray,
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp)
            )
        }
    }
}
