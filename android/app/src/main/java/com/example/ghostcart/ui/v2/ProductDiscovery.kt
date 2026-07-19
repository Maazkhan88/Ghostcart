package com.example.ghostcart.ui.v2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay

@Composable
fun ProductDiscoverySection(
    catalogProducts: List<MarketplaceProduct>,
    favoriteProducts: List<MarketplaceProduct>,
    favoriteProductIds: Set<String>,
    communityProducts: List<CommunityProduct>,
    communityProductsLoading: Boolean,
    onGhostCatalog: (String) -> Unit,
    onCoolCatalog: (String) -> Unit,
    onOpenCatalog: (String) -> Unit,
    onGhostCommunity: (String) -> Unit,
    onCoolCommunity: (String) -> Unit,
    onOpenCommunity: (String) -> Unit,
    onToggleFavoriteCatalog: (String) -> Unit,
    onToggleFavoriteCommunity: (String) -> Unit,
    onNotifications: () -> Unit,
    onViewAllCatalog: (String) -> Unit,
    onViewAllCommunity: () -> Unit,
    onViewAllFavorites: () -> Unit
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
    val visibleFavorites = favoriteProducts.filter {
        (categoryId == "all" || Marketplace.productsForCategory(categoryId, listOf(it)).isNotEmpty()) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            com.example.ghostcart.ui.GhostCartWordmark(
                modifier = Modifier.align(Alignment.Center).width(132.dp).height(32.dp),
                tint = Ink
            )
            IconButton(onClick = onNotifications, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Ink)
            }
        }
        PromoBannerCarousel()
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
        DiscoverySectionHeader(
            title = "Marketplace products",
            subtitle = "browse every temptation",
            onViewAll = { onViewAllCatalog(categoryId) }
        )
        if (visibleCatalog.isEmpty()) {
            Text("No catalogue matches. Paste the product link in Ghost + instead.", color = MutedText, fontSize = 12.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visibleCatalog.take(12), key = { it.id }) { product ->
                    DiscoveryProductCard(
                        title = product.name,
                        category = product.category,
                        priceCents = product.price.toLong() * 100,
                        isFavorite = product.id in favoriteProductIds,
                        image = {
                            Box(Modifier.fillMaxSize().background(Color.White)) {
                                ProductPhoto(product.name, iconForProduct(product), Modifier.fillMaxSize())
                                if (product.imageUrl != null) {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = "${product.name} product image",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().background(Color.White)
                                    )
                                }
                            }
                        },
                        onOpen = { onOpenCatalog(product.id) },
                        onToggleFavorite = { onToggleFavoriteCatalog(product.id) },
                        onGhost = { onGhostCatalog(product.id) },
                        onCool = { onCoolCatalog(product.id) }
                    )
                }
            }
        }

        run {
            DiscoverySectionHeader(
                title = "Community products",
                subtitle = "anonymous user-ghosted finds",
                onViewAll = onViewAllCommunity
            )
            if (communityProductsLoading) {
                Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(18.dp)).background(SoftGray))
            } else if (visibleCommunity.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(visibleCommunity, key = { it.id }) { product ->
                        DiscoveryProductCard(
                            title = product.title,
                            category = product.sourceDomain,
                            priceCents = product.priceCents,
                            isFavorite = "community_${product.id}" in favoriteProductIds,
                            image = {
                                Box(Modifier.fillMaxSize().background(Color.White)) {
                                    ProductPhoto(product.title, "gadget", Modifier.fillMaxSize())
                                    if (product.imageUrl != null) {
                                        AsyncImage(
                                            model = product.imageUrl,
                                            contentDescription = "${product.title} product image",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().background(Color.White)
                                        )
                                    }
                                }
                            },
                            onOpen = { onOpenCommunity(product.id) },
                            onToggleFavorite = { onToggleFavoriteCommunity(product.id) },
                            onGhost = { onGhostCommunity(product.id) },
                            onCool = { onCoolCommunity(product.id) }
                        )
                    }
                }
            } else {
                Text(
                    text = "Community products will appear after people ghost shared finds.",
                    color = MutedText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        DiscoverySectionHeader(
            title = "Your favorites",
            subtitle = "saved for a calmer decision",
            onViewAll = onViewAllFavorites
        )
        if (visibleFavorites.isEmpty()) {
            Text(
                text = "Favorite an item to keep it close without buying it.",
                color = MutedText,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visibleFavorites, key = { "favorite_${it.id}" }) { product ->
                    DiscoveryProductCard(
                        title = product.name,
                        category = product.category,
                        priceCents = product.price.toLong() * 100,
                        isFavorite = true,
                        image = {
                            Box(Modifier.fillMaxSize().background(Color.White)) {
                                ProductPhoto(product.name, iconForProduct(product), Modifier.fillMaxSize())
                                if (product.imageUrl != null) {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = "${product.name} product image",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().background(Color.White)
                                    )
                                }
                            }
                        },
                        onOpen = { onOpenCatalog(product.id) },
                        onToggleFavorite = { onToggleFavoriteCatalog(product.id) },
                        onGhost = { onGhostCatalog(product.id) },
                        onCool = { onCoolCatalog(product.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverySectionHeader(
    title: String,
    subtitle: String,
    onViewAll: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = MutedText, fontSize = 10.sp)
        }
        Text(
            text = "View all",
            color = GhostGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onViewAll)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

private val PROMO_BANNER_MESSAGES = listOf(
    "Ghost it before you regret it. 👻",
    "New arrivals just dropped in your favorite categories.",
    "Cool it now, decide later — nothing is charged.",
    "Share a product link from any app to import it instantly."
)

@Composable
private fun PromoBannerCarousel(modifier: Modifier = Modifier) {
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            index = (index + 1) % PROMO_BANNER_MESSAGES.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Ink)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedContent(
            targetState = index,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "promoBanner"
        ) { messageIndex ->
            Text(
                text = PROMO_BANNER_MESSAGES[messageIndex],
                color = Paper,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DiscoveryProductCard(
    title: String,
    category: String,
    priceCents: Long,
    isFavorite: Boolean,
    image: @Composable () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
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
            .clickable(onClick = onOpen)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            image()
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(category, color = GhostGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 9.dp))
        Text(title, color = Ink, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(36.dp).padding(top = 2.dp))
        if (priceCents > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                com.example.ghostcart.ui.DirhamGlyph(tint = Ink, modifier = Modifier.size(12.dp))
                Text(formatProductPrice(priceCents), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp))
            }
        } else {
            Text(formatProductPrice(priceCents), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 3.dp))
        }
        Box(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(SoftGray).border(1.dp, FaintBorder, RoundedCornerShape(12.dp)).clickable(onClick = onGhost),
                contentAlignment = Alignment.Center
            ) { Text("Add to cart", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Box(
                modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(GhostGreen).clickable(onClick = onCool),
                contentAlignment = Alignment.Center
            ) { Text("Cool it", color = Color(0xFF0A0A0A), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
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
    "%,.2f".format(java.util.Locale.US, priceCents / 100.0)
} else {
    "Add price"
}
