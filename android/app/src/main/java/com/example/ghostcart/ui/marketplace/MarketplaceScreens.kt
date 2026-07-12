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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.data.Marketplace
import com.example.ghostcart.data.MarketplaceProduct
import com.example.ghostcart.theme.FaintBorder
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GreenTint
import com.example.ghostcart.theme.Ink
import com.example.ghostcart.theme.MutedText
import com.example.ghostcart.theme.Paper
import com.example.ghostcart.theme.SoftGray
import com.example.ghostcart.ui.GhostMascotPose
import com.example.ghostcart.ui.ProductIcon
import com.example.ghostcart.ui.common.BackButton
import com.example.ghostcart.ui.common.ForwardChevron
import com.example.ghostcart.ui.common.PrimaryButton
import com.example.ghostcart.ui.common.RoundIconButton
import com.example.ghostcart.ui.common.SecondaryButton
import com.example.ghostcart.ui.common.SimulationBadge
import com.example.ghostcart.ui.common.materialIconFor

@Composable
fun HomeMarketplaceScreen(
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
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(38.dp))
                Text(
                    text = "Ghost Cart",
                    color = Ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f).padding(start = 10.dp)
                )
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
                    Text(text = "AED 642", color = GhostGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = "found in almost-spending this week.", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                ForwardChevron()
            }

            MarketplaceSectionHeader(title = "Most Ghosted Today", onViewAll = {})
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(Marketplace.mostGhostedToday) { product ->
                    MarketplaceProductCard(product, onClick = { onOpenProduct(product.id) }, onAdd = { onAddToCart(product.id) })
                }
            }

            MarketplaceSectionHeader(title = "Fake Flash Deals", onViewAll = {})
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(Marketplace.fakeFlashDeals) { product ->
                    MarketplaceProductCard(product, onClick = { onOpenProduct(product.id) }, onAdd = { onAddToCart(product.id) })
                }
            }

            MarketplaceSectionHeader(title = "Salary Protection Picks", onViewAll = {})
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Guard your salary" to "shield", "Avoid impulse\npurchases" to "target", "Save more.\nStress less." to "savings", "Build better\nmoney habits" to "chart").forEach { (label, icon) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(materialIconFor(icon), contentDescription = null, tint = GhostGreen, modifier = Modifier.size(18.dp))
                        Text(text = label, color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
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

            MarketplaceSectionHeader(title = "Sponsored Simulations", onViewAll = {})
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp)) {
                items(Marketplace.sponsoredBrands) { brand ->
                    Column(
                        modifier = Modifier
                            .width(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, FaintBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(text = brand.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        Text(text = brand.tagline, color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceSectionHeader(title: String, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(150.dp)
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
            ProductIcon(name = product.iconName, modifier = Modifier.size(42.dp), color = Ink)
            SimulationBadge(modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        Text(text = product.name, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.padding(top = 8.dp))
        Text(text = "${Marketplace.currency} ${product.price}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Ink)
                .clickable(onClick = onAdd)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Add to Ghost Cart", color = Paper, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryBrowseScreen(
    categoryId: String,
    cartItemCount: Int,
    cartTotal: Int,
    savedTotal: Int,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onAddToCart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Fast Food", "Coffee & Drinks", "Healthy")

    Column(modifier = modifier.fillMaxSize().background(Paper).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onBack = onBack)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Ink).padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                GhostMascotPose(poseName = "wave", modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = "${Marketplace.currency} $cartTotal", color = Paper, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$cartItemCount items", color = MutedText, fontSize = 8.sp)
                }
            }
        }

        Text(text = "🍔☕ Food & Coffee Cravings", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp))
        Text(text = "It wasn't hunger. It was boredom with sauce.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

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
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = Ink, modifier = Modifier.size(14.dp))
                }
            }
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
                text = "You save ${Marketplace.currency} $savedTotal.40 by ghosting temptations instead of giving in.",
                color = Ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(Marketplace.foodAndCoffeeCatalog) { product ->
                MarketplaceProductCard(
                    product = product,
                    onClick = { onOpenProduct(product.id) },
                    onAdd = { onAddToCart(product.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ProductDetailScreen(
    product: MarketplaceProduct,
    onBack: () -> Unit,
    onAddToCart: () -> Unit,
    onGhostBuyNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var favorited by remember { mutableStateOf(false) }

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
            ProductIcon(name = product.iconName, modifier = Modifier.size(120.dp), color = Ink)
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
                .padding(14.dp)
        ) {
            Text(text = "⏱", fontSize = 16.sp)
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(text = "24-Hour Cooling Hub", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = "Items in your Ghost Cart cool down here.\nMost users don't buy after 24 hours.", color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
            ForwardChevron()
        }

        SimulationBadge(text = "Product reference for simulation only.\nNo real payment or delivery will happen.", modifier = Modifier.padding(top = 16.dp))
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
