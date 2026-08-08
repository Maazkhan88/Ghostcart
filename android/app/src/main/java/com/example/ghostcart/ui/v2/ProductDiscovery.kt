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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostcart.app.R
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.data.AlmostBuy
import com.example.ghostcart.data.GhostDeliveryState
import com.example.ghostcart.data.ghostDeliverySnapshot
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
import com.example.ghostcart.ui.common.ProductCardSpotlightTarget
import com.example.ghostcart.ui.common.GhostSearchField
import com.example.ghostcart.ui.common.GhostSectionHeader
import com.example.ghostcart.ui.common.GhostSegmentedControl
import com.example.ghostcart.theme.ExpressivePrimaryText
import com.example.ghostcart.ui.tutorial.TutorialGuideOverlay
import com.example.ghostcart.ui.tutorial.TutorialGuideSpec
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun ProductDiscoverySection(
    unifiedProducts: List<MarketplaceProduct>,
    favoriteProducts: List<MarketplaceProduct>,
    favoriteProductIds: Set<String>,
    communityProductsLoading: Boolean,
    onGhost: (String) -> Unit,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShareProduct: (MarketplaceProduct) -> Unit,
    onNotifications: () -> Unit,
    onViewAllCatalog: (String) -> Unit,
    onViewAllFavorites: () -> Unit,
    activeDelivery: AlmostBuy? = null,
    onTrackDelivery: (String) -> Unit = {},
    tutorialProductId: String? = null,
    tutorialSpotlightStep: Int = 0,
    onTutorialAdvance: () -> Unit = {},
    onTutorialGhost: () -> Unit = {},
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
    var rootBounds by remember { mutableStateOf<Rect?>(null) }
    var tutorialTargetBounds by remember(tutorialSpotlightStep) { mutableStateOf<Rect?>(null) }
    val tutorialTarget = when (tutorialSpotlightStep.coerceIn(0, 4)) {
        0 -> ProductCardSpotlightTarget.CARD
        1 -> ProductCardSpotlightTarget.FAVORITE
        2 -> ProductCardSpotlightTarget.SHARE
        3 -> ProductCardSpotlightTarget.REVIEWS
        else -> ProductCardSpotlightTarget.GHOST
    }
    val tutorialMessages = listOf(
        "This is something you almost bought.",
        "Save products you like without Ghosting them.",
        "Share an almost-buy or ask someone what they think.",
        "See ratings, reviews and community comments.",
        "Tap Ghost it instead of buying immediately."
    )

    Box(
        modifier = Modifier.fillMaxWidth().onGloballyPositioned { rootBounds = it.boundsInWindow() }
    ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                com.example.ghostcart.ui.GhostCartWordmark(
                    modifier = Modifier.align(Alignment.Center).width(132.dp).height(32.dp),
                    tint = ExpressivePrimaryText
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
        if (tutorialProductId == null) {
            PromoBannerCarousel(banners = homeBanners)
        }
        GhostSearchField(
            value = query,
            onValueChange = { query = it.take(80) },
            placeholder = "Search products",
            modifier = Modifier.fillMaxWidth()
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Marketplace.browseCategories.filterNot { it.id == "food" }, key = { it.id }) { category ->
                GhostCategoryChip(
                    selected = categoryId == category.id,
                    onClick = { categoryId = category.id },
                    label = category.label
                )
            }
        }
        activeDelivery?.let { delivery ->
            ActiveGhostDeliveryCard(delivery = delivery, onTrack = { onTrackDelivery(delivery.id) })
        }
        GhostSectionHeader(
            title = "Marketplace products",
            subtitle = "browse every temptation",
            onViewAll = { onViewAllCatalog(categoryId) }
        )
        GhostSegmentedControl(
            options = listOf("All", "User Ghosted"),
            selectedIndex = if (userGhostedOnly) 1 else 0,
            onSelect = { userGhostedOnly = it == 1 }
        )
        if (userGhostedOnly && communityProductsLoading && visibleCatalog.none { it.isUserGhosted }) {
            Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(18.dp)).background(SoftGray))
        } else if (visibleCatalog.isEmpty()) {
            Text(
                text = if (userGhostedOnly) {
                    "No user-ghosted finds yet in this category."
                } else {
                    "No catalogue matches yet."
                },
                color = MutedText,
                fontSize = 12.sp
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visibleCatalog.take(12), key = { it.id }) { product ->
                    val isTutorialCard = product.id == tutorialProductId
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
                        onOpen = { if (isTutorialCard && tutorialSpotlightStep == 0) onTutorialAdvance() else onOpen(product.id) },
                        onToggleFavorite = { if (isTutorialCard && tutorialSpotlightStep == 1) onTutorialAdvance() else onToggleFavorite(product.id) },
                        onGhost = { if (isTutorialCard && tutorialSpotlightStep >= 4) onTutorialGhost() else onGhost(product.id) },
                        onShare = { if (isTutorialCard && tutorialSpotlightStep == 2) onTutorialAdvance() else onShareProduct(product) },
                        onReviews = { if (isTutorialCard && tutorialSpotlightStep == 3) onTutorialAdvance() else onOpen(product.id) },
                        spotlightTarget = tutorialTarget.takeIf { isTutorialCard },
                        onSpotlightBounds = if (isTutorialCard) {
                            { bounds: Rect -> tutorialTargetBounds = bounds }
                        } else null
                    )
                }
            }
        }
        GhostSectionHeader(
            title = "Food & delivery",
            subtitle = "Ghost lunch, dinner or a delivery craving",
            onViewAll = { onViewAllCatalog("food") }
        )
        if (foodProducts.isEmpty()) {
            Text(
                "No food matches yet.",
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
                        onGhost = { onGhost(product.id) },
                        onShare = { onShareProduct(product) },
                        onReviews = { onOpen(product.id) }
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

    if (tutorialProductId != null) {
        val root = rootBounds
        val target = tutorialTargetBounds
        TutorialGuideOverlay(
            targetBounds = if (root != null && target != null) {
                Rect(
                    target.left - root.left,
                    target.top - root.top,
                    target.right - root.left,
                    target.bottom - root.top
                )
            } else null,
            guide = TutorialGuideSpec(
                message = tutorialMessages[tutorialSpotlightStep.coerceIn(0, 4)],
                stepLabel = "MARKETPLACE ${tutorialSpotlightStep.coerceIn(0, 4) + 1} OF 5"
            ),
            modifier = Modifier.matchParentSize()
        )
    }
    }

}

