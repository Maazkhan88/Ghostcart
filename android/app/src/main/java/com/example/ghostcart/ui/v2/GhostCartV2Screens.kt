package com.example.ghostcart.ui.v2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ghostcart.app.BuildConfig
import com.ghostcart.app.R
import com.example.ghostcart.data.AlmostBuy
import com.example.ghostcart.data.AlmostBuyDraft
import com.example.ghostcart.data.AlmostBuyResolution
import com.example.ghostcart.data.AlmostBuyStatus
import com.example.ghostcart.data.GhostDeliveryState
import com.example.ghostcart.data.ghostDeliverySnapshot
import com.example.ghostcart.data.AVATAR_PRESETS
import com.example.ghostcart.data.avatarPresetById
import com.example.ghostcart.data.defaultAvatarPresetIdForGender
import com.example.ghostcart.data.ProgressSummary
import com.example.ghostcart.data.ListingProductStub
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.data.ProductImportState
import com.example.ghostcart.data.WalletConfig
import com.example.ghostcart.data.TutorialStep
import com.example.ghostcart.data.progressSummary
import com.example.ghostcart.theme.DangerRed
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
import com.example.ghostcart.ui.common.CoolingOption
import com.example.ghostcart.ui.common.CoolingDurationDialog
import com.example.ghostcart.ui.common.DEFAULT_GHOST_COOLDOWN_MILLIS
import com.example.ghostcart.ui.common.coolingOptions
import com.example.ghostcart.ui.common.GhostHeroCard
import com.example.ghostcart.ui.common.GhostTopBar
import com.example.ghostcart.ui.common.PrimaryButton
import com.example.ghostcart.ui.common.SecondaryButton
import com.example.ghostcart.ui.common.SimulationBadge
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

private val categories = listOf("Food & drinks", "Fashion", "Beauty", "Electronics", "Home", "Gaming", "Music", "Other")
private val triggers = listOf("Boredom", "Stress", "FOMO", "Hunger", "Reward", "Late-night scrolling", "Other")

@Composable
fun GhostHomeScreen(
    items: List<AlmostBuy>,
    unifiedProducts: List<MarketplaceProduct>,
    favoriteProducts: List<MarketplaceProduct>,
    favoriteProductIds: Set<String>,
    communityProductsLoading: Boolean,
    onGhostSomething: () -> Unit,
    onOpenCooldowns: () -> Unit,
    onTrackDelivery: (String) -> Unit,
    onOpenProgress: () -> Unit,
    onGhost: (String) -> Unit,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShareProduct: (MarketplaceProduct) -> Unit,
    onNotifications: () -> Unit,
    hasUnreadNotifications: Boolean = false,
    onViewAllCatalog: (String) -> Unit,
    onViewAllFavorites: () -> Unit,
    onNotificationsGranted: () -> Unit = {},
    onRefresh: () -> Unit = {},
    homeBanners: List<com.example.ghostcart.data.ContentBlockItem> = emptyList(),
    ghostCartStories: List<com.example.ghostcart.data.ContentBlockItem> = emptyList(),
    onOpenLeaderboard: () -> Unit = {},
    onOpenStory: (Int) -> Unit = {},
    tutorialProductId: String? = null,
    tutorialSpotlightStep: Int = 0,
    onTutorialAdvance: () -> Unit = {},
    onTutorialGhost: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary = items.progressSummary()
    val active = items.filter { it.status == AlmostBuyStatus.COOLING }.sortedBy { it.coolingUntilMillis }

    PullToRefreshBox(
        isRefreshing = communityProductsLoading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Paper),
        // Bottom padding sized to clear the floating nav pill so the end of scroll
        // rests above it, not behind it (content can still pass behind it mid-scroll).
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp, 20.dp, 0.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ProductDiscoverySection(
                unifiedProducts = unifiedProducts,
                favoriteProducts = favoriteProducts,
                favoriteProductIds = favoriteProductIds,
                communityProductsLoading = communityProductsLoading,
                onGhost = onGhost,
                onOpen = onOpen,
                onToggleFavorite = onToggleFavorite,
                onShareProduct = onShareProduct,
                onNotifications = onNotifications,
                hasUnreadNotifications = hasUnreadNotifications,
                onViewAllCatalog = onViewAllCatalog,
                onViewAllFavorites = onViewAllFavorites,
                activeDelivery = active.firstOrNull(),
                onTrackDelivery = onTrackDelivery,
                tutorialProductId = tutorialProductId,
                tutorialSpotlightStep = tutorialSpotlightStep,
                onTutorialAdvance = onTutorialAdvance,
                onTutorialGhost = onTutorialGhost,
                homeBanners = homeBanners
            )
        }

        item { GhostCartStoriesSection(stories = ghostCartStories, onOpenStory = onOpenStory) }

        item { CommunityLeaderboardBanner(onClick = onOpenLeaderboard) }

        item {
            GhostHeroCard(containerColor = Color(0xFF161616)) {
                SimulationBadge(text = stringResource(R.string.simulation_only), dark = true)
                Text(
                    stringResource(R.string.home_hero_title),
                    color = Color.White,
                    fontSize = 24.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 18.dp)
                )
                Text(
                    stringResource(R.string.home_hero_body),
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
                )
                PrimaryButton(
                    text = stringResource(R.string.ghost_something),
                    onClick = onGhostSomething,
                    leadingIcon = Icons.Filled.Add,
                    containerColor = GhostGreen,
                    contentColor = Color(0xFF050505)
                )
            }
        }

        item { ProgressStrip(summary = summary, onOpenProgress = onOpenProgress) }

        item {
            Text(
                stringResource(R.string.safety_disclosure),
                color = MutedText,
                fontSize = 8.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    }
}

