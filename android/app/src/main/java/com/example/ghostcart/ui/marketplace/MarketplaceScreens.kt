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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import com.example.ghostcart.theme.ExpressiveSurface
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.DirhamAmount
import com.example.ghostcart.ui.DirhamGlyph
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.GhostCartWordmark
import com.example.ghostcart.ui.ProductPhoto
import com.example.ghostcart.ui.common.BackButton
import com.example.ghostcart.ui.common.CoolingDurationDialog
import com.example.ghostcart.ui.common.ForwardChevron
import com.example.ghostcart.ui.common.GhostActionPill
import com.example.ghostcart.ui.common.GhostItButton
import com.example.ghostcart.ui.common.PrimaryButton
import com.example.ghostcart.ui.common.RoundIconButton
import com.example.ghostcart.ui.common.SecondaryButton
import com.example.ghostcart.ui.common.materialIconFor
import com.example.ghostcart.ui.tutorial.TutorialGuideOverlay
import com.example.ghostcart.ui.tutorial.TutorialGuideSpec
import coil3.compose.AsyncImage

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
                            onAdd = { onAddToCart(product.id) },
                            modifier = Modifier.width(170.dp)
                        )
                    }
                }
            }

            MarketplaceSectionHeader(title = "Fake Flash Deals", onViewAll = { onOpenCategory("flash_deals") })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(Marketplace.fakeFlashDeals) { product ->
                    MarketplaceProductCard(product, onClick = { onOpenProduct(product.id) }, onAdd = { onAddToCart(product.id) }, modifier = Modifier.width(170.dp))
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
    onShare: (() -> Unit)? = null,
    onReviews: (() -> Unit)? = null,
    activityLabel: String? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Mirrors the home DiscoveryProductCard so listing/community cards match the
    // home screen: white image tile with a top-right favorite, green category
    // label, title, Dirham-glyph price, and one predictable Ghost action.
    Surface(
        onClick = onClick,
        modifier = modifier
            // Was 310.dp - same clipping issue as GhostProductCard: stacked
            // content now needs ~289dp against a 286dp budget (310 minus 12dp
            // padding x2) since GhostItButton grew to 52dp, clipping its
            // bottom ~3dp. +5dp closes that gap with a small margin.
            .height(315.dp),
        shape = RoundedCornerShape(20.dp),
        color = ExpressiveSurface,
        tonalElevation = 1.dp,
    ) {
    Column(modifier = Modifier.padding(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            ProductPhoto(productName = product.name, fallbackIconName = iconForProduct(product), modifier = Modifier.fillMaxSize())
            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = "${product.name} product image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().background(Color.White)
                )
            }
            if (onToggleFavorite != null) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Text(
            text = product.category,
            color = GhostGreen,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp)
        )
        Text(
            text = product.name,
            color = Ink,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(36.dp).padding(top = 2.dp)
        )
        DirhamAmount(
            amount = "%,.2f".format(java.util.Locale.US, product.price.toDouble()),
            tint = Ink,
            fontSize = 13.sp,
            glyphSize = 12.dp,
            modifier = Modifier.padding(top = 3.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onReviews != null) {
                Surface(
                    onClick = onReviews,
                    shape = RoundedCornerShape(12.dp),
                    color = SoftGray
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.RateReview, contentDescription = null, tint = MutedText, modifier = Modifier.size(15.dp))
                        Text("No reviews yet", color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (onShare != null) {
                IconButton(onClick = onShare, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = "Share product", tint = Ink, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        GhostItButton(onClick = onAdd)
    }
    }
}

@Composable
fun CategoryBrowseScreen(
    categoryId: String,
    products: List<MarketplaceProduct>,
    activityCounts: Map<String, Int> = emptyMap(),
    favoriteProductIds: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onGhostProduct: (String) -> Unit,
    onShareProduct: (MarketplaceProduct) -> Unit = {},
    onReviews: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember(categoryId) { mutableStateOf("All") }
    var sortOption by remember(categoryId) { mutableStateOf(ProductSortOption.TRENDING) }
    var selectedBrand by remember(categoryId) { mutableStateOf<String?>(null) }
    var userGhostedOnly by remember(categoryId) { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    val filters = if (categoryId == "food") {
        listOf("All", "Fast Food", "Coffee & Drinks", "Healthy")
    } else {
        listOf("All")
    }
    val availableBrands = remember(products) { products.mapNotNull { it.brand }.distinct().sorted() }
    val filteredProducts = products
        .filter { selectedFilter == "All" || it.category.equals(selectedFilter, ignoreCase = true) }
        .filter { selectedBrand == null || it.brand == selectedBrand }
        .filter { !userGhostedOnly || it.isUserGhosted }
    val visibleProducts = sortProducts(filteredProducts, sortOption, activityCounts)
    val filtersActive = selectedFilter != "All" || selectedBrand != null || userGhostedOnly

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BackButton(onBack = onBack)
            Spacer(modifier = Modifier.weight(1f))
            GhostActionPill(
                label = "Sort",
                icon = Icons.Filled.SwapVert,
                active = sortOption != ProductSortOption.TRENDING,
                onClick = { showSortDialog = true }
            )
            GhostActionPill(
                label = "Filter",
                icon = Icons.Filled.FilterList,
                active = filtersActive,
                onClick = { showFiltersDialog = true }
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
            "community" -> "Community Products" to "Anonymous products other Ghost Cart users chose to ghost."
            "favorites" -> "Your Favorites" to "Everything you saved for a calmer decision."
            "all" -> "Almost-Spent Catalog" to "Browse simulation items you can add to Ghost Cart."
            else -> "Shop cravings" to "Simulate the cart and protect your budget."
        }

        Text(text = title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
        Text(text = subtitle, color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

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
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visibleProducts) { product ->
                    MarketplaceProductCard(
                        product = product,
                        isFavorite = product.id in favoriteProductIds,
                        onToggleFavorite = { onToggleFavorite(product.id) },
                        onClick = { onOpenProduct(product.id) },
                        onAdd = { onGhostProduct(product.id) },
                        onShare = { onShareProduct(product) },
                        onReviews = { onReviews(product.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    }

    if (showSortDialog) {
        SortDialog(
            selected = sortOption,
            onSelect = { option ->
                sortOption = option
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showFiltersDialog) {
        FiltersDialog(
            categoryFilters = filters,
            selectedCategoryFilter = selectedFilter,
            availableBrands = availableBrands,
            selectedBrand = selectedBrand,
            userGhostedOnly = userGhostedOnly,
            onApply = { category, brand, ghostedOnly ->
                selectedFilter = category
                selectedBrand = brand
                userGhostedOnly = ghostedOnly
                showFiltersDialog = false
            },
            onDismiss = { showFiltersDialog = false }
        )
    }

}

// Floating Sort + Filter control (matches the reference "Noon"-style pill): a
// single rounded capsule in the brand green, split by a thin divider into two
// tap targets. Overlays the grid rather than taking layout space, so it stays
// reachable while scrolling instead of living inline above the fold.
@Composable
private fun SortFilterPill(
    filtersActive: Boolean,
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = Color(0xFF050505)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GhostGreen)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(role = Role.Button) { onSortClick() }
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Icon(Icons.Filled.SwapVert, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Text("Sort", color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
        }
        Box(
            modifier = Modifier
                .height(20.dp)
                .width(1.dp)
                .background(contentColor.copy(alpha = 0.25f))
        )
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(role = Role.Button) { onFilterClick() }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Filled.FilterList, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Text("Filter", color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
            if (filtersActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 12.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4342F))
                )
            }
        }
    }
}

@Composable
private fun SortDialog(
    selected: ProductSortOption,
    onSelect: (ProductSortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort by") },
        text = {
            Column {
                ProductSortOption.entries.forEach { option ->
                    val isSelected = option == selected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { onSelect(option) }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) GhostGreen else FaintBorder,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            option.label,
                            color = Ink,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun FiltersDialog(
    categoryFilters: List<String>,
    selectedCategoryFilter: String,
    availableBrands: List<String>,
    selectedBrand: String?,
    userGhostedOnly: Boolean,
    onApply: (category: String, brand: String?, userGhostedOnly: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(selectedCategoryFilter) }
    var brand by remember { mutableStateOf(selectedBrand) }
    var ghostedOnly by remember { mutableStateOf(userGhostedOnly) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filters") },
        text = {
            Column {
                if (categoryFilters.size > 1) {
                    Text("Category", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryFilters) { candidate ->
                            FilterChip(
                                selected = category == candidate,
                                onClick = { category = candidate },
                                label = { Text(candidate) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenTint, selectedLabelColor = Ink)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (availableBrands.isNotEmpty()) {
                    Text("Brand", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Items without a known brand aren't shown here - never guessed.",
                        color = MutedText,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = brand == null,
                                onClick = { brand = null },
                                label = { Text("All brands") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenTint, selectedLabelColor = Ink)
                            )
                        }
                        items(availableBrands) { candidate ->
                            FilterChip(
                                selected = brand == candidate,
                                onClick = { brand = candidate },
                                label = { Text(candidate) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenTint, selectedLabelColor = Ink)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text("Source", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FilterChip(
                    selected = ghostedOnly,
                    onClick = { ghostedOnly = !ghostedOnly },
                    label = { Text("User Ghosted only") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenTint, selectedLabelColor = Ink)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onApply(category, brand, ghostedOnly) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Sort semantics (must match the UI labels exactly):
 * - Trending: all-time ghost count weighted by an exponential recency decay (48h half-life) on
 *   the item's last known ghost activity - recent activity counts more than old activity. Items
 *   with no recency signal (e.g. static catalog items) use a modest constant weight rather than
 *   being suppressed to zero or granted a fabricated recency.
 * - Most Ghosted: raw ghost count within today's fixed window (state.mostGhostedToday, passed in
 *   via [activityCounts]) - a hard "today" cutoff, deliberately different from Trending's decay.
 * - Recent: last known ghost-activity timestamp, descending. Items with no timestamp (catalog
 *   items lacking any recency signal) sort after everything that has one, in their original order
 *   - never given a fabricated recent timestamp just to rank higher.
 */
private enum class ProductSortOption(val label: String) {
    TRENDING("Trending"),
    MOST_GHOSTED("Most Ghosted"),
    RECENT("Recent")
}

// User-ghosted items always cluster first, regardless of which sort metric is chosen - the
// chosen metric only orders within each group (user-ghosted vs. not).
private fun sortProducts(
    products: List<MarketplaceProduct>,
    sortOption: ProductSortOption,
    activityCounts: Map<String, Int>
): List<MarketplaceProduct> = when (sortOption) {
    ProductSortOption.MOST_GHOSTED -> products.sortedWith(
        compareByDescending<MarketplaceProduct> { it.isUserGhosted }
            .thenByDescending { activityCounts[it.id] ?: 0 }
    )
    ProductSortOption.RECENT -> products.sortedWith(
        compareByDescending<MarketplaceProduct> { it.isUserGhosted }
            .thenByDescending { it.lastGhostedAtMillis != null }
            .thenByDescending { it.lastGhostedAtMillis ?: 0L }
    )
    ProductSortOption.TRENDING -> products.sortedWith(
        compareByDescending<MarketplaceProduct> { it.isUserGhosted }
            .thenByDescending { trendingScore(it) }
    )
}

private fun trendingScore(product: MarketplaceProduct): Double {
    val baseCount = product.ghostCount.coerceAtLeast(0).toDouble()
    val lastActivity = product.lastGhostedAtMillis
    val decayWeight = if (lastActivity != null) {
        val hoursSince = (System.currentTimeMillis() - lastActivity).coerceAtLeast(0L) / 3_600_000.0
        Math.pow(0.5, hoursSince / 48.0)
    } else {
        0.3
    }
    return baseCount * decayWeight
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
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Ink)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ShoppingBag,
            contentDescription = null,
            tint = GhostGreen,
            modifier = Modifier.size(18.dp)
        )
        Column(
            modifier = Modifier.padding(start = 9.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "View cart",
                color = Paper,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$itemCount ${if (itemCount == 1) "item" else "items"} ·",
                    color = Paper.copy(alpha = 0.68f),
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                DirhamGlyph(
                    tint = Paper.copy(alpha = 0.68f),
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(8.dp)
                )
                Text(
                    text = "$cartTotal",
                    color = Paper.copy(alpha = 0.68f),
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ProductDetailScreen(
    product: MarketplaceProduct,
    coolingUntilMillis: Long?,
    isFavorite: Boolean,
    ghostCount: Int,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onGhost: () -> Unit,
    isInCart: Boolean = false,
    onOpenCart: () -> Unit = {},
    onOpenCooldown: () -> Unit,
    tutorialGuide: TutorialGuideSpec? = null,
    modifier: Modifier = Modifier
) {
    val coolingComplete = coolingUntilMillis != null && coolingUntilMillis <= System.currentTimeMillis()
    val coolingActive = coolingUntilMillis != null && !coolingComplete
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var tutorialTarget by remember { mutableStateOf<Rect?>(null) }
    var showReviews by remember { mutableStateOf(false) }
    val tutorialActionText = if (isInCart) "View Ghost Cart" else "Ghost it"
    val tutorialAction = if (isInCart) onOpenCart else onGhost

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .onGloballyPositioned { rootPosition = it.positionInRoot() }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = if (tutorialGuide != null) 104.dp else 0.dp)
            .then(if (tutorialGuide != null) Modifier.blur(5.dp) else Modifier)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onBack = onBack)
            Text(text = "Ghost Cart", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f).padding(start = 10.dp))
            RoundIconButton(icon = Icons.Filled.Share, onClick = onShare)
            Spacer(modifier = Modifier.width(8.dp))
            RoundIconButton(
                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                onClick = onToggleFavorite
            )
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

        DirhamAmount(
            amount = "%,.2f".format(java.util.Locale.US, product.price.toDouble()),
            tint = Ink,
            fontSize = 24.sp,
            glyphSize = 20.dp,
            modifier = Modifier.padding(top = 12.dp)
        )
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
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            ProductPhoto(productName = product.name, fallbackIconName = iconForProduct(product), modifier = Modifier.fillMaxSize())
            if (product.imageUrl != null) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = "${product.name} product image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().background(Color.White)
                )
            }
        }
        Text(
            text = when (ghostCount) {
                1 -> "Ghosted 1 time"
                else -> "Ghosted $ghostCount times"
            },
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp)
        )

        if (product.scentOrType.isNotBlank()) {
            DetailRow(label = "Type", value = "Eau de Parfum")
            DetailRow(label = "Scent Profile", value = product.scentOrType)
        }
        if (product.size.isNotBlank()) DetailRow(label = "Size", value = product.size)
        if (!product.brand.isNullOrBlank()) DetailRow(label = "Brand", value = product.brand)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SoftGray)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HighlightPoint(Icons.Filled.ShoppingBag, "Ghost it", "Add it to your\nGhost Cart.")
            HighlightPoint(Icons.Filled.Notifications, "Get reminded", "Push, email and\nin-app reminder.")
            HighlightPoint(Icons.Filled.Shield, "Decide calmly", "Skip, buy, record,\nor restart.")
        }

        Surface(
            onClick = { showReviews = true },
            shape = RoundedCornerShape(18.dp),
            color = SoftGray,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.RateReview, contentDescription = null, tint = GhostGreen)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Ratings, reviews & comments", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("No reviews yet", color = MutedText, fontSize = 11.sp)
                }
            }
        }

        if (tutorialGuide == null) PrimaryButton(
            text = when {
                coolingComplete -> "Make your decision"
                coolingActive -> "View cooldown"
                isInCart -> "View Ghost Cart"
                else -> "Ghost it"
            },
            onClick = when {
                coolingUntilMillis != null -> onOpenCooldown
                isInCart -> onOpenCart
                else -> onGhost
            },
            modifier = Modifier.padding(top = 18.dp),
            containerColor = GhostGreen,
            contentColor = Color(0xFF050505)
        )
        if (tutorialGuide == null) Text(
            text = if (coolingUntilMillis == null) {
                "Add this item to your Ghost Cart. You will choose a Ghost Delivery time at checkout."
            } else if (coolingComplete) {
                "Your item is ready. Skip it, visit the source, record it as bought, or restart the timer."
            } else {
                "This item is cooling. We’ll remind you when it is ready for a decision."
            },
            color = MutedText,
            fontSize = 10.sp,
            // 96dp (not 24dp) - this Column scrolls under the transparent floating nav pill
            // (Navigation.kt), so its natural resting/end-of-scroll position needs to clear
            // the pill's real height, or the "Ghost it" button lands behind it.
            modifier = Modifier.padding(top = 7.dp, bottom = 96.dp)
        )

    }
    if (tutorialGuide != null) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Paper)
                .border(1.dp, FaintBorder)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            PrimaryButton(
                text = tutorialActionText,
                onClick = tutorialAction,
                containerColor = GhostGreen,
                contentColor = Color(0xFF050505),
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    tutorialTarget = coordinates.boundsInRoot().translate(
                        Offset(-rootPosition.x, -rootPosition.y)
                    )
                }
            )
        }
        TutorialGuideOverlay(
            targetBounds = tutorialTarget,
            guide = tutorialGuide,
            modifier = Modifier.fillMaxSize()
        )
    }
    }
    if (showReviews) {
        AlertDialog(
            onDismissRequest = { showReviews = false },
            title = { Text("Ratings, reviews & comments") },
            text = {
                Text(
                    "No reviews yet. Ghost Cart never invents ratings or community comments.",
                    color = MutedText
                )
            },
            confirmButton = { TextButton(onClick = { showReviews = false }) { Text("Close") } }
        )
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
        Text(text = title, color = Ink, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Text(text = caption, color = MutedText, fontSize = 8.sp, lineHeight = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
    }
}