@Composable
private fun ActiveGhostDeliveryCard(delivery: AlmostBuy, onTrack: () -> Unit) {
    var now by remember(delivery.id) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(delivery.id) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val snapshot = ghostDeliverySnapshot(
        nowMillis = now,
        startMillis = delivery.deliveryStartedAtMillis,
        endMillis = delivery.deliveryEndsAtMillis,
        persistedState = delivery.deliveryState
    )
    val remaining = (delivery.deliveryEndsAtMillis - now).coerceAtLeast(0L)
    val remainingLabel = when {
        remaining >= TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toDays(remaining)}d ${TimeUnit.MILLISECONDS.toHours(remaining) % 24}h"
        remaining >= TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(remaining)}h ${TimeUnit.MILLISECONDS.toMinutes(remaining) % 60}m"
        else -> "${TimeUnit.MILLISECONDS.toMinutes(remaining)}m ${TimeUnit.MILLISECONDS.toSeconds(remaining) % 60}s"
    }
    val stageLabel = when (snapshot.state) {
        GhostDeliveryState.PLACED -> "Ghost Order placed"
        GhostDeliveryState.PREPARING -> "Being prepared"
        GhostDeliveryState.RIDER_PICKING_UP -> "Ghost Rider picking up"
        GhostDeliveryState.OUT_FOR_DELIVERY -> "Out for Ghost Delivery"
        GhostDeliveryState.RIDER_NEARBY -> "Ghost Rider nearby"
        GhostDeliveryState.DELIVERED -> "Delivered · decision ready"
        else -> "Ghost Delivery"
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF151715),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("YOUR GHOST DELIVERY", color = GhostGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text(delivery.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$stageLabel · $remainingLabel", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
            LinearProgressIndicator(
                progress = { snapshot.progress },
                color = GhostGreen,
                trackColor = Color.White.copy(alpha = .10f),
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
            )
            Button(
                onClick = onTrack,
                colors = ButtonDefaults.buttonColors(containerColor = GhostGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (snapshot.state == GhostDeliveryState.DELIVERED) "Make decision" else "Track Ghost Delivery", fontWeight = FontWeight.Bold) }
            Text("Simulation only · No real product or rider is involved.", color = Color.White.copy(alpha = .54f), fontSize = 9.sp)
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

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth().aspectRatio(3f)
    ) { page ->
        if (banners.isNotEmpty()) {
            coil3.compose.AsyncImage(
                model = banners[page].imageUrl,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(PROMO_BANNER_DRAWABLES[page]),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
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

@Composable
private fun DiscoveryProductCard(
    title: String,
    category: String,
    priceCents: Long,
    isFavorite: Boolean,
    image: @Composable () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onGhost: () -> Unit
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GhostGreen)
                .clickable(onClick = onGhost),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ghost it", color = Color(0xFF0A0A0A), fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
                Text("Add to Ghost Cart", color = Color(0xFF0A0A0A).copy(alpha = 0.68f), fontSize = 9.sp, lineHeight = 11.sp)
            }
        }
    }
}

private fun formatProductPrice(priceCents: Long): String = if (priceCents > 0) {
    "%,.2f".format(java.util.Locale.US, priceCents / 100.0)
} else {
    "Add price"
}
