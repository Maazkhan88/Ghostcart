package com.example.ghostcart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.DemoCatalog
import com.example.ghostcart.data.Product
import com.example.ghostcart.theme.DarkGray
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.Paper

@Composable
fun CartScreen(
    cartIds: Set<String>,
    onRemoveFromCart: (String) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cartProducts = DemoCatalog.products.filter { cartIds.contains(it.id) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Topbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GHOST CART",
                color = Paper,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${cartProducts.size} item${if (cartProducts.size == 1) "" else "s"}",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }

        if (cartProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Portal graphic
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .border(1.dp, GhostGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        GhostMascotPose(
                            poseName = "cart",
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    Text(
                        text = "Your cart is ghost empty.",
                        color = Paper,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Browse items and select \"Ghost it\" to fill your cart.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartProducts) { product ->
                    CartItemRow(
                        product = product,
                        onRemove = { onRemoveFromCart(product.id) }
                    )
                }
            }

            // Summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Real amount charged", color = Color.Gray, fontSize = 12.sp)
                    Text("Zero", color = GhostGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onCheckout,
                    colors = ButtonDefaults.buttonColors(containerColor = GhostGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Complete Fake Checkout",
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "Simulation only · No payment details · No delivery",
                    color = Color.Gray,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun CartItemRow(
    product: Product,
    onRemove: () -> Unit
) {
    val bgHex = android.graphics.Color.parseColor("#" + product.toneColorHex.substring(2))
    val shapeBg = Color(bgHex).copy(alpha = 0.15f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkGray)
            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shapeBg),
            contentAlignment = Alignment.Center
        ) {
            ProductIcon(name = product.vectorIconName, modifier = Modifier.size(24.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(product.name, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Simulated item", color = Color.Gray, fontSize = 9.sp)
        }

        Text(
            text = "×",
            color = Color.Gray,
            fontSize = 20.sp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clickable { onRemove() }
        )
    }
}