@Composable
fun CaptureAlmostBuyScreen(
    seed: AlmostBuyDraft? = null,
    importState: ProductImportState = ProductImportState.Idle,
    onImportSharedUrl: (String) -> Unit,
    onBack: () -> Unit,
    onGhostIt: (AlmostBuyDraft) -> Unit,
    onAddListingToCart: (List<ListingProductStub>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var name by remember(seed?.name) { mutableStateOf(seed?.name.orEmpty()) }
    var amount by remember(seed?.amountCents) {
        mutableStateOf(seed?.amountCents?.takeIf { it > 0 }?.let { DecimalFormat("0.00").format(it / 100.0) }.orEmpty())
    }
    var sourceUrl by remember(seed?.sourceUrl) { mutableStateOf(seed?.sourceUrl.orEmpty()) }
    var imageUrl by remember(seed?.imageUrl) { mutableStateOf(seed?.imageUrl) }
    var imageLoadFailed by remember(seed?.imageUrl) { mutableStateOf(false) }
    var sourceKind by remember(seed?.sourceKind) { mutableStateOf(seed?.sourceKind ?: "manual") }
    var shareWithCommunity by remember(seed?.sourceUrl) { mutableStateOf(seed?.sourceUrl != null) }
    var readingStage by remember { mutableIntStateOf(0) }
    var category by remember(seed?.category) { mutableStateOf(seed?.category?.takeIf { it in categories } ?: categories.first()) }
    var trigger by remember(seed?.trigger) { mutableStateOf(seed?.trigger?.takeIf { it in triggers } ?: triggers.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    val requestNotifications = rememberNotificationPermissionRequest()
    var selectedListingIndices by remember(importState) {
        val initial = (importState as? ProductImportState.ListingDetected)?.items?.indices?.toSet() ?: emptySet()
        mutableStateOf(initial)
    }

    fun validatedDraft(): AlmostBuyDraft? {
        val numericAmount = amount.toDoubleOrNull()
        return when {
            name.isBlank() -> null.also { error = "Give this item a name." }
            numericAmount == null || numericAmount <= 0 -> null.also { error = "Enter an amount greater than zero." }
            else -> AlmostBuyDraft(
                name = name.trim(),
                amountCents = (numericAmount * 100).toLong(),
                category = category,
                trigger = trigger,
                coolingDurationMillis = DEFAULT_GHOST_COOLDOWN_MILLIS,
                sourceUrl = sourceUrl.trim().takeIf { it.isNotBlank() },
                imageUrl = imageUrl,
                sourceKind = if (sourceUrl.isNotBlank()) "share" else sourceKind,
                shareWithCommunity = shareWithCommunity
            )
        }
    }

    LaunchedEffect(seed?.sourceUrl, seed?.imageUrl) {
        if (seed != null) {
            name = seed.name
            amount = seed.amountCents.takeIf { it > 0 }?.let { DecimalFormat("0.00").format(it / 100.0) }.orEmpty()
            sourceUrl = seed.sourceUrl.orEmpty()
            imageUrl = seed.imageUrl
            imageLoadFailed = false
            sourceKind = seed.sourceKind
            category = seed.category.takeIf { it in categories } ?: "Other"
            trigger = seed.trigger.takeIf { it in triggers } ?: triggers.first()
        }
    }

    LaunchedEffect(importState) {
        readingStage = 0
        while (importState is ProductImportState.Loading) {
            delay(750)
            readingStage = (readingStage + 1) % 4
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Paper),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp, 16.dp, 0.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { GhostTopBar(title = stringResource(R.string.ghost_an_almost_buy), onBack = onBack) }
        item {
            Column {
                Text(stringResource(R.string.capture_headline), color = Ink, fontSize = 30.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold)
                Text("Share a product from any shopping app or paste its link. Everything stays editable.", color = MutedText, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftGray), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Import a product link", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("In any shopping app, tap Share and choose Ghost Cart - or paste the public product link here.", color = MutedText, fontSize = 11.sp)
                    OutlinedTextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it.take(2048); error = null },
                        label = { Text("Product link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ghostTextFieldColors()
                    )
                    OutlinedButton(
                        onClick = { if (sourceUrl.isNotBlank()) onImportSharedUrl(sourceUrl.trim()) else error = "Paste a public HTTPS product link." },
                        enabled = importState !is ProductImportState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (importState is ProductImportState.Loading) {
                            CircularProgressIndicator(
                                color = GhostGreen,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Reading product...", modifier = Modifier.padding(start = 9.dp))
                        } else {
                            Text("Capture product details")
                        }
                    }
                    if (importState is ProductImportState.Loading) {
                        val readingMessages = listOf(
                            "Opening the product page",
                            "Finding the title and picture",
                            "Checking the price",
                            "Preparing your Ghost Cart preview"
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(GreenTint)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GhostMascotPose("phoneList", Modifier.size(34.dp))
                                Column(modifier = Modifier.padding(start = 10.dp)) {
                                    Text(readingMessages[readingStage], color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Hang tight - we're sorting the useful details.", color = MutedText, fontSize = 9.sp)
                                }
                            }
                            LinearProgressIndicator(
                                color = GhostGreen,
                                trackColor = FaintBorder,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(4.dp).clip(RoundedCornerShape(999.dp))
                            )
                        }
                    }
                    when (importState) {
                        is ProductImportState.Ready -> {
                            val captureMessage = if (importState.product.status == "complete") {
                                "${importState.product.retailer} image, title and price captured. Check them before ghosting."
                            } else {
                                "${importState.product.retailer} shared what it could. Add any missing image or price before ghosting."
                            }
                            Text(
                                captureMessage,
                                color = if (importState.product.status == "complete") GhostGreen else Ink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            importState.product.note?.let { Text(it, color = MutedText, fontSize = 10.sp) }
                        }
                        is ProductImportState.ListingDetected -> Text(
                            "This looks like a ${importState.retailer} listing page. Found ${importState.items.size} products - review and add the ones you want below.",
                            color = GhostGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        is ProductImportState.Error -> Text(importState.message, color = Color(0xFFB42318), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        else -> Unit
                    }
                }
            }
        }
        val listingState = importState as? ProductImportState.ListingDetected
        if (listingState != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${listingState.items.size} products found",
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        selectedListingIndices = if (selectedListingIndices.size == listingState.items.size) {
                            emptySet()
                        } else {
                            listingState.items.indices.toSet()
                        }
                    }) {
                        Text(if (selectedListingIndices.size == listingState.items.size) "Deselect all" else "Select all")
                    }
                }
            }
            items(listingState.items.size) { index ->
                val stub = listingState.items[index]
                val checked = index in selectedListingIndices
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoftGray)
                        .clickable {
                            selectedListingIndices = if (checked) selectedListingIndices - index else selectedListingIndices + index
                        }
                        .padding(10.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            selectedListingIndices = if (it) selectedListingIndices + index else selectedListingIndices - index
                        },
                        colors = CheckboxDefaults.colors(checkedColor = GhostGreen, checkmarkColor = Ink)
                    )
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!stub.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = stub.imageUrl,
                                contentDescription = stub.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(4.dp)
                            )
                        } else {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = MutedText, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(stub.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                        if (stub.priceCents != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                                DirhamGlyph(tint = MutedText, modifier = Modifier.size(11.dp))
                                Text(
                                    "%,.2f".format(java.util.Locale.US, stub.priceCents / 100.0),
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 3.dp)
                                )
                            }
                        } else {
                            Text("Price not captured", color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }
            item {
                PrimaryButton(
                    text = if (selectedListingIndices.isEmpty()) "Select products to add" else "Add ${selectedListingIndices.size} to Ghost Cart",
                    onClick = {
                        onAddListingToCart(selectedListingIndices.sorted().map { listingState.items[it] })
                    },
                    leadingIcon = Icons.Filled.ShoppingBag,
                    containerColor = Ink
                )
            }
        } else {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (!imageUrl.isNullOrBlank() && !imageLoadFailed) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = if (name.isBlank()) "Imported product image" else "$name product image",
                            contentScale = ContentScale.Fit,
                            onError = { imageLoadFailed = true },
                            onSuccess = { imageLoadFailed = false },
                            modifier = Modifier.fillMaxSize().padding(12.dp)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = MutedText, modifier = Modifier.size(34.dp))
                            Text("Product image not captured yet", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Paste an image URL below or continue without one.", color = MutedText, fontSize = 10.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = imageUrl.orEmpty(),
                    onValueChange = {
                        imageUrl = it.trim().take(2048).takeIf(String::isNotBlank)
                        imageLoadFailed = false
                    },
                    label = { Text("Product image URL (optional)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ghostTextFieldColors()
                )
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(160); error = null },
                label = { Text(stringResource(R.string.item_name)) },
                placeholder = { Text(stringResource(R.string.item_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = ghostTextFieldColors()
            )
        }
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { value -> amount = value.filter { it.isDigit() || it == '.' }.take(10); error = null },
                label = { Text(stringResource(R.string.amount_in_dirhams)) },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = ghostTextFieldColors()
            )
        }
        item { ChoiceSection(stringResource(R.string.category), categories, category) { category = it } }
        item { ChoiceSection(stringResource(R.string.what_triggered_this), triggers, trigger) { trigger = it } }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GreenTint).padding(14.dp)
            ) {
                Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = GhostGreen)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Ghost Cart first", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Add it to your Ghost Cart now. Choose Ghost Delivery time at checkout.", color = MutedText, fontSize = 10.sp)
                }
            }
        }
        if (sourceUrl.isNotBlank()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GreenTint).padding(14.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Show this as a User Ghosted item", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Optional and anonymous. Your name, profile and source link are never shown.", color = MutedText, fontSize = 10.sp)
                    }
                    Switch(
                        checked = shareWithCommunity,
                        onCheckedChange = { shareWithCommunity = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = GhostGreen, checkedThumbColor = Ink)
                    )
                }
            }
        }
        if (error != null) item { Text(error.orEmpty(), color = Color(0xFFB42318), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        item {
            PrimaryButton(
                text = "Ghost it",
                onClick = {
                    validatedDraft()?.let {
                        onGhostIt(it)
                    }
                },
                leadingIcon = Icons.Filled.ShoppingBag,
                containerColor = GhostGreen,
                contentColor = Color(0xFF050505)
            )
        }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MutedText, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.capture_disclosure), color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
@Composable
fun CooldownsScreen(
    almostBuys: List<AlmostBuy>,
    onGhostSomething: () -> Unit,
    onResolve: (String, AlmostBuyResolution) -> Unit,
    onMoreTime: (String, Long) -> Unit,
    onShare: (AlmostBuy) -> Unit,
    onOpenSource: (String) -> Unit,
    onTrack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val active = almostBuys.filter { it.status == AlmostBuyStatus.COOLING }.sortedBy { it.coolingUntilMillis }
    val activeGroups = active.groupBy { it.ghostOrderId ?: it.id }
        .toList()
        .sortedBy { (_, groupItems) -> groupItems.minOf { it.coolingUntilMillis } }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Paper),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp, 20.dp, 0.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ghost Orders", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Track active Ghost Deliveries and decide when they arrive.", color = MutedText, fontSize = 12.sp)
                }
                AssistChip(onClick = onGhostSomething, label = { Text(stringResource(R.string.add)) }, leadingIcon = { Icon(Icons.Filled.Add, null) })
            }
        }

        if (active.isEmpty()) {
            item {
                EmptyPanel(
                    title = stringResource(R.string.nothing_cooling),
                    body = stringResource(R.string.nothing_cooling_body),
                    action = stringResource(R.string.ghost_something),
                    onAction = onGhostSomething
                )
            }
        } else {
            item { SectionHeader("Active Ghost Deliveries") }
            activeGroups.forEach { (groupId, groupItems) ->
                if (groupItems.size > 1) {
                    item(key = "active_group_$groupId") {
                        GhostOrderGroupHeader(
                            groupItems = groupItems,
                            now = now,
                            resolved = false
                        )
                    }
                }
                items(groupItems, key = { "active_${it.id}" }) { item ->
                    CooldownDecisionCard(item, now, onResolve, onMoreTime, onShare, onOpenSource, onTrack)
                }
            }
        }

        val resolved = almostBuys.filter { it.status != AlmostBuyStatus.COOLING }
            .sortedByDescending { it.resolvedAtMillis ?: it.createdAtMillis }
        if (resolved.isNotEmpty()) {
            item { SectionHeader("Past Ghost Orders") }
            resolved.take(20).groupBy { it.ghostOrderId ?: it.id }.forEach { (groupId, groupItems) ->
                if (groupItems.size > 1) {
                    item(key = "past_group_$groupId") {
                        GhostOrderGroupHeader(groupItems = groupItems, now = now, resolved = true)
                    }
                }
                items(groupItems, key = { "past_${it.id}" }) { item ->
                    ResolvedRow(item, onShare, onOpenSource)
                }
            }
        }
    }
}

