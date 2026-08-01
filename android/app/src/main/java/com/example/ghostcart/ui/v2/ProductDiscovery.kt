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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
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
import com.example.ghostcart.ui.common.GhostCategoryChip
import com.example.ghostcart.ui.common.GhostGlassSurface
import com.example.ghostcart.ui.common.GhostIconButton
import com.example.ghostcart.ui.common.GhostProductCard
import com.example.ghostcart.ui.common.GhostSearchField
import com.example.ghostcart.ui.common.GhostSectionHeader
import com.example.ghostcart.ui.common.GhostSegmentedControl
import kotlinx.coroutines.delay

@Composable
fun ProductDiscoverySection(
    unifiedProducts: List<MarketplaceProduct>,
    favoriteProducts: List<MarketplaceProduct>,
    favoriteProductIds: Set<String>,
    communityProductsLoading: Boolean,
    onGhost: (String) -> Unit,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onNotifications: () -> Unit,
    onViewAllCatalog: (String) -> Unit,
    onViewAllFavorites: () -> Unit,
    homeBanners: List<com.example.ghostcart.data.ContentBlockItem> = emptyList()
) {
    var query by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("all") }
    var userGhostedOnly by remember { mutableStateOf(false) }
    val categoryProducts = Marketplace.productsForCategory(categoryId, unifiedProducts)
    val foodProducts = Marketplace.productsForCategory("food", unifiedProducts).filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
    }
    val visibleCatalog = categoryProducts.filter {
        Marketplace.productsForCategory("food", listOf(it)).isEmpty() &&
        (!userGhostedOnly || it.isUserGhosted) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true))
    }
    val visibleFavorites = favoriteProducts.filter {
        (categoryId == "all" || Marketplace.productsForCategory(categoryId, listOf(it)).isNotEmpty()) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GhostGlassSurface(modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                com.example.ghostcart.ui.GhostCartWordmark(
                    modifier = Modifier.align(Alignment.Center).width(132.dp).height(32.dp),
                    tint = Ink
                )
                GhostPeekMascot(modifier = Modifier.align(Alignment.CenterStart))
                GhostIconButton(
                    icon = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    onClick = onNotifications,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
        PromoBannerCarousel(banners = homeBanners)
        GhostSearchField(
            value = query,
            onValueChange = { query = it.take(80) },
            placeholder = "Search products",
            modifier = Modifier.fillMaxWidth()
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Marketplace.browseCategories.filterNot { it.id == "food" }, key = { it.id }) { category ->
                GhostCategoryChip(
                    selected = categoryId == category.id,
                    onClick = { categoryId = category.id },
                    label = category.label,
                )
            }
        }
        GhostSectionHeader(
            title = "Food & delivery",
            subtitle = "Ghost lunch, dinner or a delivery craving",
            onViewAll = { onViewAllCatalog("food") }
        )
        Text(
            "Share from Noon Food, Keeta, Talabat, Deliveroo, Uber Eats or Careem Food using Ghost +.",
            color = MutedText,
            fontSize = 10.sp
        )
        if (foodProducts.isEmpty()) {
            Text(
                "No food matches yet. Share a food-app link using Ghost +.",
                color = MutedText,
                fontSize = 12.sp
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(foodProducts.take(12), key = { "food_${it.id}" }) { product ->
                    GhostProductCard(
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
                                        contentDescription = "${product.name} food image",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().background(Color.White)
                                    )
                                }
                            }
                        },
                        onOpen = { onOpen(product.id) },
                        onToggleFavorite = { onToggleFavorite(product.id) },
                        onGhost = { onGhost(product.id) }
                    )
                }
            }
        }
        GhostSectionHeader(
            title = "Marketplace products",
            subtitle = "browse every temptation",
            onViewAll = { onViewAllCatalog(categoryId) }
        )
        GhostSegmentedControl(
            options = listOf("All", "User Ghosted"),
            selectedIndex = if (userGhostedOnly) 1 else 0,
            onSelect = { userGhostedOnly = it == 1 },
        )
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
                    GhostProductCard(
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
                        onGhost = { onGhost(product.id) }
                    )
                }
            }
        }

        GhostSectionHeader(
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
                    GhostProductCard(
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
                        onGhost = { onGhost(product.id) }
                    )
                }
            }
        }
    }

}
private val PROMO_BANNER_DRAWABLES = listOf(
    R.drawable.home_banner_1,
    R.drawable.home_banner_2,
    R.drawable.home_banner_3,
    R.drawable.home_banner_4,
    R.drawable.home_banner_5,
)

