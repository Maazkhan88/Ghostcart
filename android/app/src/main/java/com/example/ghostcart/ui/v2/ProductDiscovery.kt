package com.example.ghostcart.ui.v2

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.ghostcart.data.CommunityProduct
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.data.iconForProduct
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.ProductPhoto

@Composable
fun ProductDiscoverySection(
    catalogProducts: List<MarketplaceProduct>,
    communityProducts: List<CommunityProduct>,
    communityProductsLoading: Boolean,
    onGhostCatalog: (String) -> Unit,
    onCoolCatalog: (String) -> Unit,
    onGhostCommunity: (String) -> Unit,
    onCoolCommunity: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("all") }
    val categoryProducts = Marketplace.productsForCategory(categoryId, catalogProducts)
    val visibleCatalog = categoryProducts.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
    }
    val visibleCommunity = communityProducts.filter {
        (categoryId == "all" || communityMatchesCategory(it, categoryId)) &&
            (query.isBlank() || it.title.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Products", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("Search the catalogue or share a product from any shopping app.", color = MutedText, fontSize = 12.sp)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(80) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MutedText) },
            placeholder = { Text("Search products") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Marketplace.browseCategories, key = { it.id }) { category ->
                FilterChip(
                    selected = categoryId == category.id,
                    onClick = { categoryId = category.id },
                    label = { Text(category.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Ink,
                        selectedLabelColor = Paper
                    )
                )
            }
        }
        if (visibleCatalog.isEmpty()) {
            Text("No catalogue matches. Paste the product link in Ghost + instead.", color = MutedText, fontSize = 12.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visibleCatalog.take(12), key = { it.id }) { product ->
                    DiscoveryProductCard(
                        title = product.name,
                        category = product.category,
                        priceCents = product.price.toLong() * 100,
                        image = {
                            Box(Modifier.fillMaxSize()) {
                                ProductPhoto(product.name, iconForProduct(product), Modifier.fillMaxSize())
                                if (product.imageUrl != null) {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = "${product.name} product image",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().background(Paper)
                                    )
                                }
                            }
                        },
                        onGhost = { onGhostCatalog(product.id) },
                        onCool = { onCoolCatalog(product.id) }
                    )
                }
            }
        }

        if (communityProductsLoading || visibleCommunity.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("User Ghosted", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("anonymous community finds", color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
            }
            if (communityProductsLoading) {
                Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(18.dp)).background(SoftGray))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(visibleCommunity, key = { it.id }) { product ->
                        DiscoveryProductCard(
                            title = product.title,
                            category = product.sourceDomain,
                            priceCents = product.priceCents,
                            tag = product.activityTag,
                            image = {
                                Box(Modifier.fillMaxSize()) {
                                    ProductPhoto(product.title, "gadget", Modifier.fillMaxSize())
                                    if (product.imageUrl != null) {
                                        AsyncImage(
                                            model = product.imageUrl,
                                            contentDescription = "${product.title} product image",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().background(Paper)
                                        )
                                    }
                                }
                            },
                            onGhost = { onGhostCommunity(product.id) },
                            onCool = { onCoolCommunity(product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryProductCard(
    title: String,
    category: String,
    priceCents: Long,
    tag: String? = null,
    image: @Composable () -> Unit,
    onGhost: () -> Unit,
    onCool: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(188.dp)
            .height(294.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Paper)
            .border(1.dp, FaintBorder, RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(14.dp)).background(SoftGray),
            contentAlignment = Alignment.Center
        ) {
            image()
            if (tag != null) {
                Text(
                    tag,
                    color = Paper,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(999.dp)).background(Ink).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Text(category, color = GhostGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 9.dp))
        Text(title, color = Ink, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(36.dp).padding(top = 2.dp))
        Text(formatProductPrice(priceCents), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 3.dp))
        Box(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp)).background(Ink).clickable(onClick = onGhost),
                contentAlignment = Alignment.Center
            ) { Text("Add to cart", color = Paper, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            Box(
                modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp)).background(GreenTint).clickable(onClick = onCool),
                contentAlignment = Alignment.Center
            ) { Text("Cool it", color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun communityMatchesCategory(product: CommunityProduct, categoryId: String): Boolean {
    val text = "${product.category} ${product.title}".lowercase()
    return when (categoryId) {
        "electronics" -> listOf("electronic", "phone", "laptop", "tablet", "earbud", "watch").any(text::contains)
        "apparel" -> listOf("fashion", "shirt", "shoe", "dress", "bag").any(text::contains)
        "music" -> listOf("music", "guitar", "keyboard", "drum", "microphone").any(text::contains)
        "jewelry" -> listOf("jewel", "ring", "necklace", "bracelet", "watch").any(text::contains)
        "gaming" -> listOf("game", "console", "controller").any(text::contains)
        "beauty" -> listOf("beauty", "perfume", "makeup", "skin").any(text::contains)
        "home" -> listOf("home", "decor", "chair", "lamp", "kitchen").any(text::contains)
        "food" -> listOf("food", "coffee", "meal", "drink", "burger").any(text::contains)
        else -> true
    }
}

private fun formatProductPrice(priceCents: Long): String = if (priceCents > 0) {
    "AED ${"%,.2f".format(java.util.Locale.US, priceCents / 100.0)}"
} else {
    "Add price"
}