@Composable
fun ProgressScreen(
    almostBuys: List<AlmostBuy>,
    config: WalletConfig,
    onSetCardholderName: (String) -> Unit,
    onSelectCardTheme: (String) -> Unit,
    onDownloadCard: () -> Unit,
    onAddBalance: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var editName by remember { mutableStateOf(false) }
    var addBalance by remember { mutableStateOf(false) }
    val summary = almostBuys.progressSummary()
    val keptItems = almostBuys.filter { it.status == AlmostBuyStatus.SKIPPED && !it.tutorialOnly }
    val categoriesByKept = keptItems.groupBy { it.category }
        .mapValues { (_, values) -> values.sumOf { it.amountCents } }
        .toList().sortedByDescending { it.second }
    val triggersByCount = almostBuys.groupingBy { it.trigger }.eachCount().toList().sortedByDescending { it.second }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Paper),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp, 20.dp, 0.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Ghost Wallet", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Your simulated balance, membership card and decision progress, together.", color = MutedText, fontSize = 12.sp)
        }
        item {
            GhostHeroCard {
                Text("SIMULATED BALANCE", color = Paper.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                DirhamAmount(formatDirhams(config.startingBalance.toLong() * 100), tint = GhostGreen, fontSize = 35.sp, glyphSize = 26.dp)
                Text("Internal Ghost Cart credit only. No real money is stored.", color = Paper.copy(alpha = 0.65f), fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                PrimaryButton(
                    text = "Add simulated balance",
                    onClick = { addBalance = true },
                    leadingIcon = Icons.Filled.Add,
                    containerColor = GhostGreen,
                    contentColor = Color(0xFF050505)
                )
            }
        }
        item { SectionHeader("Ghost Card") }
        item { MembershipCard(config) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { editName = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Edit, null)
                    Text(stringResource(R.string.name_on_card), modifier = Modifier.padding(start = 7.dp))
                }
                Button(
                    onClick = onDownloadCard,
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Download, null)
                    Text(stringResource(R.string.download_png), modifier = Modifier.padding(start = 7.dp))
                }
            }
        }
        item {
            Column {
                Text(stringResource(R.string.card_theme), color = Ink, fontWeight = FontWeight.ExtraBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(listOf("Dark", "Light", "Ghost Green")) { theme ->
                        FilterChip(
                            selected = config.cardTheme == theme,
                            onClick = { onSelectCardTheme(theme) },
                            label = { Text(theme) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenTint)
                        )
                    }
                }
            }
        }
        item {
            GhostHeroCard {
                Text(stringResource(R.string.confirmed_money_kept), color = Paper.copy(alpha = 0.65f), fontSize = 12.sp)
                DirhamAmount(formatDirhams(summary.confirmedMoneyKeptCents), tint = GhostGreen, fontSize = 35.sp, glyphSize = 26.dp)
                Text(stringResource(R.string.only_skipped_counts), color = Paper.copy(alpha = 0.65f), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            Text(
                "Money Kept is the listed value of items you confirmed you skipped. It is not cash or a bank balance.",
                color = MutedText,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoftGray)
                    .padding(14.dp)
            )
        }
        item { SectionHeader("Your progress") }
        item { ProgressStrip(summary) }

        if (almostBuys.isEmpty()) {
            item {
                EmptyPanel(
                    title = stringResource(R.string.no_progress_yet),
                    body = stringResource(R.string.no_progress_body)
                )
            }
        } else {
            item { SectionHeader(stringResource(R.string.money_kept_by_category)) }
            if (categoriesByKept.isEmpty()) {
                item { Text(stringResource(R.string.resolve_to_build_insights), color = MutedText, fontSize = 12.sp) }
            } else {
                items(categoriesByKept) { (category, amount) -> BreakdownRow(category, formatDirhams(amount), money = true) }
            }

            item { SectionHeader(stringResource(R.string.your_common_triggers)) }
            items(triggersByCount.take(5)) { (trigger, count) -> BreakdownRow(trigger, "$count") }
        }
        item {
            Text(
                stringResource(R.string.progress_disclosure),
                color = MutedText,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
        }
    }

    if (editName) {
        var draft by remember(config.cardholderName) { mutableStateOf(config.cardholderName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text(stringResource(R.string.name_on_membership_card)) },
            text = { OutlinedTextField(value = draft, onValueChange = { draft = it.take(28) }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (draft.isNotBlank()) onSetCardholderName(draft.trim())
                    editName = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (addBalance) {
        var draft by remember { mutableStateOf("10000") }
        AlertDialog(
            onDismissRequest = { addBalance = false },
            title = { Text("Add simulated balance") },
            text = {
                Column {
                    Text("This adds internal Ghost Cart credit. No real money is deposited.", color = MutedText, fontSize = 11.sp)
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.filter(Char::isDigit).take(7) },
                        label = { Text("Amount in dirhams") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    draft.toIntOrNull()?.takeIf { it > 0 }?.let(onAddBalance)
                    addBalance = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addBalance = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/**
 * A simple static entry point to the standalone Leaderboard page - the user
 * asked to keep this to "just a banner", not a new bottom-nav tab or a full
 * admin-managed content-block type.
 */
@Composable
private fun CommunityLeaderboardBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Ink)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🏆", fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Community Leaderboard", color = Paper, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text("See who's kept the most money this month", color = Paper.copy(alpha = 0.65f), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text("View →", color = GhostGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/**
 * Display name, avatar, and the opt-in Community Leaderboard toggle. Opting
 * in requires a username (validated server-side: format, reserved names,
 * a blocklist, uniqueness); opting out hides the user from the leaderboard
 * immediately without losing the username, in case they opt back in later.
 * Completely separate from - and never weakens - the existing anonymous
 * community-products feed's anonymity guarantee.
 */
@Composable
private fun ProfileCommunitySection(
    profile: com.example.ghostcart.data.UserProfile?,
    saving: Boolean,
    error: String?,
    onSaveDisplayName: (String) -> Unit,
    onUploadAvatar: (ByteArray, String) -> Unit,
    onSelectAvatarPreset: (String) -> Unit,
    onSetCommunityOptIn: (username: String?, consent: Boolean) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onSetShowRecentActivityPublicly: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
    var usernameDraft by remember(profile?.username) { mutableStateOf(profile?.username ?: "") }
    var showAvatarPresetPicker by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri)?.takeIf { it == "image/png" || it == "image/jpeg" } ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes != null) onUploadAvatar(bytes, mimeType)
    }

    // An explicitly-picked preset always wins; otherwise an uploaded photo
    // wins (the more deliberate, more recent action); otherwise fall back to
    // whichever mascot matches the onboarding Male/Female pick.
    val explicitPreset = avatarPresetById(profile?.avatarPresetId)
    val effectivePreset = explicitPreset
        ?: if (profile?.avatarUrl != null) null else avatarPresetById(defaultAvatarPresetIdForGender(profile?.gender))

    if (showAvatarPresetPicker) {
        AvatarPresetPickerDialog(
            selectedPresetId = explicitPreset?.id,
            onSelect = { presetId ->
                onSelectAvatarPreset(presetId)
                showAvatarPresetPicker = false
            },
            onDismiss = { showAvatarPresetPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Paper)
            .border(1.dp, FaintBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Ink)
                    .clickable { avatarPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                when {
                    effectivePreset != null -> Image(
                        painter = painterResource(effectivePreset.drawableRes),
                        contentDescription = "Your avatar",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(6.dp)
                    )
                    profile?.avatarUrl != null -> AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = "Your avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                    else -> Text((displayName.ifBlank { "?" }).take(1).uppercase(), color = Paper, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Tap photo to upload your own", color = MutedText, fontSize = 10.sp)
                TextButton(
                    onClick = { showAvatarPresetPicker = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = GhostGreen)
                ) { Text("Choose a Ghost avatar", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Text("Shown on the Community Leaderboard if you opt in.", color = MutedText, fontSize = 9.sp)
            }
        }

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display name") },
            singleLine = true,
            colors = ghostTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(
            onClick = { onSaveDisplayName(displayName) },
            enabled = !saving && displayName != (profile?.displayName ?: ""),
            colors = ButtonDefaults.textButtonColors(contentColor = GhostGreen, disabledContentColor = MutedText)
        ) { Text(if (saving) "Saving…" else "Save name") }

        HorizontalDivider(color = FaintBorder)

        Text("Community Leaderboard", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Text(
            "Opt in to show a username on the public leaderboard (ranked by money kept). Your email is never shown.",
            color = MutedText,
            fontSize = 10.sp
        )

        var editingUsername by remember { mutableStateOf(false) }
        if (profile?.communityConsent == true && !editingUsername) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("You're on the leaderboard as @${profile.username}", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onOpenLeaderboard, colors = ButtonDefaults.textButtonColors(contentColor = GhostGreen)) { Text("View") }
                TextButton(
                    onClick = { usernameDraft = profile.username ?: ""; editingUsername = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = GhostGreen)
                ) { Text("Edit") }
                TextButton(
                    onClick = { onSetCommunityOptIn(null, false) },
                    enabled = !saving,
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed, disabledContentColor = MutedText)
                ) { Text("Opt out") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show recent activity to other members", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Lets others see your recent ghosted items and activity feed when they open your leaderboard entry. Off by default.",
                        color = MutedText,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = profile.showRecentActivityPublicly,
                    onCheckedChange = onSetShowRecentActivityPublicly,
                    enabled = !saving,
                    colors = SwitchDefaults.colors(checkedTrackColor = GhostGreen, checkedThumbColor = Ink)
                )
            }
        } else {
            LaunchedEffect(profile?.username) {
                if (editingUsername && profile?.username == usernameDraft.trim()) editingUsername = false
            }
            OutlinedTextField(
                value = usernameDraft,
                onValueChange = { usernameDraft = it },
                label = { Text("Username") },
                placeholder = { Text("e.g. ghost_saver_22") },
                singleLine = true,
                colors = ghostTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                TextButton(
                    onClick = { onSetCommunityOptIn(usernameDraft.trim(), true) },
                    enabled = !saving && usernameDraft.trim().length >= 3,
                    colors = ButtonDefaults.textButtonColors(contentColor = GhostGreen, disabledContentColor = MutedText)
                ) { Text(if (saving) "Saving…" else if (editingUsername) "Save username" else "Join the leaderboard") }
                if (editingUsername) {
                    TextButton(
                        onClick = { editingUsername = false },
                        enabled = !saving,
                        colors = ButtonDefaults.textButtonColors(contentColor = MutedText)
                    ) { Text("Cancel") }
                }
            }
        }

        if (error != null) {
            Text(error, color = DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Grid of every AVATAR_PRESETS entry - adding a preset later (new drawable +
// one line in AvatarPresets.kt) shows up here automatically, no UI change needed.
@Composable
private fun AvatarPresetPickerDialog(
    selectedPresetId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = GhostGreen)) { Text("Done") }
        },
        title = { Text("Choose a Ghost avatar", fontWeight = FontWeight.ExtraBold) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(280.dp)
            ) {
                gridItems(AVATAR_PRESETS, key = { it.id }) { preset ->
                    val selected = preset.id == selectedPresetId
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(GreenTint)
                            .border(if (selected) 2.dp else 1.dp, if (selected) GhostGreen else FaintBorder, CircleShape)
                            .clickable { onSelect(preset.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(preset.drawableRes),
                            contentDescription = preset.id,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ProfileScreen(
    config: WalletConfig,
    authEmail: String?,
    appTheme: String,
    onSelectAppTheme: (String) -> Unit,
    onToggleCooling: () -> Unit,
    onToggleLunch: () -> Unit,
    onToggleDinner: () -> Unit,
    onOpenLegal: (docId: String) -> Unit = {},
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit = {},
    profile: com.example.ghostcart.data.UserProfile? = null,
    profileSaving: Boolean = false,
    profileError: String? = null,
    onSaveDisplayName: (String) -> Unit = {},
    onUploadAvatar: (ByteArray, String) -> Unit = { _, _ -> },
    onSelectAvatarPreset: (String) -> Unit = {},
    onSetCommunityOptIn: (username: String?, consent: Boolean) -> Unit = { _, _ -> },
    onOpenLeaderboard: () -> Unit = {},
    onSetShowRecentActivityPublicly: (Boolean) -> Unit = {},
    onReplayTutorial: () -> Unit = {},
    tutorialDebugState: String = "",
    onResetTutorialDebug: () -> Unit = {},
    onClearTutorialSessionDebug: () -> Unit = {},
    onStartTutorialStepDebug: (TutorialStep) -> Unit = {},
    onOpenGifts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showTutorialDebug by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val requestNotifications = rememberNotificationPermissionRequest()

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Paper),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp, 20.dp, 0.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(stringResource(R.string.profile), color = Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(authEmail ?: stringResource(R.string.guest_profile), color = MutedText, fontSize = 12.sp)
        }
        if (authEmail == null) {
            item {
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GhostGreen, contentColor = Color(0xFF050505)),
                ) {
                    Text(stringResource(R.string.sign_in_sign_up), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (authEmail != null) {
            item {
                ProfileCommunitySection(
                    profile = profile,
                    saving = profileSaving,
                    error = profileError,
                    onSaveDisplayName = onSaveDisplayName,
                    onUploadAvatar = onUploadAvatar,
                    onSelectAvatarPreset = onSelectAvatarPreset,
                    onSetCommunityOptIn = onSetCommunityOptIn,
                    onOpenLeaderboard = onOpenLeaderboard,
                    onSetShowRecentActivityPublicly = onSetShowRecentActivityPublicly
                )
            }
            item {
                Column {
                    SectionHeader("Gifts")
                    LegalRow("Received and sent gifts", onOpenGifts)
                }
            }
        }
        item {
            Column {
                Text("App appearance", color = Ink, fontWeight = FontWeight.ExtraBold)
                Text("Choose light, dark, or follow your phone.", color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(listOf("System", "Light", "Dark")) { theme ->
                        FilterChip(
                            selected = appTheme == theme,
                            onClick = { onSelectAppTheme(theme) },
                            label = { Text(theme) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenTint)
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Learn Ghost Cart", color = Ink, fontWeight = FontWeight.ExtraBold)
                Text("Return to the practice flow whenever you want.", color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp, bottom = 4.dp))
                LegalRow("Replay app tutorial", onReplayTutorial)
            }
        }
        if (BuildConfig.DEBUG) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SoftGray).padding(14.dp)
                ) {
                    Text("Tutorial debug tools", color = Ink, fontWeight = FontWeight.ExtraBold)
                    Text(tutorialDebugState, color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        OutlinedButton(onClick = onResetTutorialDebug, modifier = Modifier.weight(1f)) { Text("Reset") }
                        OutlinedButton(onClick = onClearTutorialSessionDebug, modifier = Modifier.weight(1f)) { Text("Clear session") }
                    }
                    OutlinedButton(onClick = { showTutorialDebug = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Start at selected step")
                    }
                }
            }
        }
        item { SectionHeader(stringResource(R.string.reminders)) }
        item {
            LegalRow("Manage notification categories") {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                )
            }
        }
        item {
            NotificationPreferenceRow(
                title = stringResource(R.string.cooling_complete_notifications),
                body = stringResource(R.string.cooling_complete_notifications_body),
                checked = config.coolingNotificationsEnabled,
                onToggle = {
                    if (!config.coolingNotificationsEnabled) requestNotifications()
                    onToggleCooling()
                }
            )
        }
        item {
            NotificationPreferenceRow(
                title = stringResource(R.string.lunch_reminder),
                body = stringResource(R.string.lunch_reminder_body),
                checked = config.lunchReminderEnabled,
                onToggle = {
                    if (!config.lunchReminderEnabled) requestNotifications()
                    onToggleLunch()
                }
            )
        }
        item {
            NotificationPreferenceRow(
                title = stringResource(R.string.dinner_reminder),
                body = stringResource(R.string.dinner_reminder_body),
                checked = config.dinnerReminderEnabled,
                onToggle = {
                    if (!config.dinnerReminderEnabled) requestNotifications()
                    onToggleDinner()
                }
            )
        }
        item { SectionHeader("Legal") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                LegalRow("Privacy Policy") { onOpenLegal("privacy") }
                LegalRow("Terms & Conditions") { onOpenLegal("terms") }
                LegalRow("Data Security") { onOpenLegal("data-security") }
            }
        }
        item {
            Text(
                stringResource(R.string.card_disclosure),
                color = MutedText,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            )
        }
        if (authEmail != null) {
            item { OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sign_out)) } }
        }
        item {
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete account", color = com.example.ghostcart.theme.DangerRed)
            }
        }
        item {
            // Shown until Play Store release, so beta testers can tell Claude/support
            // exactly which build a bug report came from without digging through Settings.
            Text(
                "Ghost Cart v${BuildConfig.VERSION_NAME}",
                color = MutedText,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Ghost Cart account?") },
            text = { Text("This permanently removes this device's profile, favorites, cooldowns, simulated wallet and activity history.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteAccount()
                }) { Text("Delete", color = com.example.ghostcart.theme.DangerRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (BuildConfig.DEBUG && showTutorialDebug) {
        AlertDialog(
            onDismissRequest = { showTutorialDebug = false },
            title = { Text("Start tutorial at step") },
            text = {
                LazyColumn(modifier = Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(TutorialStep.entries) { step ->
                        TextButton(
                            onClick = {
                                showTutorialDebug = false
                                onStartTutorialStepDebug(step)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(step.name.replace('_', ' '), modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTutorialDebug = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun LegalRow(title: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedText, modifier = Modifier.size(20.dp))
    }
}

private val LEGAL_DOCUMENT_TITLES = mapOf(
    "privacy" to "Privacy Policy",
    "terms" to "Terms & Conditions",
    "data-security" to "Data Security",
)

// Last-updated stamp shown on every legal doc so a support conversation can
// reference exactly which version a user saw. Bump this whenever any of the
// three bodies below change materially.
private const val LEGAL_DOCUMENTS_LAST_UPDATED = "23 July 2026"

private val LEGAL_DOCUMENT_BODIES = mapOf(
    "privacy" to """
        Ghost Cart ("we", "us", "our") respects your privacy. This policy explains what information we collect, why, and how it is used, consistent with UAE Federal Decree-Law No. 45 of 2021 on the Protection of Personal Data ("PDPL").

        What we collect
        • Account information: your email address and a securely hashed password - we never store your actual password.
        • Profile information you choose to provide: display name, leaderboard username, and profile photo.
        • Simulated activity: items you capture, cool off, or resolve in the app (Ghost Cart items, cooldowns, "Money Kept" records). None of this reflects a real purchase, payment, or bank transaction.
        • Device and usage information: push-notification device tokens, and aggregate analytics via Firebase Analytics (Android) and Google Analytics (website) - feature usage and crash diagnostics only.
        • Anonymous community content: if you opt in, your leaderboard username, avatar, and activity counts (never your email or real name) become visible to other users. You can withdraw this consent at any time from Profile.

        Why we use it
        To operate and secure the app and website, personalize your experience, respond to support requests, and improve the product. We do not sell your personal data.

        Who we share it with
        We use trusted service providers to run Ghost Cart: Cloudflare (hosting, database, and email delivery) and Google Firebase / Google Analytics (sign-in, push notifications, analytics). These providers process data on our behalf and may store it outside the UAE; we take reasonable steps to ensure they provide an adequate level of protection.

        Your rights
        Under the PDPL you may access, correct, or request deletion of your personal data. Delete your account and all associated data at any time from Profile → Delete Account. For any other request, contact info@theghostcart.com.

        Data retention
        We retain your data while your account is active, or as needed to meet legal obligations. Deleting your account permanently removes your profile, cooldown history, favorites, and simulated wallet data.

        Children's privacy
        Ghost Cart is not directed at, and should not be used by, anyone under 18.

        Changes to this policy
        We may update this policy from time to time; continued use of the app after a change means you accept the update.

        Contact: info@theghostcart.com
    """,
    "terms" to """
        By using the Ghost Cart app (the "App") or theghostcart.com (the "Site"), you agree to these Terms & Conditions, governed by the laws of the United Arab Emirates.

        1. What Ghost Cart is
        Ghost Cart is a simulation-only cooling-off tool that helps you pause before an impulse purchase. Every "purchase," "checkout," "delivery," and "payment" shown in the App is simulated for behavioral/educational purposes only. No real money moves through the App, no real goods are ordered or delivered, and Ghost Cart is not a bank, e-wallet, payment service provider, or licensed financial institution. The "Ghost Card" is a non-financial achievement/membership card only - it is not a payment card and carries no CVV, expiry date, or real card number.

        2. Eligibility
        You must be at least 18 years old to create an account.

        3. Your account
        You are responsible for keeping your login credentials secure and for all activity under your account. Notify us immediately at info@theghostcart.com if you suspect unauthorized access.

        4. Community features
        The Community Leaderboard and community product feed let opted-in users share a chosen username, avatar, and activity - never your email or real name. Do not impersonate others or post unlawful, defamatory, or offensive content. We may remove content or suspend accounts that violate this, consistent with UAE Federal Decree-Law No. 34 of 2021 on Combating Rumours and Cybercrimes.

        5. No real transactions
        Nothing in the App constitutes an offer to sell, a real order, a real payment instrument, or a real delivery service. "Money Kept" and similar figures are personal tracking metrics only - not a financial product, investment, or guarantee of savings.

        6. Intellectual property
        The Ghost Cart name, logo, mascot, and app/site design belong to us. Do not copy, reproduce, or reuse them without permission.

        7. Termination
        We may suspend or terminate accounts that violate these Terms. You may delete your own account at any time from Profile.

        8. Disclaimer and limitation of liability
        The App and Site are provided "as is," without warranties of any kind. To the fullest extent permitted by UAE law, we are not liable for indirect, incidental, or consequential damages arising from your use of the App or Site.

        9. Governing law
        These Terms are governed by the laws of the United Arab Emirates. Any dispute is subject to the exclusive jurisdiction of the competent courts of the UAE.

        10. Contact: info@theghostcart.com
    """,
    "data-security" to """
        We take reasonable technical and organizational measures to protect your data, consistent with UAE Federal Decree-Law No. 45 of 2021 on the Protection of Personal Data.

        • Encryption in transit: all traffic between the App/Site and our servers uses HTTPS/TLS.
        • Password security: passwords are never stored in plain text. We store a salted, one-way cryptographic hash and cannot see or recover your actual password.
        • Session security: sign-in sessions use httpOnly, secure tokens; admin access is separately gated and audited.
        • Data minimization: the anonymous community feed and leaderboard never expose your email, password, or real name to other users.
        • Infrastructure: our backend runs on Cloudflare's Workers/D1 platform; sign-in and push notifications use Google Firebase. These providers maintain their own independent security certifications.
        • Retention and deletion: deleting your account (Profile → Delete Account) permanently removes your stored profile, cooldown history, favorites, and simulated wallet data from our production database.
        • Incident response: if we become aware of a data breach affecting your personal data, we will take reasonable steps to notify affected users and the relevant UAE authority without undue delay, as required by law.

        This page is a good-faith summary of our practices, not an exhaustive security audit. Questions: info@theghostcart.com
    """,
)

/**
 * Legal screens (Privacy Policy, Terms & Conditions, Data Security), reached from Profile.
 */
@Composable
fun LegalDocumentScreen(docId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val title = LEGAL_DOCUMENT_TITLES[docId] ?: "Legal"
    val body = (LEGAL_DOCUMENT_BODIES[docId] ?: "Legal copy for this document has not been supplied yet.")
        .trimIndent()

    Column(modifier = modifier.fillMaxSize().background(Paper)) {
        Column(modifier = Modifier.padding(20.dp)) {
            GhostTopBar(title = title, onBack = onBack)
            Text("Last updated: $LEGAL_DOCUMENTS_LAST_UPDATED", color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp))
        }
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            item {
                Text(body, color = MutedText, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 32.dp))
            }
        }
    }
}

@Composable
private fun ProgressStrip(summary: ProgressSummary, onOpenProgress: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Paper),
        border = androidx.compose.foundation.BorderStroke(1.dp, FaintBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = onOpenProgress != null) { onOpenProgress?.invoke() }
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricColumn(stringResource(R.string.almost_spent), formatDirhams(summary.totalAlmostSpentCents), Modifier.weight(1f))
            MetricColumn(stringResource(R.string.cooling), formatDirhams(summary.activeCoolingCents), Modifier.weight(1f))
            MetricColumn(stringResource(R.string.money_kept), formatDirhams(summary.confirmedMoneyKeptCents), Modifier.weight(1f), GhostGreen)
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Ink) {
    Column(modifier) {
        Text(label, color = MutedText, fontSize = 9.sp)
        DirhamAmount(value, tint = valueColor, fontSize = 14.sp, glyphSize = 11.dp)
    }
}

@Composable
private fun CooldownSummaryCard(item: AlmostBuy, onClick: () -> Unit) {
    var imageLoadFailed by remember(item.imageUrl) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Paper),
        border = androidx.compose.foundation.BorderStroke(1.dp, FaintBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(GreenTint), contentAlignment = Alignment.Center) {
                if (!item.imageUrl.isNullOrBlank() && !imageLoadFailed) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        onError = { imageLoadFailed = true },
                        onSuccess = { imageLoadFailed = false },
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Icon(Icons.Filled.AccessTime, null, tint = GhostGreen)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text("${item.category} · ${item.trigger}", color = MutedText, fontSize = 10.sp)
            }
            DirhamAmount(formatDirhams(item.amountCents), tint = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, glyphSize = 10.dp)
        }
    }
}

@Composable
private fun CooldownDecisionCard(
    item: AlmostBuy,
    now: Long,
    onResolve: (String, AlmostBuyResolution) -> Unit,
    onMoreTime: (String, Long) -> Unit,
    onShare: (AlmostBuy) -> Unit,
    onOpenSource: (String) -> Unit,
    onTrack: (String) -> Unit
) {
    val hasCooled = now >= item.coolingUntilMillis
    val remainingMillis = (item.coolingUntilMillis - now).coerceAtLeast(0L)
    val expectedDuration = item.coolingDurationMillis.coerceAtLeast(60_000L)
    val coolingProgress = if (hasCooled) 1f else {
        (1f - remainingMillis.toFloat() / expectedDuration.toFloat()).coerceIn(0f, 1f)
    }
    val deliverySnapshot = ghostDeliverySnapshot(
        nowMillis = now,
        startMillis = item.deliveryStartedAtMillis ?: item.createdAtMillis,
        endMillis = item.deliveryEndsAtMillis ?: item.coolingUntilMillis,
        persistedState = item.deliveryState
    )
    var imageLoadFailed by remember(item.imageUrl) { mutableStateOf(false) }
    var showRestartDialog by remember(item.id) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = if (hasCooled) GreenTint else SoftGray),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.imageUrl.isNullOrBlank() && !imageLoadFailed) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.name,
                            contentScale = ContentScale.Fit,
                            onError = { imageLoadFailed = true },
                            onSuccess = { imageLoadFailed = false },
                            modifier = Modifier.fillMaxSize().padding(5.dp)
                        )
                    } else {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = MutedText, modifier = Modifier.size(22.dp))
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(item.name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${item.category} · ${item.trigger}", color = MutedText, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    DirhamAmount(formatDirhams(item.amountCents), tint = Ink, fontSize = 15.sp, glyphSize = 12.dp)
                    Row {
                        IconButton(onClick = { onShare(item) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Share, contentDescription = "Share ${item.name}", tint = Ink, modifier = Modifier.size(17.dp))
                        }
                        if (hasCooled) item.sourceUrl?.let { source ->
                            IconButton(onClick = { onOpenSource(source) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = "Open original product", tint = Ink, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
            if (!hasCooled) {
                LinearProgressIndicator(
                    progress = { coolingProgress },
                    color = GhostGreen,
                    trackColor = FaintBorder,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(7.dp).clip(RoundedCornerShape(999.dp))
                )
            }
            if (deliverySnapshot.state != GhostDeliveryState.DELIVERED) {
                Button(
                    onClick = { onTrack(item.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = GhostGreen, contentColor = Color(0xFF050505)),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Text("Track Ghost Delivery", fontWeight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 14.dp)) {
                Icon(if (hasCooled) Icons.Filled.CheckCircle else Icons.Filled.AccessTime, null, tint = if (hasCooled) GhostGreen else MutedText, modifier = Modifier.size(17.dp))
                Text(
                    if (hasCooled) stringResource(R.string.ready_to_decide) else remainingLabel(item.coolingUntilMillis - now),
                    color = if (hasCooled) Ink else MutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 7.dp)
                )
            }
            if (hasCooled) {
                Text("Make a decision", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Button(
                    onClick = { onResolve(item.id, AlmostBuyResolution.SKIPPED) },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Skip the purchase") }
                item.sourceUrl?.let { source ->
                    OutlinedButton(
                        onClick = { onOpenSource(source) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text("Buy it from source", modifier = Modifier.padding(start = 7.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    OutlinedButton(
                        onClick = { onResolve(item.id, AlmostBuyResolution.BOUGHT_INTENTIONALLY) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Bought it already", fontSize = 11.sp) }
                    OutlinedButton(onClick = { showRestartDialog = true }, modifier = Modifier.weight(1f)) {
                        Text("Send it around again", fontSize = 11.sp)
                    }
                }
                OutlinedButton(
                    onClick = { onShare(item) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text("Ask a friend", modifier = Modifier.padding(start = 7.dp))
                }
                Text(stringResource(R.string.resolve_disclosure), color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 10.dp))
            } else {
                Text(
                    "Your decision buttons will unlock when the cooldown ends.",
                    color = MutedText,
                    fontSize = 10.sp
                )
            }
        }
    }

    if (showRestartDialog) {
        CoolingDurationDialog(
            onConfirm = { option ->
                onMoreTime(item.id, option.durationMillis)
                showRestartDialog = false
            },
            onDismiss = { showRestartDialog = false }
        )
    }
}

@Composable
private fun GhostOrderGroupHeader(
    groupItems: List<AlmostBuy>,
    now: Long,
    resolved: Boolean
) {
    val readyCount = groupItems.count { now >= it.coolingUntilMillis }
    val total = groupItems.size
    val totalCents = groupItems.sumOf { it.amountCents }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (resolved) SoftGray else GreenTint)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (resolved) "Past Ghost order" else "Ghost order · $total items",
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                if (resolved) {
                    "Each item keeps its own decision"
                } else if (readyCount > 0) {
                    "$readyCount of $total ready to decide"
                } else {
                    "All items are cooling together"
                },
                color = MutedText,
                fontSize = 10.sp
            )
        }
        DirhamAmount(
            formatDirhams(totalCents),
            tint = Ink,
            fontSize = 12.sp,
            glyphSize = 10.dp
        )
    }
}

@Composable
private fun ResolvedRow(
    item: AlmostBuy,
    onShare: (AlmostBuy) -> Unit,
    onOpenSource: (String) -> Unit
) {
    var imageLoadFailed by remember(item.imageUrl) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (!item.imageUrl.isNullOrBlank() && !imageLoadFailed) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    onError = { imageLoadFailed = true },
                    onSuccess = { imageLoadFailed = false },
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            } else {
                Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = MutedText, modifier = Modifier.size(16.dp))
            }
        }
        Icon(
            if (item.status == AlmostBuyStatus.SKIPPED) Icons.Filled.CheckCircle else Icons.Filled.ShoppingBag,
            contentDescription = null,
            tint = if (item.status == AlmostBuyStatus.SKIPPED) GhostGreen else MutedText,
            modifier = Modifier.size(16.dp).padding(start = 6.dp)
        )
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                text = item.name,
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (item.status == AlmostBuyStatus.SKIPPED) stringResource(R.string.skipped_money_kept) else stringResource(R.string.bought_not_counted),
                color = MutedText,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { onShare(item) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Share, contentDescription = "Share ${item.name}", tint = Ink, modifier = Modifier.size(17.dp))
        }
        item.sourceUrl?.let { source ->
            IconButton(onClick = { onOpenSource(source) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.OpenInNew, contentDescription = "Open original product", tint = Ink, modifier = Modifier.size(17.dp))
            }
        }
        DirhamAmount(formatDirhams(item.amountCents), tint = if (item.status == AlmostBuyStatus.SKIPPED) GhostGreen else Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, glyphSize = 10.dp)
    }
}

@Composable
private fun MembershipCard(config: WalletConfig) {
    val (background, foreground) = when (config.cardTheme) {
        "Light" -> SoftGray to Ink
        "Ghost Green" -> GhostGreen to Ink
        else -> Ink to Paper
    }
    Box(
        modifier = Modifier.fillMaxWidth().height(214.dp).clip(RoundedCornerShape(28.dp)).background(background)
    ) {
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostMascotPose("wave", Modifier.size(34.dp))
                Text("Ghost Membership", color = foreground, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 10.dp))
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.simulation_only), color = foreground.copy(alpha = 0.65f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.member), color = foreground.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(config.cardholderName, color = foreground, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("GHOST ID", color = foreground.copy(alpha = 0.6f), fontSize = 8.sp)
                    Text(config.ghostId, color = foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.member_since), color = foreground.copy(alpha = 0.6f), fontSize = 8.sp)
                    Text(config.memberSince, color = foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(stringResource(R.string.not_a_payment_card), color = foreground.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun NotificationPreferenceRow(title: String, body: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SoftGray).clickable(onClick = onToggle).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (checked) Icons.Filled.Notifications else Icons.Filled.NotificationsOff, null, tint = if (checked) GhostGreen else MutedText)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(body, color = MutedText, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedTrackColor = GhostGreen, checkedThumbColor = Ink)
        )
    }
}

@Composable
private fun ChoiceSection(title: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Column {
        Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(options) { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenTint, selectedLabelColor = Ink)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) Text(action, color = GhostGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onAction).padding(8.dp))
    }
}

@Composable
private fun EmptyPanel(title: String, body: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(SoftGray).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.History, null, tint = MutedText, modifier = Modifier.size(30.dp))
        Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        Text(body, color = MutedText, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
        if (action != null && onAction != null) {
            AssistChip(onClick = onAction, label = { Text(action) }, modifier = Modifier.padding(top = 12.dp), colors = AssistChipDefaults.assistChipColors(containerColor = Ink, labelColor = Paper))
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, money: Boolean = false) {
    Row(Modifier.fillMaxWidth().border(1.dp, FaintBorder, RoundedCornerShape(16.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (money) {
            DirhamAmount(value, tint = GhostGreen, fontSize = 13.sp, glyphSize = 11.dp)
        } else {
            Text(value, color = GhostGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ghostTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    disabledTextColor = Ink,
    cursorColor = GhostGreen,
    focusedLabelColor = Ink,
    unfocusedLabelColor = Ink,
    disabledLabelColor = MutedText,
    focusedPlaceholderColor = MutedText,
    unfocusedPlaceholderColor = MutedText,
    disabledPlaceholderColor = MutedText,
    focusedBorderColor = GhostGreen,
    unfocusedBorderColor = Color(0xFF777772),
    disabledBorderColor = Color(0xFF9A9A95)
)

@Composable
fun rememberNotificationPermissionRequest(onGranted: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted()
    }
    return remember(context, launcher) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

private fun remainingLabel(remainingMillis: Long): String {
    val safe = remainingMillis.coerceAtLeast(0)
    val days = TimeUnit.MILLISECONDS.toDays(safe)
    val hours = TimeUnit.MILLISECONDS.toHours(safe) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(safe) % 60
    return when {
        days > 0 -> "$days d $hours h remaining"
        hours > 0 -> "$hours h $minutes min remaining"
        else -> "${minutes.coerceAtLeast(1)} min remaining"
    }
}

private fun formatDirhams(amountCents: Long): String =
    DecimalFormat("#,##0.00").format(amountCents / 100.0)