/**
 * Swipeable, image-based home banner carousel - replaces the old single-height (56dp)
 * text-only auto-advancing banner. Fetched from the admin-managed /api/content-blocks
 * (Content tab in /admin); falls back to the bundled drawables only if that fetch hasn't
 * returned anything yet (first frame) or failed (offline), so the carousel is never empty.
 *
 * Sized by the banners' own 3:1 aspect ratio rather than a fixed "double height" (112dp):
 * these are pre-composed marketing graphics with text/logo spread across the full image, not
 * croppable stock photos - a fixed height with ContentScale.Crop was clipping headlines at
 * the top and bottom of the frame. Full-width, uncropped is the correct trade-off here.
 */
@Composable
private fun PromoBannerCarousel(
    banners: List<com.example.ghostcart.data.ContentBlockItem>,
    modifier: Modifier = Modifier,
) {
    val bannerCount = if (banners.isNotEmpty()) banners.size else PROMO_BANNER_DRAWABLES.size
    val pagerState = rememberPagerState(pageCount = { bannerCount })

    LaunchedEffect(pagerState, bannerCount) {
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % bannerCount
            pagerState.animateScrollToPage(next)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f)
        ) { page ->
            if (banners.isNotEmpty()) {
                coil3.compose.AsyncImage(
                    model = banners[page].imageUrl,
                    contentDescription = "Promotion ${page + 1} of $bannerCount",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(PROMO_BANNER_DRAWABLES[page]),
                    contentDescription = "Promotion ${page + 1} of $bannerCount",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(bannerCount) { index ->
                Box(
                    modifier = Modifier
                        .width(if (pagerState.currentPage == index) 18.dp else 6.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (pagerState.currentPage == index) GhostGreen else FaintBorder)
                )
            }
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
)

/**
 * Editorial carousel shown right after Favorites on Home. Deliberately labeled "Ghost Cart
 * Stories" rather than "User Generated Content" - these are admin-curated marketing cards, not
 * actual user submissions, and the app must not imply otherwise until real UGC exists.
 * Uses the same card treatment (width, corner radius, border, background) as
 * [DiscoveryProductCard] rather than full-bleed images, so it reads as part of the same product
 * grid rather than an oversized inserted banner.
 *
 * Fetched from the admin-managed /api/content-blocks (Content tab in /admin); falls back to
 * the bundled drawables only if that fetch hasn't returned anything yet or failed.
 */
@Composable
fun GhostCartStoriesSection(
    stories: List<com.example.ghostcart.data.ContentBlockItem> = emptyList(),
    onOpenStory: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ghost Cart Stories", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (stories.isNotEmpty()) {
                itemsIndexed(stories) { index, story ->
                    Box(
                        modifier = Modifier
                            .width(188.dp)
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Paper)
                            .border(1.dp, FaintBorder, RoundedCornerShape(20.dp))
                            .clickable { onOpenStory(index) }
                    ) {
                        coil3.compose.AsyncImage(
                            model = story.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                items(GHOST_CART_STORY_DRAWABLES) { drawableRes ->
                    Box(
                        modifier = Modifier
                            .width(188.dp)
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Paper)
                            .border(1.dp, FaintBorder, RoundedCornerShape(20.dp))
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(drawableRes),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
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
