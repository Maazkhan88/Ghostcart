package com.example.ghostcart.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.ghostcart.data.ProductImportState
import com.example.ghostcart.data.ShareQueueItem
import com.example.ghostcart.theme.DarkGray
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.DirhamGlyph
import com.example.ghostcart.ui.common.CoolingDurationDialog
import com.example.ghostcart.ui.common.GhostTopBar
import com.example.ghostcart.ui.common.PrimaryButton
import com.example.ghostcart.ui.common.SecondaryButton
import com.example.ghostcart.ui.common.SimulationBadge

private val categories = listOf("Food & drinks", "Fashion", "Beauty", "Electronics", "Home", "Gaming", "Music", "Other")

@Composable
fun ShareQueueReviewScreen(
    queue: List<ShareQueueItem>,
    importState: ProductImportState,
    onUpdateItem: (ShareQueueItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    onConfirmAll: (shareWithCommunity: Boolean) -> Unit,
    onCoolAll: (shareWithCommunity: Boolean, durationMillis: Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingItem by remember { mutableStateOf<ShareQueueItem?>(null) }
    val hasUnresolvedDuplicates = queue.any { it.duplicateAction == "flagged" }
    var shareWithCommunity by remember { mutableStateOf(false) }
    // The cooling duration is always a user choice - never a silent fixed default, even when
    // bulk-cooling every queued item at once.
    var showBulkCoolingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        GhostTopBar(
            title = "Shared review queue",
            onBack = onBack,
            trailing = {
                SimulationBadge(text = "Staged")
            }
        )

        Text(
            text = "Review and edit shared product links before pushing to the cart.",
            color = MutedText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (queue.isEmpty() && importState !is ProductImportState.Loading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Filled.ShoppingBag,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Queue is empty",
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = "Share items to Ghost Cart to stack them here.",
                        color = MutedText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (importState is ProductImportState.Loading) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SoftGray),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = GhostGreen,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.padding(start = 14.dp)) {
                                        Text(
                                            text = "Analyzing share...",
                                            color = Ink,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Extracting details and checks",
                                            color = MutedText,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(queue, key = { it.id }) { item ->
                        QueueItemCard(
                            item = item,
                            onEdit = { editingItem = item },
                            onRemove = { onRemoveItem(item.id) },
                            onResolveDuplicate = { action ->
                                if (action == "merge" || action == "remove") {
                                    onRemoveItem(item.id)
                                } else {
                                    onUpdateItem(item.copy(duplicateAction = action))
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (queue.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clickable { shareWithCommunity = !shareWithCommunity }
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = shareWithCommunity,
                        onCheckedChange = { shareWithCommunity = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = GhostGreen)
                    )
                    Text(
                        text = "Share anonymously with community feed",
                        color = Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                PrimaryButton(
                    text = if (hasUnresolvedDuplicates) "Resolve duplicates to confirm" else "Add ${queue.size} to Ghost Cart",
                    onClick = {
                        if (!hasUnresolvedDuplicates) {
                            onConfirmAll(shareWithCommunity)
                        }
                    },
                    containerColor = if (hasUnresolvedDuplicates) MutedText else Ink,
                    leadingIcon = Icons.Filled.ShoppingBag
                )

                SecondaryButton(
                    text = if (hasUnresolvedDuplicates) "Resolve duplicates to cool down" else "Cool down ${queue.size} items",
                    onClick = {
                        if (!hasUnresolvedDuplicates) {
                            showBulkCoolingDialog = true
                        }
                    }
                )
            }
        }
    }

    if (showBulkCoolingDialog) {
        CoolingDurationDialog(
            onConfirm = { option ->
                onCoolAll(shareWithCommunity, option.durationMillis)
                showBulkCoolingDialog = false
            },
            onDismiss = { showBulkCoolingDialog = false }
        )
    }

    editingItem?.let { item ->
        EditQueueItemDialog(
            item = item,
            onSave = { updated ->
                onUpdateItem(updated)
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }
}

@Composable
private fun QueueItemCard(
    item: ShareQueueItem,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onResolveDuplicate: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftGray),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        )
                    } else {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = MutedText, modifier = Modifier.size(24.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = item.name,
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        DirhamGlyph(tint = MutedText, modifier = Modifier.size(11.dp))
                        Text(
                            text = "%,.2f".format(java.util.Locale.US, item.amountCents / 100.0),
                            color = MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                        Text(
                            text = " · ${item.category}",
                            color = MutedText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit product details", tint = Ink, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete product", tint = Ink, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (item.duplicateAction == "flagged") {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Duplicate product detected in queue",
                            color = Color(0xFF92400E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Text(
                        text = "Would you like to merge this with the existing item, keep both, or remove this duplicate?",
                        color = Color(0xFFB45309),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        TextButton(
                            onClick = { onResolveDuplicate("merge") },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Color(0xFF92400E))
                        ) {
                            Text("Merge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { onResolveDuplicate("keep_both") },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Color(0xFF92400E))
                        ) {
                            Text("Keep both", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { onResolveDuplicate("remove") },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Color(0xFFB42318))
                        ) {
                            Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditQueueItemDialog(
    item: ShareQueueItem,
    onSave: (ShareQueueItem) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var priceText by remember { mutableStateOf((item.amountCents / 100.0).toString()) }
    var category by remember { mutableStateOf(item.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit product details", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GhostGreen, focusedLabelColor = Ink),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (AED)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GhostGreen, focusedLabelColor = Ink),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Category", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            val selected = cat == category
                            FilterChip(
                                selected = selected,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GhostGreen.copy(alpha = 0.2f), selectedLabelColor = Ink)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = priceText.toDoubleOrNull()?.let { (it * 100).toLong() } ?: item.amountCents
                    onSave(item.copy(name = name, amountCents = cents, category = category))
                }
            ) {
                Text("Save", color = GhostGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Ink)
            }
        }
    )
}
