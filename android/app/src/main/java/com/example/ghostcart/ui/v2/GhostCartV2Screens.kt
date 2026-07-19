package com.example.ghostcart.ui.v2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ghostcart.app.BuildConfig
import com.ghostcart.app.R
import com.example.ghostcart.data.AlmostBuy
import com.example.ghostcart.data.AlmostBuyDraft
import com.example.ghostcart.data.AlmostBuyResolution
import com.example.ghostcart.data.AlmostBuyStatus
import com.example.ghostcart.data.ProgressSummary
import com.example.ghostcart.data.ListingProductStub
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.data.ProductImportState
import com.example.ghostcart.data.WalletConfig
import com.example.ghostcart.data.progressSummary
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
    onOpenProgress: () -> Unit,
    onGhost: (String) -> Unit,
    onCool: (id: String, durationMillis: Long, durationLabel: String) -> Unit,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onNotifications: () -> Unit,
    onViewAllCatalog: (String) -> Unit,
    onViewAllFavorites: () -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary = items.progressSummary()
    val active = items.filter { it.status == AlmostBuyStatus.COOLING }.sortedBy { it.coolingUntilMillis }

    // Ask for notification permission once, the first time the user reaches Home (covers both
    // the guest and signed-up onboarding paths), rather than only when a specific reminder
    // toggle is switched on - otherwise a user who never visits Profile never gets asked at all.
    val context = LocalContext.current
    val requestNotificationsOnFirstRun = rememberNotificationPermissionRequest()
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("ghost_cart_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notification_permission_prompted", false)) {
            requestNotificationsOnFirstRun()
            prefs.edit().putBoolean("notification_permission_prompted", true).apply()
        }
    }

    PullToRefreshBox(
        isRefreshing = communityProductsLoading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Paper),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 20.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ProductDiscoverySection(
                unifiedProducts = unifiedProducts,
                favoriteProducts = favoriteProducts,
                favoriteProductIds = favoriteProductIds,
                communityProductsLoading = communityProductsLoading,
                onGhost = onGhost,
                onCool = onCool,
                onOpen = onOpen,
                onToggleFavorite = onToggleFavorite,
                onNotifications = onNotifications,
                onViewAllCatalog = onViewAllCatalog,
                onViewAllFavorites = onViewAllFavorites
            )
        }

        item { GhostCartStoriesSection() }

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
            SectionHeader(
                title = stringResource(R.string.active_cooldowns),
                action = if (active.isEmpty()) null else stringResource(R.string.view_all),
                onAction = onOpenCooldowns
            )
        }

        if (active.isEmpty()) {
            item {
                EmptyPanel(
                    title = stringResource(R.string.no_active_cooldowns),
                    body = stringResource(R.string.no_active_cooldowns_body),
                    action = stringResource(R.string.add_almost_buy),
                    onAction = onGhostSomething
                )
            }
        } else {
            items(active.take(3), key = { it.id }) { item ->
                CooldownSummaryCard(item = item, onClick = onOpenCooldowns)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SoftGray),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(stringResource(R.string.add_from_anywhere), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.add_from_anywhere_body), color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp, bottom = 14.dp))
                    OutlinedButton(onClick = onGhostSomething, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.add_manually), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

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
    onAddToCart: (AlmostBuyDraft) -> Unit,
    onCoolIt: (AlmostBuyDraft) -> Unit,
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
    var cooling by remember(seed?.coolingDurationMillis) {
        mutableStateOf(coolingOptions.minByOrNull { kotlin.math.abs(it.durationMillis - (seed?.coolingDurationMillis ?: coolingOptions[1].durationMillis)) } ?: coolingOptions[1])
    }
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
                coolingDurationMillis = cooling.durationMillis,
                sourceUrl = sourceUrl.trim().takeIf { it.isNotBlank() },
                imageUrl = imageUrl,
                sourceKind = if (sourceUrl.isNotBlank()) "share" else sourceKind,
                shareWithCommunity = shareWithCommunity
            )
        }
    }

    LaunchedEffect(category, seed?.coolingDurationMillis) {
        if (seed?.coolingDurationMillis != null) return@LaunchedEffect
        cooling = when (category) {
            "Food & drinks" -> coolingOptions[0]
            "Electronics" -> coolingOptions[2]
            else -> coolingOptions[1]
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 16.dp, 20.dp, 32.dp),
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
            Column {
                Text(stringResource(R.string.cooling_period), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.cooling_period_body), color = MutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(coolingOptions) { option ->
                        FilterChip(
                            selected = option == cooling,
                            onClick = { cooling = option },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenTint, selectedLabelColor = Ink)
                        )
                    }
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
                text = "Add to Ghost Cart",
                onClick = {
                    validatedDraft()?.let(onAddToCart)
                },
                leadingIcon = Icons.Filled.ShoppingBag,
                containerColor = Ink
            )
        }
        item {
            SecondaryButton(
                text = "Cool it instead",
                onClick = {
                    validatedDraft()?.let {
                        requestNotifications()
                        onCoolIt(it)
                    }
                },
                leadingIcon = Icons.Filled.Timer
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
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val active = almostBuys.filter { it.status == AlmostBuyStatus.COOLING }.sortedBy { it.coolingUntilMillis }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Paper),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 20.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cooldowns), color = Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.cooldowns_body), color = MutedText, fontSize = 12.sp)
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
            items(active, key = { it.id }) { item ->
                CooldownDecisionCard(item, now, onResolve, onMoreTime, onShare, onOpenSource)
            }
        }

        val resolved = almostBuys.filter { it.status != AlmostBuyStatus.COOLING }
        if (resolved.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.recent_decisions)) }
            items(resolved.take(8), key = { it.id }) { item ->
                ResolvedRow(item, onShare, onOpenSource)
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
    val keptItems = almostBuys.filter { it.status == AlmostBuyStatus.SKIPPED }
    val categoriesByKept = keptItems.groupBy { it.category }
        .mapValues { (_, values) -> values.sumOf { it.amountCents } }
        .toList().sortedByDescending { it.second }
    val triggersByCount = almostBuys.groupingBy { it.trigger }.eachCount().toList().sortedByDescending { it.second }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Paper),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 20.dp, 20.dp, 32.dp),
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

