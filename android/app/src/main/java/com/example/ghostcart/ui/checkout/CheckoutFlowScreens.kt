package com.example.ghostcart.ui.checkout

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.data.WalletDemoData
import com.example.ghostcart.data.WalletConfig
import com.example.ghostcart.data.iconForProduct
import com.example.ghostcart.theme.DangerRed
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.ProductPhoto
import com.example.ghostcart.ui.common.BackButton
import com.example.ghostcart.ui.common.ForwardChevron
import com.example.ghostcart.ui.common.GhostHeroCard
import com.example.ghostcart.ui.common.GhostTopBar
import com.example.ghostcart.ui.common.PrimaryButton
import com.example.ghostcart.ui.common.RoundIconButton
import com.example.ghostcart.ui.common.SecondaryButton
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SERVICE_FEE_RATE = 0.05f
private const val VAT_RATE = 0.05f
private const val PROMO_RATE = 0.10f

@Composable
fun GhostCartListScreen(
    products: List<Pair<MarketplaceProduct, Int>>,
    coolingUntilByProductId: Map<String, Long>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    onStartCooling: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    onShareProduct: (MarketplaceProduct) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtotal = products.sumOf { (product, qty) -> product.price * qty }

    Box(modifier = modifier.fillMaxSize().background(Paper)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 16.dp,
                end = 20.dp,
                bottom = 112.dp
            )
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    BackButton(onBack = onBack)
                    Text(text = "Ghost Cart", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f).padding(start = 10.dp))
                    RoundIconButton(icon = Icons.Filled.Delete, onClick = onClearAll)
                }

                GhostMascotPose(poseName = "wave", modifier = Modifier.size(56.dp).padding(top = 16.dp))
                Text(
                    text = "The craving disappeared.\nThe money stayed.",
                    color = Ink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(text = "Nothing here is charged or delivered.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(14.dp))
                    Text(text = "You almost spent ${Marketplace.currency} $subtotal", color = GhostGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                }
            }

            if (products.isEmpty()) {
                item {
                    Text(
                        text = "Your Ghost Cart is empty. Add something you almost bought.",
                        color = MutedText,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(products, key = { it.first.id }) { (product, qty) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftGray)
                                .clickable { onOpenProduct(product.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            ProductPhoto(productName = product.name, fallbackIconName = iconForProduct(product), modifier = Modifier.fillMaxSize())
                            if (product.imageUrl != null) AsyncImage(
                                model = product.imageUrl,
                                contentDescription = "${product.name} product image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().background(Paper)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenProduct(product.id) }
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(text = product.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${Marketplace.currency} ${product.price}", color = MutedText, fontSize = 11.sp)
                            val coolingUntil = coolingUntilByProductId[product.id]
                            Text(
                                text = when {
                                    coolingUntil == null -> "Start 24-hour cooling"
                                    coolingUntil <= System.currentTimeMillis() -> "Cooled off — review now"
                                    else -> "Cooling active"
                                },
                                color = GhostGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp).clickable { onStartCooling(product.id) }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { onShareProduct(product) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = "Share ${product.name}",
                                    tint = Ink,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(SoftGray)
                                    .clickable { onRemove(product.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "-", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "$qty", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(SoftGray)
                                    .clickable { onAdd(product.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "+", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    SummaryLine("Subtotal", "${Marketplace.currency} $subtotal")
                    SummaryLine("Small Win Fee", "${Marketplace.currency} 0")
                    SummaryLine("Real amount charged", "${Marketplace.currency} 0", valueColor = GhostGreen)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(FaintBorder))
                    SummaryLine("Total you almost spent:", "${Marketplace.currency} $subtotal", bold = true)
                }
                SecondaryButton(text = "Clear Ghost Cart", onClick = onClearAll, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Paper)
                .border(width = 1.dp, color = FaintBorder)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            PrimaryButton(text = "Proceed to Ghost Checkout", onClick = onCheckout)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, valueColor: Color = Ink, bold: Boolean = false, showDirhamIcon: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = if (bold) Ink else MutedText, fontSize = if (bold) 14.sp else 12.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal, modifier = Modifier.weight(1f))
        if (showDirhamIcon) {
            com.example.ghostcart.ui.DirhamGlyph(tint = valueColor, modifier = Modifier.size(if (bold) 14.dp else 11.dp))
            Text(text = value.removePrefix("${Marketplace.currency} "), color = valueColor, fontSize = if (bold) 16.sp else 12.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        } else {
            Text(text = value, color = valueColor, fontSize = if (bold) 16.sp else 12.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold)
        }
    }
}

@Composable
fun GhostCheckoutScreen(
    products: List<Pair<MarketplaceProduct, Int>>,
    walletBalance: Int,
    simulationIntervalMinutes: Int,
    onSelectInterval: (Int) -> Unit,
    onBack: () -> Unit,
    onPlaceOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtotal = products.sumOf { (product, qty) -> product.price * qty }
    val promoDiscount = (subtotal * PROMO_RATE).toInt()
    val serviceFee = ((subtotal - promoDiscount) * SERVICE_FEE_RATE).toInt()
    val vat = ((subtotal - promoDiscount) * VAT_RATE).toInt()
    val total = subtotal - promoDiscount + serviceFee + vat

    var deliveryAddress by remember { mutableStateOf("123 Ghost Street\nAl Wasl, Dubai\nUnited Arab Emirates") }
    var editingAddress by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GhostTopBar(title = "Ghost Checkout", onBack = onBack)
        Text(text = "Simulation mode", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Ink, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(text = "This is not a real order.", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = "No payment will be charged.\nNo product will be delivered.", color = Ink, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        CheckoutInfoRow(icon = Icons.Filled.LocationOn, title = "Fake Delivery Address", subtitle = deliveryAddress, action = "Change", onAction = { editingAddress = true })
        CheckoutInfoRow(icon = Icons.Filled.Notifications, title = "Ghost Wallet", subtitle = "Balance: ${Marketplace.currency} $walletBalance.00", action = "Change")
        CheckoutInfoRow(icon = Icons.Filled.CheckCircle, title = "NoPay Balance", subtitle = "${Marketplace.currency} 800.00 available", action = "Available")
        CheckoutInfoRow(icon = Icons.Filled.Sell, title = "Promo Code", subtitle = "GHOST10 · 10% off applied", action = "Remove")

        Text(text = "Simulation Speed (per step)", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SoftGray)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(1, 2, 5, 10).forEach { mins ->
                val selected = simulationIntervalMinutes == mins
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Ink else Color.Transparent)
                        .clickable { onSelectInterval(mins) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$mins min",
                        color = if (selected) Paper else Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(text = "Order Summary", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            products.forEach { (product, qty) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(SoftGray), contentAlignment = Alignment.Center) {
                        ProductPhoto(productName = product.name, fallbackIconName = iconForProduct(product), modifier = Modifier.fillMaxSize())
                        if (product.imageUrl != null) AsyncImage(
                            model = product.imageUrl,
                            contentDescription = "${product.name} product image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().background(Paper)
                        )
                    }
                    Text(text = "${product.name} (x$qty)", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                    Text(text = "${Marketplace.currency} ${product.price * qty}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(FaintBorder))
            SummaryLine("Subtotal", "${Marketplace.currency} $subtotal")
            SummaryLine("Promo Discount (GHOST10)", "-${Marketplace.currency} $promoDiscount", valueColor = GhostGreen)
            SummaryLine("Service Fee", "${Marketplace.currency} $serviceFee")
            SummaryLine("VAT (5%)", "${Marketplace.currency} $vat")
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(FaintBorder))
            SummaryLine("Total", "${Marketplace.currency} $total", bold = true, showDirhamIcon = true)
        }

        PrimaryButton(text = "Place Fake Order", onClick = onPlaceOrder, trailingIcon = Icons.Filled.ArrowForward, modifier = Modifier.padding(top = 20.dp))
    }

    if (editingAddress) {
        var draft by remember(deliveryAddress) { mutableStateOf(deliveryAddress) }
        AlertDialog(
            onDismissRequest = { editingAddress = false },
            title = { Text("Fake delivery address") },
            text = {
                OutlinedTextField(value = draft, onValueChange = { draft = it.take(120) }, minLines = 3)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draft.isNotBlank()) deliveryAddress = draft.trim()
                    editingAddress = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingAddress = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CheckoutInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    action: String,
    onAction: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, FaintBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(SoftGray), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(text = title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            text = action,
            color = GhostGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier
        )
    }
}

@Composable
fun OrderGhostedSuccessScreen(
    orderId: String,
    amountAvoided: Int,
    sourceProducts: List<MarketplaceProduct>,
    onTrackDelivery: () -> Unit,
    onViewSavings: () -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val redeemableProducts = sourceProducts.filter { !it.sourceUrl.isNullOrBlank() }.distinctBy { it.sourceUrl }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GhostMascotPose(poseName = "wave", modifier = Modifier.size(32.dp))
            Text(text = "Ghost Cart", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 8.dp))
        }

        Box(modifier = Modifier.padding(top = 32.dp), contentAlignment = Alignment.BottomEnd) {
            GhostMascotPose(poseName = "thumbsup", modifier = Modifier.size(140.dp))
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(GhostGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
            }
        }

        Text(text = "Order Ghosted\nSuccessfully", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Text(text = "Simulated checkout total: ", color = MutedText, fontSize = 13.sp)
            Text(text = "${Marketplace.currency} $amountAvoided.", color = GhostGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SoftGray)
                .padding(14.dp)
        ) {
            Text(text = "Ghost Order #", color = MutedText, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(text = orderId, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile(label = "Real amount charged:", value = "${Marketplace.currency} 0", modifier = Modifier.weight(1f))
            InfoTile(label = "Not counted as Money Kept until you later choose to skip it", value = "", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (redeemableProducts.isNotEmpty()) {
            Text(
                text = "Product link unlocked",
                color = GhostGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            redeemableProducts.forEach { product ->
                SecondaryButton(
                    text = if (redeemableProducts.size == 1) {
                        "Ghosted — view original product"
                    } else {
                        "View ${product.name.take(34)}"
                    },
                    onClick = { product.sourceUrl?.let(onOpenSource) },
                    leadingIcon = Icons.Filled.OpenInNew,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = "Opens the original retailer page. Ghost Cart is not affiliated with the retailer.",
                color = MutedText,
                fontSize = 9.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }

        PrimaryButton(text = "Track Fake Delivery", onClick = onTrackDelivery, trailingIcon = Icons.Filled.ArrowForward)
        SecondaryButton(text = "View Progress", onClick = onViewSavings, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp))
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, FaintBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(16.dp))
        Text(text = label, color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
        if (value.isNotBlank()) {
            Text(text = value, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun FakeDeliveryTrackingScreen(
    orderId: String,
    amountSaved: Int,
    deliveryStep: Int,
    orderPlacedAtMillis: Long,
    simulationIntervalMinutes: Int,
    feedbackSubmitted: Boolean = false,
    onSubmitFeedback: (Int, String) -> Unit = { _, _ -> },
    onViewReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dismissedFeedback by remember(orderId) { mutableStateOf(false) }
    val showFeedbackPrompt = deliveryStep >= 4 && !feedbackSubmitted && !dismissedFeedback

    if (showFeedbackPrompt) {
        GhostFeedbackDialog(
            onSubmit = { rating, comment -> onSubmitFeedback(rating, comment) },
            onDismiss = { dismissedFeedback = true }
        )
    }

    val deviceTime = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val baseTime = orderPlacedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
    val intervalMillis = simulationIntervalMinutes.coerceAtLeast(1) * 60_000L
    val steps = listOf(
        "Order placed" to "We've received your imaginary order.",
        "Preparing imaginary order" to "Our team is carefully doing nothing.",
        "Ghost Rider is on the way" to "Zooming through the void.",
        "Rider left absolutely nothing at your doorstep" to "Yep, nothing's there.",
        "Fake delivery complete" to "Thanks for choosing smart savings."
    ).mapIndexed { index, (title, caption) ->
        Triple(title, caption, deviceTime.format(Date(baseTime + index * intervalMillis)))
    }

    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "‹", color = Ink, fontSize = 22.sp, modifier = Modifier.width(38.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(26.dp))
                Text(text = "Ghost Cart", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 8.dp))
            }
            Text(text = "Help", color = MutedText, fontSize = 13.sp, modifier = Modifier.width(38.dp))
        }

        Text(text = "Fake Delivery Tracking", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp))
        Text(text = "Order $orderId", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GreenTint)
                .padding(14.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(text = "No real charge was made", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "${Marketplace.currency} $amountSaved remains an almost-spent amount until you decide.", color = MutedText, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Simulated", color = MutedText, fontSize = 9.sp)
                Text(text = "${Marketplace.currency} $amountSaved", color = GhostGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Text(
            text = "✦ Please prepare to receive absolutely nothing. ✦",
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
        )

        Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
            steps.forEachIndexed { index, (title, caption, time) ->
                val done = index <= deliveryStep
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (done) GhostGreen else FaintBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            if (done && index < steps.lastIndex) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Paper, modifier = Modifier.size(13.dp))
                            }
                        }
                        if (index != steps.lastIndex) {
                            Box(modifier = Modifier.width(2.dp).height(38.dp).background(if (done) GhostGreen else FaintBorder))
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp, bottom = 18.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(text = time, color = MutedText, fontSize = 10.sp)
                        }
                        Text(text = caption, color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        SimulatedGhostRiderRoute(deliveryStep = deliveryStep)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SoftGray), contentAlignment = Alignment.Center) {
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(text = "Your Ghost Rider", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "Delivering absolutely nothing, on time.", color = MutedText, fontSize = 10.sp)
            }
        }

        PrimaryButton(text = "View Ghost Receipt", onClick = onViewReceipt, modifier = Modifier.padding(top = 18.dp))
    }
}

@Composable
private fun GhostFeedbackDialog(onSubmit: (Int, String) -> Unit, onDismiss: () -> Unit) {
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "How was this ghosting?", color = Ink, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Text(text = "Quick feedback helps us improve Ghost Cart.", color = MutedText, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.Center) {
                    for (star in 1..5) {
                        Icon(
                            imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Rate $star stars",
                            tint = if (star <= rating) GhostGreen else MutedText,
                            modifier = Modifier
                                .size(34.dp)
                                .padding(horizontal = 3.dp)
                                .clickable { rating = star }
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Anything you'd like to add? (optional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(rating, comment) }, enabled = rating > 0) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}

@Composable
private fun SimulatedGhostRiderRoute(
    deliveryStep: Int,
    modifier: Modifier = Modifier
) {
    val safeStep = deliveryStep.coerceIn(0, 4)
    val targetProgress = when (safeStep) {
        0 -> 0.08f
        1 -> 0.24f
        2 -> 0.58f
        3 -> 0.88f
        else -> 0.96f
    }
    val routeProgress = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1_200),
        label = "ghost rider route progress"
    ).value
    val isDriving = safeStep < 4
    val infiniteTransition = rememberInfiniteTransition(label = "ghost rider driving")
    val roadMotion = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Restart
        ),
        label = "moving road markings"
    ).value
    val riderBounce = infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rider bounce"
    ).value
    val status = when (safeStep) {
        0 -> "Leaving the imaginary kitchen"
        1 -> "Loading absolutely nothing"
        2 -> "Ghost Rider is cruising"
        3 -> "Arriving at your imaginary doorstep"
        else -> "Fake delivery complete"
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SoftGray)
            .semantics {
                contentDescription = "Simulated live route. $status. This is not real GPS tracking."
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(GreenTint)
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(text = "● Live simulation", color = GhostGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "NOT REAL GPS", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = 44.dp, bottom = 34.dp)
        ) {
            val startX = 20f
            val endX = size.width - 20f
            val roadY = size.height * 0.62f
            val totalDistance = endX - startX

            drawLine(
                color = FaintBorder,
                start = Offset(startX, roadY),
                end = Offset(endX, roadY),
                strokeWidth = 16f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = GhostGreen.copy(alpha = 0.38f),
                start = Offset(startX, roadY),
                end = Offset(startX + totalDistance * routeProgress, roadY),
                strokeWidth = 16f,
                cap = StrokeCap.Round
            )

            val dashOffset = if (isDriving) roadMotion * 38f else 0f
            var dashX = startX - 38f + dashOffset
            while (dashX < endX) {
                drawLine(
                    color = Paper.copy(alpha = 0.92f),
                    start = Offset(dashX.coerceAtLeast(startX), roadY),
                    end = Offset((dashX + 18f).coerceAtMost(endX), roadY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                dashX += 38f
            }

            drawCircle(color = Ink, radius = 7f, center = Offset(startX, roadY))
            drawCircle(color = GhostGreen, radius = 9f, center = Offset(endX, roadY))
            drawCircle(color = Paper, radius = 4f, center = Offset(endX, roadY))
        }

        val riderWidth = 64.dp
        val travelWidth = (maxWidth - riderWidth - 24.dp).coerceAtLeast(0.dp)
        val drivingNudge = if (isDriving) (roadMotion * 5f).dp else 0.dp
        Box(
            modifier = Modifier
                .offset(
                    x = 12.dp + (travelWidth * routeProgress) + drivingNudge,
                    y = (55f + if (isDriving) riderBounce else 0f).dp
                )
                .size(width = riderWidth, height = 52.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val wheelY = size.height * 0.82f
                drawCircle(color = Ink, radius = size.width * 0.10f, center = Offset(size.width * 0.27f, wheelY))
                drawCircle(color = Ink, radius = size.width * 0.10f, center = Offset(size.width * 0.76f, wheelY))
                drawCircle(color = Paper, radius = size.width * 0.04f, center = Offset(size.width * 0.27f, wheelY))
                drawCircle(color = Paper, radius = size.width * 0.04f, center = Offset(size.width * 0.76f, wheelY))
                drawRoundRect(
                    color = GhostGreen,
                    topLeft = Offset(size.width * 0.26f, size.height * 0.49f),
                    size = Size(size.width * 0.43f, size.height * 0.23f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.08f)
                )
                drawLine(
                    color = Ink,
                    start = Offset(size.width * 0.65f, size.height * 0.52f),
                    end = Offset(size.width * 0.78f, size.height * 0.31f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Ink,
                    start = Offset(size.width * 0.76f, size.height * 0.31f),
                    end = Offset(size.width * 0.86f, size.height * 0.31f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
            GhostMascotPose(
                poseName = "wave",
                modifier = Modifier.size(31.dp).offset(x = 7.dp, y = 1.dp)
            )
        }

        Text(
            text = "Ghost kitchen",
            color = MutedText,
            fontSize = 8.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp)
        )
        Text(
            text = "Imaginary doorstep",
            color = MutedText,
            fontSize = 8.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 8.dp)
        )
        Text(
            text = status,
            color = Ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 45.dp)
        )
    }
}

@Composable
fun PayWithGhostCardScreen(
    product: MarketplaceProduct,
    config: WalletConfig,
    walletBalance: Int,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GhostTopBar(title = "Ghost Cart", onBack = onBack, trailing = { RoundIconButton(icon = Icons.Filled.Notifications) {} })
        Text(text = "Pay with Ghost Card", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp))
        Text(text = "Ghost Card is a demo. Nothing will be charged.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        GhostHeroCard(modifier = Modifier.padding(top = 18.dp)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(70.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = config.cardName, color = Paper, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    Text(text = config.cardholderName, color = Paper, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
                    Text(text = "Ghost ID  ${config.ghostId}", color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp)
                    Text(text = "Balance", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp))
                    Text(text = "${Marketplace.currency} $walletBalance", color = Paper, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Text(text = "Order summary", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 20.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(SoftGray), contentAlignment = Alignment.Center) {
                ProductPhoto(productName = product.name, fallbackIconName = iconForProduct(product), modifier = Modifier.fillMaxSize())
                if (product.imageUrl != null) AsyncImage(
                    model = product.imageUrl,
                    contentDescription = "${product.name} product image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().background(Paper)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(text = product.name, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "Blind Buy • ${product.category}", color = MutedText, fontSize = 10.sp)
            }
            Text(text = "${Marketplace.currency} ${product.price}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(FaintBorder))
        SummaryLine("Item amount", "${Marketplace.currency} ${product.price}")
        SummaryLine("Service (Ghost Protection)", "${Marketplace.currency} 0")
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(FaintBorder))
        SummaryLine("Total protected", "${Marketplace.currency} ${product.price}", valueColor = GhostGreen, bold = true)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GreenTint)
                .padding(12.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(14.dp))
            Text(text = "Real amount charged: ${Marketplace.currency} 0", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }

        PrimaryButton(text = "Confirm Ghost Pay", onClick = onConfirm, leadingIcon = null, trailingIcon = Icons.Filled.ArrowForward, modifier = Modifier.padding(top = 16.dp))
        SecondaryButton(text = "Cancel", onClick = onCancel, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
fun OrderProtectedScreen(
    orderId: String,
    amountSaved: Int,
    balanceBefore: Int,
    onBack: () -> Unit,
    onViewReceipt: () -> Unit,
    onBackToWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val newBalance = balanceBefore + amountSaved

    Column(modifier = modifier.fillMaxSize().background(Paper).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GhostTopBar(title = "Ghost Cart", onBack = onBack, trailing = { RoundIconButton(icon = Icons.Filled.Notifications) {} })

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Box(contentAlignment = Alignment.BottomEnd) {
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(96.dp))
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(GhostGreen), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
                }
            }
            Text(text = "Order Protected", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp))
            Text(text = "Your Ghost Card protected", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Text(text = "${Marketplace.currency} $amountSaved", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            SummaryLine("Before", "${Marketplace.currency} $balanceBefore")
            SummaryLine("+ Saved", "${Marketplace.currency} $amountSaved", valueColor = GhostGreen)
            SummaryLine("New balance", "${Marketplace.currency} $newBalance", valueColor = GhostGreen)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SoftGray)
                .padding(14.dp)
        ) {
            Text(text = "Ghost Order", color = MutedText, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(text = orderId, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 8.dp))
            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = MutedText, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(text = "View Ghost Receipt", onClick = onViewReceipt)
        SecondaryButton(text = "Back to Wallet", onClick = onBackToWallet, modifier = Modifier.padding(top = 10.dp))
    }
}
