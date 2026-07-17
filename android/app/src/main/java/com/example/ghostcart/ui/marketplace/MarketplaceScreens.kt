package com.example.ghostcart.ui.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.MarketplaceCategory
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
import com.example.ghostcart.ui.GhostCartWordmark
import com.example.ghostcart.ui.ProductPhoto
import com.example.ghostcart.ui.common.BackButton
import com.example.ghostcart.ui.common.ForwardChevron
import com.example.ghostcart.ui.common.PrimaryButton
import com.example.ghostcart.ui.common.RoundIconButton
import com.example.ghostcart.ui.common.SecondaryButton
import com.example.ghostcart.ui.common.materialIconFor

@Composable
fun HomeMarketplaceScreen(
    mostGhostedToday: List<Pair<MarketplaceProduct, Int>>,
    isMostGhostedLoading: Boolean,
    isMostGhostedUnavailable: Boolean,
    onRefreshMostGhosted: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenTrends: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    onAddToCart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Paper)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).verticalScroll(rememberScrollState()).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostCartWordmark(modifier = Modifier.height(42.dp).weight(1f))
                RoundIconButton(icon = Icons.Filled.Notifications, onClick = {})
                Spacer(modifier = Modifier.width(8.dp))
                RoundIconButton(icon = Icons.Filled.Person, onClick = onOpenWallet)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoftGray)
                    .clickable { onOpenCategory("all") }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MutedText, modifier = Modifier.size(18.dp))
                Text(
                    text = "What are you tempted to buy?",
                    color = MutedText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            Text(
                text = "Browse categories",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 16.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 2.dp)
            ) {
                items(Marketplace.browseCategories) { category ->
                    MarketplaceCategoryCard(
                        category = category,
                        onClick = { onOpenCategory(category.id) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, FaintBorder, RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenTrends)
                    .padding(16.dp)
            ) {
                GhostMascotPose(poseName = "wallet", modifier = Modifier.size(44.dp))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(text = "Track almost-spending", color = GhostGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = "See your Ghost Cart insights.", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                ForwardChevron()
            }

            MarketplaceSectionHeader(
                title = "Most Ghosted Today",
                badge = if (mostGhostedToday.isNotEmpty()) "LIVE" else null,
                onViewAll = { onOpenCategory("most_ghosted") }
            )
            when {
                isMostGhostedLoading -> LiveActivityMessage(
                    message = "Loading today's ghost activity…"
                )
                isMostGhostedUnavailable -> LiveActivityMessage(
                    message = "Live activity is temporarily unavailable.",
                    actionLabel = "Retry",
                    onAction = onRefreshMostGhosted
                )
                mostGhostedToday.isEmpty() -> LiveActivityMessage(
                    message = "No items have been ghosted yet today. Be the first."
                )
                else -> LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(mostGhostedToday, key = { it.first.id }) { (product, ghostCount) ->
                        MarketplaceProductCard(
                            product = product,
                            activityLabel = "$ghostCount ${if (ghostCount == 1) "ghost" else "ghosts"} today",
                            onClick = { onOpenProduct(product.id) },
                            onAdd = { onAddToCart(product.id) }
                        )
                    }
                }
            }

            MarketplaceSectionHeader(title = "Fake Flash Deals", onViewAll = { onOpenCategory("flash_deals") })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(Marketplace.fakeFlashDeals) { product ->
                    MarketplaceProductCard(product, onClick = { onOpenProduct(product.id) }, onAdd = { onAddToCart(product.id) })
                }
            }

            MarketplaceSectionHeader(title = "Salary Protection Picks", onViewAll = { onOpenCategory("all") })
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Guard your\nsalary" to "shield", "Avoid impulse\npurchases" to "target", "Save more.\nStress less." to "savings", "Build better\nmoney habits" to "chart").forEach { (label, icon) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(materialIconFor(icon), contentDescription = null, tint = GhostGreen, modifier = Modifier.size(18.dp))
                        Text(
                            text = label,
                            color = Ink,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ink)
                    .clickable { onOpenCategory("food") }
                    .padding(16.dp)
            ) {
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(28.dp))
                Text(
                    text = "Don't Chase Offers. Ghost Them.",
                    color = Paper,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 10.dp)
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Paper, modifier = Modifier.size(16.dp))
            }

            MarketplaceSectionHeader(title = "Brand Simulations", badge = "DEMO", onViewAll = { onOpenCategory("all") })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp)) {
                items(Marketplace.sponsoredBrands) { brand ->
                    Column(
                        modifier = Modifier
                            .width(144.dp)
                            .height(142.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, FaintBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(GreenTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = brand.logoMark, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            text = brand.name,
                            color = Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = brand.tagline,
                            color = MutedText,
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveActivityMessage(
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        GhostMascotPose(poseName = "wave", modifier = Modifier.size(30.dp))
        Text(
            text = message,
            color = MutedText,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                color = GhostGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clickable(role = Role.Button, onClick = onAction)
            )
        }
    }
}

@Composable
private fun MarketplaceCategoryCard(
    category: MarketplaceCategory,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SoftGray)
        ) {
            Icon(
                imageVector = materialIconFor(category.iconName),
                contentDescription = category.label,
                tint = Ink,
                modifier = Modifier.size(23.dp)
            )
        }
        Text(
            text = category.label,
            color = Ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp)
        )
    }
}