@Composable
fun ProfileScreen(
    config: WalletConfig,
    authEmail: String?,
    appTheme: String,
    onSelectAppTheme: (String) -> Unit,
    onToggleCooling: () -> Unit,
    onToggleLunch: () -> Unit,
    onToggleDinner: () -> Unit,
    onDebugTestReminder: (meal: String, hourOfDay: Int) -> Unit = { _, _ -> },
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val requestNotifications = rememberNotificationPermissionRequest()

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Paper),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 20.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(stringResource(R.string.profile), color = Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(authEmail ?: stringResource(R.string.guest_profile), color = MutedText, fontSize = 12.sp)
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
        item { SectionHeader(stringResource(R.string.reminders)) }
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
        if (BuildConfig.DEBUG) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        requestNotifications()
                        onDebugTestReminder("lunch", 13)
                    }) {
                        Text("Test lunch reminder (~90s)", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = {
                        requestNotifications()
                        onDebugTestReminder("dinner", 20)
                    }) {
                        Text("Test dinner reminder (~90s)", fontSize = 11.sp)
                    }
                }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Paper),
        border = androidx.compose.foundation.BorderStroke(1.dp, FaintBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(GreenTint), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AccessTime, null, tint = GhostGreen)
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
    onOpenSource: (String) -> Unit
) {
    val hasCooled = now >= item.coolingUntilMillis
    Card(
        colors = CardDefaults.cardColors(containerColor = if (hasCooled) GreenTint else SoftGray),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${item.category} · ${item.trigger}", color = MutedText, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    DirhamAmount(formatDirhams(item.amountCents), tint = Ink, fontSize = 15.sp, glyphSize = 12.dp)
                    Row {
                        IconButton(onClick = { onShare(item) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Share, contentDescription = "Share ${item.name}", tint = Ink, modifier = Modifier.size(17.dp))
                        }
                        item.sourceUrl?.let { source ->
                            IconButton(onClick = { onOpenSource(source) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = "Open original product", tint = Ink, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp, bottom = 14.dp)) {
                Icon(if (hasCooled) Icons.Filled.CheckCircle else Icons.Filled.AccessTime, null, tint = if (hasCooled) GhostGreen else MutedText, modifier = Modifier.size(17.dp))
                Text(
                    if (hasCooled) stringResource(R.string.ready_to_decide) else remainingLabel(item.coolingUntilMillis - now),
                    color = if (hasCooled) Ink else MutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 7.dp)
                )
            }
            Button(
                onClick = { onResolve(item.id, AlmostBuyResolution.SKIPPED) },
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.i_skipped_it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                OutlinedButton(onClick = { onResolve(item.id, AlmostBuyResolution.BOUGHT_INTENTIONALLY) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.bought_intentionally), fontSize = 11.sp)
                }
                OutlinedButton(onClick = { onMoreTime(item.id, TimeUnit.HOURS.toMillis(24)) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.more_time), fontSize = 11.sp)
                }
            }
            Text(stringResource(R.string.resolve_disclosure), color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun ResolvedRow(
    item: AlmostBuy,
    onShare: (AlmostBuy) -> Unit,
    onOpenSource: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (item.status == AlmostBuyStatus.SKIPPED) Icons.Filled.CheckCircle else Icons.Filled.ShoppingBag,
            contentDescription = null,
            tint = if (item.status == AlmostBuyStatus.SKIPPED) GhostGreen else MutedText,
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(item.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                if (item.status == AlmostBuyStatus.SKIPPED) stringResource(R.string.skipped_money_kept) else stringResource(R.string.bought_not_counted),
                color = MutedText,
                fontSize = 10.sp
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
private fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
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
