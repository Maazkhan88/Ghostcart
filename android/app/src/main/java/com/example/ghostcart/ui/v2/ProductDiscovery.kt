package com.example.ghostcart.ui.v2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.ghostcart.app.R
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
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.ProductPhoto
import com.example.ghostcart.ui.common.CoolingDurationDialog
import kotlinx.coroutines.delay

@Composable
fun ProductDiscoverySection(
    unifiedProducts: List<MarketplaceProduct>,
    favoriteProducts: List<MarketplaceProduct>,
    favoriteProductIds: Set<String>,
    communityProductsLoading: Boolean,
    onGhost: (String) -> Unit,
    onCool: (id: String, durationMillis: Long, durationLabel: String) -> Unit,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onNotifications: () -> Unit,
    onViewAllCatalog: (String) -> Unit,
    onViewAllFavorites: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("all") }
    var userGhostedOnly by remember { mutableStateOf(false) }
    // The cooling duration is always a user choice - tapping "Cool it" opens this picker instead
    // of silently applying a fixed default.
    var pendingCoolProductId by remember { mutableStateOf<String?>(null) }
    val categoryProducts = Marketplace.productsForCategory(categoryId, unifiedProducts)
    val visibleCatalog = categoryProducts.filter {
        (!userGhostedOnly || it.isUserGhosted) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true))
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
            GhostPeekMascot(modifier = Modifier.align(Alignment.CenterStart))
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !userGhostedOnly,
                onClick = { userGhostedOnly = false },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = Paper)
            )
            FilterChip(
                selected = userGhostedOnly,
                onClick = { userGhostedOnly = true },
                label = { Text("User Ghosted") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = Paper)
            )
        }
        if (userGhostedOnly && communityProductsLoading && visibleCatalog.none { it.isUserGhosted }) {
            Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(18.dp)).background(SoftGray))
        } else if (visibleCatalog.isEmpty()) {
            Text(
                text = if (userGhostedOnly) {
                    "No user-ghosted finds yet in this category."
                } else {
                    "No catalogue matches. Paste the product link in Ghost + instead."
                },
                color = MutedText,
                fontSize = 12.sp
            )
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
                        onOpen = { onOpen(product.id) },
                        onToggleFavorite = { onToggleFavorite(product.id) },
                        onGhost = { onGhost(product.id) },
                        onCool = { pendingCoolProductId = product.id }
                    )
                }
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
                        onOpen = { onOpen(product.id) },
                        onToggleFavorite = { onToggleFavorite(product.id) },
                        onGhost = { onGhost(product.id) },
                        onCool = { pendingCoolProductId = product.id }
                    )
                }
            }
        }
    }

    pendingCoolProductId?.let { productId ->
        CoolingDurationDialog(
            onConfirm = { option ->
                onCool(productId, option.durationMillis, option.label)
                pendingCoolProductId = null
            },
            onDismiss = { pendingCoolProductId = null }
        )
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

private val GHOST_CART_STORY_DRAWABLES = listOf(
    R.drawable.ghost_cart_story_1,
    R.drawable.ghost_cart_story_2,
    R.drawable.ghost_cart_story_3,
    R.drawable.ghost_cart_story_4,
    R.drawable.ghost_cart_story_5,
    R.drawable.ghost_cart_story_6,
    R.drawable.ghost_cart_story_7,
    R.drawable.ghost_cart_story_8,
    R.drawable.ghost_cart_story_9,
    R.drawable.ghost_cart_story_10,
)

/**
 * Editorial carousel shown right after Favorites on Home. Deliberately labeled "Ghost Cart
 * Stories" rather than "User Generated Content" - these are admin-curated marketing cards, not
 * actual user submissions, and the app must not imply otherwise until real UGC exists.
 * Full-bleed (escapes the LazyColumn's contentPadding via negative horizontal padding on the
 * pager only, so the heading above stays at the normal inset) and swipes independently of the
 * page's own vertical scroll, same as [HorizontalPager] always does.
 */
@Composable
fun GhostCartStoriesSection(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { GHOST_CART_STORY_DRAWABLES.size })
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Ghost Cart Stories",
            color = Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().padding(horizontal = (-20).dp)
        ) { page ->
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(GHOST_CART_STORY_DRAWABLES[page]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
            )
        }
    }
}

/**
 * Small home-screen easter egg: the ghost mascot briefly peeks in from the header's empty
 * leading edge, then fades back out. Purely decorative (no click target, no layout impact -
 * the header Box already reserves this space), same lightweight LaunchedEffect/fade pattern
 * as [PromoBannerCarousel], no new dependency.
 */
@Composable
private fun GhostPeekMascot(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(25_000)
            visible = true
            delay(2_500)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        GhostMascotPose(poseName = "peek", modifier = Modifier.size(28.dp))
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

private fun formatProductPrice(priceCents: Long): String = if (priceCents > 0) {
    "%,.2f".format(java.util.Locale.US, priceCents / 100.0)
} else {
    "Add price"
}