@Composable
private fun MarketplaceSectionHeader(title: String, badge: String? = null, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            if (badge != null) {
                Text(
                    text = badge,
                    color = Ink,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .padding(start = 7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(GreenTint)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
        Text(
            text = "View all",
            color = GhostGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onViewAll)
        )
    }
}

@Composable
fun MarketplaceProductCard(
    product: MarketplaceProduct,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    activityLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(150.dp)
            .height(226.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, FaintBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SoftGray),
            contentAlignment = Alignment.Center
        ) {
            ProductPhoto(productName = product.name, fallbackIconName = iconForProduct(product), modifier = Modifier.fillMaxSize())
            if (activityLabel != null) {
                Text(
                    text = activityLabel,
                    color = Paper,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Ink)
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }
        }
        // Fixed height (not intrinsic) so a 1-line vs 2-line title never changes how much
        // room is left for the button below — this is what caused the button to get
        // clipped for longer titles when the card's overall height is also fixed.
        Text(
            text = product.name,
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            modifier = Modifier.padding(top = 8.dp).height(32.dp)
        )
        Text(text = "${Marketplace.currency} ${product.price}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Ink)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Add to Ghost Cart", color = Paper, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryBrowseScreen(
    categoryId: String,
    products: List<MarketplaceProduct>,
    activityCounts: Map<String, Int> = emptyMap(),
    cartItemCount: Int,
    cartTotal: Int,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onAddToCart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember(categoryId) { mutableStateOf("All") }
    val filters = if (categoryId == "food") {
        listOf("All", "Fast Food", "Coffee & Drinks", "Healthy")
    } else {
        listOf("All")
    }
    val visibleProducts = if (selectedFilter == "All") {
        products
    } else {
        products.filter { it.category.equals(selectedFilter, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onBack = onBack)
            Spacer(modifier = Modifier.weight(1f))
            CartSummaryButton(
                itemCount = cartItemCount,
                cartTotal = cartTotal,
                onClick = onOpenCart
            )
        }

        val (title, subtitle) = when (categoryId) {
            "food" -> "Food & drinks" to "Ghost delivery cravings before they reach checkout."
            "beauty" -> "Beauty & self-care" to "A calm place for every almost-bought beauty item."
            "apparel", "fashion" -> "Apparel" to "Cool down late-night wardrobe additions."
            "electronics", "gadgets" -> "Electronics" to "Put tempting tech somewhere safe before buying."
            "music" -> "Music instruments" to "Save the gear you want and review it later."
            "jewelry" -> "Jewellery" to "Keep considered pieces in a simulated cart."
            "gaming" -> "Gaming" to "Ghost the upgrade before it reaches real checkout."
            "home" -> "Home" to "Collect home ideas without making a real purchase."
            "most_ghosted" -> "Most Ghosted Today" to "Real anonymous Ghost Checkout activity from today."
            "flash_deals" -> "Fake Flash Deals" to "Simulated urgency, with no real purchase or payment."
            "all" -> "Almost-Spent Catalog" to "Browse simulation items you can add to Ghost Cart."
            else -> "Shop cravings" to "Simulate the cart and protect your budget."
        }

        Text(text = title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
        Text(text = subtitle, color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        if (filters.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
                items(filters) { filter ->
                    val selected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) Ink else Paper)
                            .border(1.dp, if (selected) Ink else FaintBorder, RoundedCornerShape(999.dp))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(text = filter, color = if (selected) Paper else Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(1.dp, FaintBorder, CircleShape)
                            .padding(10.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = "More filters", tint = Ink, modifier = Modifier.size(14.dp))
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GreenTint)
                .padding(14.dp)
        ) {
            Text(
                text = "Simulation only. No real payment. No real delivery.",
                color = Ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        if (visibleProducts.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 56.dp)
            ) {
                Icon(materialIconFor("bag"), contentDescription = null, tint = MutedText, modifier = Modifier.size(36.dp))
                Text(
                    text = if (categoryId == "most_ghosted") "No ghost activity yet today" else "No demo items here yet",
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Text(
                    text = if (categoryId == "most_ghosted") {
                        "Ghost an item to start today's live list."
                    } else {
                        "This category is ready. Products will appear when the catalog is added."
                    },
                    color = MutedText,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visibleProducts) { product ->
                    MarketplaceProductCard(
                        product = product,
                        activityLabel = activityCounts[product.id]?.let { count ->
                            "$count ${if (count == 1) "ghost" else "ghosts"} today"
                        },
                        onClick = { onOpenProduct(product.id) },
                        onAdd = { onAddToCart(product.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CartSummaryButton(
    itemCount: Int,
    cartTotal: Int,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Ink)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ShoppingBag,
            contentDescription = null,
            tint = GhostGreen,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.padding(start = 9.dp)) {
            Text(
                text = "View cart",
                color = Paper,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = "$itemCount ${if (itemCount == 1) "item" else "items"} · ${Marketplace.currency} $cartTotal",
                color = Paper.copy(alpha = 0.68f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ProductDetailScreen(
    product: MarketplaceProduct,
    coolingUntilMillis: Long?,
    onBack: () -> Unit,
    onAddToCart: () -> Unit,
    onGhostBuyNow: () -> Unit,
    onStartCooling: () -> Unit,
    modifier: Modifier = Modifier
) {
    var favorited by remember { mutableStateOf(false) }
    val coolingComplete = coolingUntilMillis != null && coolingUntilMillis <= System.currentTimeMillis()
    val coolingActive = coolingUntilMillis != null && !coolingComplete

    Column(modifier = modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onBack = onBack)
            Text(text = "Ghost Cart", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f).padding(start = 10.dp))
            RoundIconButton(icon = Icons.Filled.Share, onClick = {})
            Spacer(modifier = Modifier.width(8.dp))
            RoundIconButton(icon = if (favorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, onClick = { favorited = !favorited })
        }

        if (product.highEmotion) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFFCE4E4))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFE0453C), modifier = Modifier.size(12.dp))
                Text(text = "High-emotion item", color = Color(0xFFE0453C), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
        }

        Text(text = product.name, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
        if (product.description.isNotBlank()) {
            Text(text = product.description, color = MutedText, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }

        Text(text = "${Marketplace.currency} ${product.price}", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(14.dp))
            Text(text = "Safe to Ghost", color = GhostGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(top = 18.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SoftGray),
            contentAlignment = Alignment.Center
        ) {
            ProductPhoto(productName = product.name, fallbackIconName = iconForProduct(product), modifier = Modifier.fillMaxSize())
        }

        if (product.scentOrType.isNotBlank()) {
            DetailRow(label = "Type", value = "Eau de Parfum")
            DetailRow(label = "Scent Profile", value = product.scentOrType)
        }
        if (product.size.isNotBlank()) DetailRow(label = "Size", value = product.size)
        if (product.brand.isNotBlank()) DetailRow(label = "Brand", value = product.brand)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SoftGray)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HighlightPoint(Icons.Filled.FavoriteBorder, "High-Emotion Item", "Emotional purchases\noften fade fast.")
            HighlightPoint(materialIconFor("target"), "Simulate First", "Add to Ghost Cart.\nSee if you still want it.")
            HighlightPoint(Icons.Filled.Shield, "Protect Your Money", "Avoid impulse buys.\nSpend smarter.")
        }

        PrimaryButton(text = "Add to Ghost Cart", onClick = onAddToCart, modifier = Modifier.padding(top = 18.dp))
        SecondaryButton(text = "Ghost Buy Now", onClick = onGhostBuyNow, modifier = Modifier.padding(top = 10.dp))
        Text(text = "Instant. No checkout. No payment.", color = GhostGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, FaintBorder, RoundedCornerShape(14.dp))
                .background(if (coolingUntilMillis != null) GreenTint else Paper)
                .clickable(role = Role.Button, onClick = onStartCooling)
                .padding(14.dp)
        ) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    text = when {
                        coolingComplete -> "Cooling complete"
                        coolingActive -> "24-Hour Cooling active"
                        else -> "Start 24-Hour Cooling"
                    },
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        coolingComplete -> "Review the item now. Do you still want it? Tap to cool it again."
                        coolingActive -> "We'll notify you when this item has cooled off."
                        else -> "Pause this craving and get a reminder after 24 hours."
                    },
                    color = MutedText,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            ForwardChevron()
        }

    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(text = label, color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(text = value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HighlightPoint(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, caption: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
        Icon(icon, contentDescription = null, tint = GhostGreen, modifier = Modifier.size(18.dp))
        Text(text = title, color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Text(text = caption, color = MutedText, fontSize = 8.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
    }
}
