package com.example.ghostcart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.DemoCatalog
import com.example.ghostcart.data.Product
import com.example.ghostcart.theme.DarkGray
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import kotlinx.coroutines.delay

@Composable
fun CatalogScreen(
    cartIds: Set<String>,
    cooledIds: Set<String>,
    ghostedIds: Set<String>,
    onAddToCart: (String) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onCoolDown: (String) -> Unit,
    onResetState: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .padding(16.dp)
    ) {
        Text(
            text = "Browse Temptation",
            color = GhostGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Choose an almost-buy.",
            color = Paper,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(DemoCatalog.products) { product ->
                val inCart = cartIds.contains(product.id)
                val cooled = cooledIds.contains(product.id)
                val ghosted = ghostedIds.contains(product.id)

                ProductCard(
                    product = product,
                    inCart = inCart,
                    cooled = cooled,
                    ghosted = ghosted,
                    onAddToCart = { onAddToCart(product.id) },
                    onRemoveFromCart = { onRemoveFromCart(product.id) },
                    onCoolDown = { onCoolDown(product.id) },
                    onResetState = { onResetState(product.id) }
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    inCart: Boolean,
    cooled: Boolean,
    ghosted: Boolean,
    onAddToCart: () -> Unit,
    onRemoveFromCart: () -> Unit,
    onCoolDown: () -> Unit,
    onResetState: () -> Unit
) {
    val bgHex = android.graphics.Color.parseColor("#" + product.toneColorHex.substring(2))
    val cardBg = Color(bgHex).copy(alpha = 0.08f)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkGray),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Product Art Wrapper
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg),
                contentAlignment = Alignment.Center
            ) {
                ProductIcon(
                    name = product.vectorIconName,
                    modifier = Modifier.size(56.dp),
                    color = Color.White
                )
            }

            Text(
                text = product.category.uppercase(),
                color = Color.Gray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = product.name,
                color = Paper,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = product.note,
                color = Color.LightGray,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                maxLines = 2
            )

            // Status Indicator with reset button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                val statusText = when {
                    cooled -> "Cooling active"
                    ghosted -> "Ghosted"
                    inCart -> "In Ghost Cart"
                    else -> "Ready to ghost"
                }
                Text(
                    text = statusText,
                    color = if (cooled || ghosted) GhostGreen else Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                if (cooled || ghosted) {
                    Text(
                        text = "RESET",
                        color = GhostGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onResetState() }
                    )
                }
            }

            // Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (inCart) {
                    Button(
                        onClick = onRemoveFromCart,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text("Undo", color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onAddToCart,
                        colors = ButtonDefaults.buttonColors(containerColor = GhostGreen),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text("Ghost it", color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HoldToCoolButton(
                    onCooled = onCoolDown,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                )
            }
        }
    }
}

@Composable
fun HoldToCoolButton(
    onCooled: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            while (progress < 1f && isHolding) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed / 900f).coerceIn(0f, 1f)
                delay(16)
            }
            if (progress >= 1f) {
                onCooled()
            }
            isHolding = false
            progress = 0f
        } else {
            progress = 0f
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Color.Transparent)
            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(9.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown()
                        isHolding = true
                        waitForUpOrCancellation()
                        isHolding = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(GhostGreen.copy(alpha = 0.28f))
                    .align(Alignment.CenterStart)
            )
        }
        Text(
            text = if (isHolding) "Cooling..." else "Hold to cool",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
