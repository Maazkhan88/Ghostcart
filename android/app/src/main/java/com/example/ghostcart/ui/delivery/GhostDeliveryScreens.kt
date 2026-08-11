package com.example.ghostcart.ui.delivery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.ghostcart.data.AlmostBuy
import com.example.ghostcart.data.GhostDeliveryDuration
import com.example.ghostcart.data.GhostDeliveryState
import com.example.ghostcart.data.GhostOrderResolution
import com.example.ghostcart.data.foodGhostDeliveryDurations
import com.example.ghostcart.data.ghostDeliverySnapshot
import com.example.ghostcart.data.interpolateRoutePosition
import com.example.ghostcart.data.isFoodDeliveryCategory
import com.example.ghostcart.data.marketplaceGhostDeliveryDurations
import com.example.ghostcart.data.simulatedRoute
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.DirhamAmount
import com.example.ghostcart.ui.GhostMascotPose
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun GhostDeliveryTimeDialog(
    productName: String,
    category: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val options = if (isFoodDeliveryCategory(category)) foodGhostDeliveryDurations else marketplaceGhostDeliveryDurations
    var selected by remember(category) { mutableStateOf(options.first()) }
    var customMode by remember { mutableStateOf(false) }
    var customValue by remember { mutableStateOf("24") }
    var customUnit by remember { mutableStateOf("hours") }
    val customMillis = customValue.toLongOrNull()?.let { value ->
        when (customUnit) {
            "minutes" -> TimeUnit.MINUTES.toMillis(value)
            "days" -> TimeUnit.DAYS.toMillis(value)
            else -> TimeUnit.HOURS.toMillis(value)
        }
    }
    val validCustom = customMillis != null && customMillis >= TimeUnit.MINUTES.toMillis(15)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("When should your Ghost Order be delivered?", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(productName, color = Ink, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "Your Ghost Delivery time is your cooling period. Nothing will be purchased or physically delivered.",
                    color = MutedText,
                    fontSize = 12.sp
                )
                options.forEach { option ->
                    FilterChip(
                        selected = !customMode && selected == option,
                        onClick = { selected = option; customMode = false },
                        label = { Text(option.label) },
                        leadingIcon = { Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GhostGreen, selectedLabelColor = Color(0xFF050505)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                FilterChip(
                    selected = customMode,
                    onClick = { customMode = true },
                    label = { Text("Custom") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GhostGreen, selectedLabelColor = Color(0xFF050505)),
                    modifier = Modifier.fillMaxWidth()
                )
                if (customMode) {
                    OutlinedTextField(
                        value = customValue,
                        onValueChange = { customValue = it.filter(Char::isDigit).take(4) },
                        label = { Text("Duration") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        supportingText = { if (!validCustom) Text("Minimum Ghost Delivery time is 15 minutes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("minutes", "hours", "days").forEach { unit ->
                            FilterChip(
                                selected = customUnit == unit,
                                onClick = { customUnit = unit },
                                label = { Text(unit.replaceFirstChar(Char::uppercase)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GhostGreen, selectedLabelColor = Color(0xFF050505))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(if (customMode) customMillis!! else selected.durationMillis) },
                enabled = !customMode || validCustom,
                colors = ButtonDefaults.buttonColors(containerColor = GhostGreen, contentColor = Color(0xFF050505))
            ) { Text("Place Ghost Order", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }
    )
}

@Composable
fun GhostDeliveryTrackerScreen(
    item: AlmostBuy,
    onBack: () -> Unit,
    onResolve: (GhostOrderResolution) -> Unit,
    onRestart: (Long) -> Unit,
    onOpenSource: (String) -> Unit,
    onShare: () -> Unit,
    tutorialMode: Boolean = false
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showRestart by remember { mutableStateOf(false) }
    var confirmSource by remember { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val start = item.deliveryStartedAtMillis ?: item.createdAtMillis
    val end = item.deliveryEndsAtMillis ?: item.coolingUntilMillis
    val snapshot = ghostDeliverySnapshot(now, start, end, item.deliveryState)
    val delivered = snapshot.state == GhostDeliveryState.DELIVERED
    val resolved = snapshot.state.name.startsWith("RESOLVED_")

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Paper),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp, 18.dp, 0.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("Ghost Delivery", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Simulation", color = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onShare, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = "Share product")
                }
            }
        }
        item { ProductSummary(item) }
        item {
            Column {
                Text(stageTitle(snapshot.state), color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text(stageBody(snapshot.state), color = MutedText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                if (!resolved) {
                    LinearProgressIndicator(
                        progress = { snapshot.progress },
                        color = GhostGreen,
                        trackColor = FaintBorder,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp).clip(CircleShape)
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Expected ${timeLabel(end)}", color = MutedText, fontSize = 11.sp)
                        Text(if (delivered) "Delivered" else remainingLabel(snapshot.remainingMillis), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { SimulatedRouteCard(item, snapshot.progress, snapshot.state) }
        item { DeliveryTimeline(snapshot.state) }
        if (delivered) {
            item {
                DeliveredDecisionPanel(
                    elapsed = formatDuration(end - start),
                    hasSource = tutorialMode || !item.sourceUrl.isNullOrBlank(),
                    onSkip = { onResolve(GhostOrderResolution.SKIPPED) },
                    onRestart = { showRestart = true },
                    onBuySource = { confirmSource = true },
                    onBought = { onResolve(GhostOrderResolution.BOUGHT_ALREADY) },
                    onAskFriend = onShare
                )
            }
        }
        item {
            item.sourceUrl?.takeIf { !tutorialMode }?.let { source ->
                OutlinedButton(onClick = { onOpenSource(source) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null)
                    Text("View original source", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(
                "Ghost Delivery · Simulation. No real product or rider is involved.",
                color = MutedText,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
        }
    }

    if (showRestart) {
        GhostDeliveryTimeDialog(
            productName = item.name,
            category = item.category,
            onDismiss = { showRestart = false },
            onConfirm = { duration -> showRestart = false; onRestart(duration) }
        )
    }
    if (confirmSource) {
        AlertDialog(
            onDismissRequest = { confirmSource = false },
            title = { Text("Open original source?") },
            text = { Text("Ghost Cart does not sell this product. You are opening the original source.") },
            confirmButton = {
                Button(onClick = {
                    confirmSource = false
                    onResolve(GhostOrderResolution.BUY_FROM_SOURCE)
                    item.sourceUrl?.takeIf { !tutorialMode }?.let(onOpenSource)
                }) { Text("Buy from source") }
            },
            dismissButton = { TextButton(onClick = { confirmSource = false }) { Text("Stay here") } }
        )
    }
}

@Composable
private fun ProductSummary(item: AlmostBuy) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftGray), shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(model = item.imageUrl, contentDescription = item.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(5.dp))
                } else {
                    GhostMascotPose("cart", Modifier.size(46.dp))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.sourceMerchant ?: item.category, color = MutedText, fontSize = 11.sp)
            }
            DirhamAmount(formatDirhams(item.amountCents), tint = Ink, fontSize = 14.sp, glyphSize = 12.dp)
        }
    }
}

@Composable
private fun SimulatedRouteCard(item: AlmostBuy, progress: Float, state: GhostDeliveryState) {
    val route = remember(item.routeSeed, item.id) { simulatedRoute(item.routeSeed ?: item.id.hashCode().toLong()) }
    val rider = interpolateRoutePosition(route, progress)
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(245.dp).clip(RoundedCornerShape(28.dp)).background(SoftGray)
            .semantics { contentDescription = "Simulated route. ${stageTitle(state)}. ${progress.times(100).toInt()} percent complete." }
    ) {
        Canvas(Modifier.fillMaxSize().padding(22.dp)) {
            val path = Path()
            route.forEachIndexed { index, point ->
                val p = Offset(point.x * size.width, point.y * size.height)
                if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color = FaintBorder, style = Stroke(width = 12f, cap = StrokeCap.Round))
            val travelled = Path()
            val count = (progress * (route.lastIndex)).toInt().coerceIn(0, route.lastIndex)
            route.take(count + 1).forEachIndexed { index, point ->
                val p = Offset(point.x * size.width, point.y * size.height)
                if (index == 0) travelled.moveTo(p.x, p.y) else travelled.lineTo(p.x, p.y)
            }
            if (count > 0) drawPath(travelled, color = GhostGreen, style = Stroke(width = 12f, cap = StrokeCap.Round))
        }
        Box(Modifier.align(Alignment.TopStart).padding(14.dp).clip(CircleShape).background(GreenTint).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("Ghost Delivery · Simulation", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier.offset(x = maxWidth * rider.x - 22.dp, y = maxHeight * rider.y - 4.dp)
                .size(44.dp).clip(CircleShape).background(GhostGreen),
            contentAlignment = Alignment.Center
        ) { GhostMascotPose("cart", Modifier.size(36.dp)) }
        Icon(Icons.Filled.LocationOn, contentDescription = "Simulated destination", tint = Ink, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).size(30.dp))
    }
}

@Composable
private fun DeliveryTimeline(current: GhostDeliveryState) {
    val stages = listOf(
        GhostDeliveryState.PLACED,
        GhostDeliveryState.PREPARING,
        GhostDeliveryState.RIDER_PICKING_UP,
        GhostDeliveryState.OUT_FOR_DELIVERY,
        GhostDeliveryState.RIDER_NEARBY,
        GhostDeliveryState.DELIVERED
    )
    val activeIndex = stages.indexOf(current).let { if (it < 0) stages.lastIndex else it }
    Card(colors = CardDefaults.cardColors(containerColor = SoftGray), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ghost Delivery timeline", color = Ink, fontWeight = FontWeight.ExtraBold)
            stages.forEachIndexed { index, stage ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (index <= activeIndex) Icons.Filled.CheckCircle else Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = if (index <= activeIndex) GhostGreen else MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(stageTitle(stage), color = Ink, fontSize = 12.sp, fontWeight = if (index == activeIndex) FontWeight.ExtraBold else FontWeight.Medium)
                        if (index == activeIndex) Text(stageBody(stage), color = MutedText, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveredDecisionPanel(
    elapsed: String,
    hasSource: Boolean,
    onSkip: () -> Unit,
    onRestart: () -> Unit,
    onBuySource: () -> Unit,
    onBought: () -> Unit,
    onAskFriend: () -> Unit
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(GreenTint).padding(20.dp)) {
        Text("Your decision has arrived", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("You waited $elapsed. What would you like to do?", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp, bottom = 14.dp))
        Button(onClick = onSkip, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)) {
            Text("Skip the purchase", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text("Send it around again", modifier = Modifier.padding(start = 7.dp))
        }
        if (hasSource) TextButton(onClick = onBuySource, modifier = Modifier.fillMaxWidth()) { Text("Buy from source") }
        TextButton(onClick = onBought, modifier = Modifier.fillMaxWidth()) { Text("I bought it already") }
        TextButton(onClick = onAskFriend, modifier = Modifier.fillMaxWidth()) { Text("Ask a friend") }
    }
}

private fun stageTitle(state: GhostDeliveryState): String = when (state) {
    GhostDeliveryState.PLACED -> "Ghost Order placed"
    GhostDeliveryState.PREPARING -> "Being prepared"
    GhostDeliveryState.RIDER_PICKING_UP -> "Ghost Rider picking up"
    GhostDeliveryState.OUT_FOR_DELIVERY -> "Out for Ghost Delivery"
    GhostDeliveryState.RIDER_NEARBY -> "Ghost Rider nearby"
    GhostDeliveryState.DELIVERED -> "Delivered"
    GhostDeliveryState.RESOLVED_SKIPPED -> "Purchase skipped"
    GhostDeliveryState.RESOLVED_BUY_FROM_SOURCE -> "Opened original source"
    GhostDeliveryState.RESOLVED_BOUGHT_ALREADY -> "Bought already"
    GhostDeliveryState.RESTARTED -> "Sent around again"
    GhostDeliveryState.CANCELLED -> "Cancelled"
}

private fun stageBody(state: GhostDeliveryState): String = when (state) {
    GhostDeliveryState.PLACED -> "Your decision is cooling."
    GhostDeliveryState.PREPARING -> "Giving the impulse some space."
    GhostDeliveryState.RIDER_PICKING_UP -> "Your almost-buy is being collected."
    GhostDeliveryState.OUT_FOR_DELIVERY -> "No action is needed yet."
    GhostDeliveryState.RIDER_NEARBY -> "Your decision is almost ready."
    GhostDeliveryState.DELIVERED -> "Time to choose what happens next."
    GhostDeliveryState.RESOLVED_SKIPPED -> "This amount is now confirmed as Money Kept."
    GhostDeliveryState.RESOLVED_BUY_FROM_SOURCE -> "This was not counted as Money Kept."
    GhostDeliveryState.RESOLVED_BOUGHT_ALREADY -> "This was not counted as Money Kept."
    GhostDeliveryState.RESTARTED -> "A fresh Ghost Delivery has started."
    GhostDeliveryState.CANCELLED -> "No decision was recorded."
}

private fun remainingLabel(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis).coerceAtLeast(0)
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "${days}d ${hours % 24}h remaining"
        hours > 0 -> "${hours}h ${minutes % 60}m remaining"
        minutes > 0 -> "${minutes}m remaining"
        else -> "Less than a minute"
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis.coerceAtLeast(0))
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "${days} day${if (days == 1L) "" else "s"}"
        hours > 0 -> "${hours} hour${if (hours == 1L) "" else "s"}"
        else -> "${minutes.coerceAtLeast(1)} minutes"
    }
}

private fun timeLabel(timestamp: Long): String =
    SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(timestamp))

private fun formatDirhams(amountCents: Long): String =
    NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
        .format(amountCents / 100.0)
