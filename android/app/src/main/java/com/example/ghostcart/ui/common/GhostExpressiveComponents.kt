package com.example.ghostcart.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostcart.theme.ExpressivePrimaryText
import com.example.ghostcart.theme.ExpressiveSecondaryText
import com.example.ghostcart.theme.ExpressiveSurface
import com.example.ghostcart.theme.ExpressiveSurfaceHigh
import com.example.ghostcart.theme.GhostGlass
import com.example.ghostcart.theme.GhostGlassHighlight
import com.example.ghostcart.theme.GhostGreen
import com.example.ghostcart.theme.GhostOnGreen
import com.example.ghostcart.theme.GhostSubtleBorder
import com.example.ghostcart.ui.DirhamGlyph

enum class ProductCardSpotlightTarget { CARD, FAVORITE, SHARE, REVIEWS, GHOST }

@Composable
fun GhostGlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(GhostGlass)
            .border(1.dp, GhostSubtleBorder, shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GhostGlassHighlight)
        )
        content()
    }
}

@Composable
fun GhostIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: androidx.compose.ui.graphics.Color = ExpressivePrimaryText,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = ExpressiveSurfaceHigh,
        contentColor = iconTint,
        tonalElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun GhostSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(22.dp))
        },
        placeholder = { Text(placeholder, maxLines = 1) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = ExpressiveSurfaceHigh,
            unfocusedContainerColor = ExpressiveSurface,
            disabledContainerColor = ExpressiveSurface,
            focusedTextColor = ExpressivePrimaryText,
            unfocusedTextColor = ExpressivePrimaryText,
            focusedPlaceholderColor = ExpressiveSecondaryText,
            unfocusedPlaceholderColor = ExpressiveSecondaryText,
            focusedLeadingIconColor = GhostGreen,
            unfocusedLeadingIconColor = ExpressiveSecondaryText,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        )
    )
}

@Composable
fun GhostCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        if (selected) GhostGreen else ExpressiveSurfaceHigh,
        label = "categoryContainer",
    )
    val foreground by animateColorAsState(
        if (selected) GhostOnGreen else ExpressiveSecondaryText,
        label = "categoryForeground",
    )
    val elevation by animateDpAsState(if (selected) 3.dp else 0.dp, label = "categoryElevation")
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "categoryScale",
    )
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(24.dp),
        color = container,
        contentColor = foreground,
        tonalElevation = elevation,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun GhostSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(ExpressiveSurfaceHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            GhostSegment(label = label, selected = selectedIndex == index) { onSelect(index) }
        }
    }
}

@Composable
private fun RowScope.GhostSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (selected) GhostGreen else Color.Transparent,
        label = "segmentContainer",
    )
    val foreground by animateColorAsState(
        if (selected) GhostOnGreen else ExpressiveSecondaryText,
        label = "segmentForeground",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 600f),
        label = "segmentScale",
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(container)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = foreground, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostActionPill(
    label: String,
    icon: ImageVector,
    active: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue = if (active) GhostGreen else ExpressiveSurfaceHigh,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f),
        label = "actionPillContainer",
    )
    val content by animateColorAsState(
        targetValue = if (active) GhostOnGreen else ExpressivePrimaryText,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f),
        label = "actionPillContent",
    )
    val elevation by animateDpAsState(
        targetValue = if (active) 3.dp else 1.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f),
        label = "actionPillElevation",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        color = container,
        contentColor = content,
        tonalElevation = elevation,
        border = androidx.compose.foundation.BorderStroke(1.dp, GhostSubtleBorder),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun GhostSectionHeader(
    title: String,
    subtitle: String,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = ExpressivePrimaryText, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, color = ExpressiveSecondaryText, style = MaterialTheme.typography.bodyMedium)
        }
        Surface(
            onClick = onViewAll,
            shape = RoundedCornerShape(20.dp),
            color = ExpressiveSurfaceHigh,
            contentColor = GhostGreen,
        ) {
            Text(
                "View all",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
fun GhostProductCard(
    title: String,
    category: String,
    priceCents: Long,
    isFavorite: Boolean,
    image: @Composable () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onGhost: () -> Unit,
    onShare: (() -> Unit)? = null,
    onReviews: (() -> Unit)? = null,
    spotlightTarget: ProductCardSpotlightTarget? = null,
    onSpotlightBounds: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onOpen,
        modifier = modifier
            .width(202.dp)
            .height(352.dp)
            .then(
                if (spotlightTarget == ProductCardSpotlightTarget.CARD && onSpotlightBounds != null) {
                    Modifier.onGloballyPositioned { onSpotlightBounds(it.boundsInWindow()) }
                } else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        color = ExpressiveSurface,
        contentColor = ExpressivePrimaryText,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, GhostSubtleBorder),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.fillMaxSize()) { image() }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .then(
                            if (spotlightTarget == ProductCardSpotlightTarget.FAVORITE && onSpotlightBounds != null) {
                                Modifier.onGloballyPositioned { onSpotlightBounds(it.boundsInWindow()) }
                            } else Modifier
                        ),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Text(
                category,
                color = GhostGreen,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                title,
                color = ExpressivePrimaryText,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 48.dp).padding(top = 4.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                if (priceCents > 0) {
                    DirhamGlyph(tint = ExpressivePrimaryText, modifier = Modifier.size(14.dp))
                    Text(
                        "%,.2f".format(java.util.Locale.US, priceCents / 100.0),
                        color = ExpressivePrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                } else {
                    Text("Add price", color = ExpressiveSecondaryText, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onReviews != null) {
                    Surface(
                        onClick = onReviews,
                        shape = RoundedCornerShape(12.dp),
                        color = ExpressiveSurfaceHigh,
                        modifier = Modifier.then(
                            if (spotlightTarget == ProductCardSpotlightTarget.REVIEWS && onSpotlightBounds != null) {
                                Modifier.onGloballyPositioned { onSpotlightBounds(it.boundsInWindow()) }
                            } else Modifier
                        )
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.RateReview, contentDescription = null, modifier = Modifier.size(15.dp), tint = ExpressiveSecondaryText)
                            Text("No reviews yet", color = ExpressiveSecondaryText, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
                Box(Modifier.weight(1f))
                if (onShare != null) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(44.dp).then(
                            if (spotlightTarget == ProductCardSpotlightTarget.SHARE && onSpotlightBounds != null) {
                                Modifier.onGloballyPositioned { onSpotlightBounds(it.boundsInWindow()) }
                            } else Modifier
                        )
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share product", tint = ExpressivePrimaryText, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Box(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GhostGreen)
                    .clickable(onClick = onGhost)
                    .then(
                        if (spotlightTarget == ProductCardSpotlightTarget.GHOST && onSpotlightBounds != null) {
                            Modifier.onGloballyPositioned { onSpotlightBounds(it.boundsInWindow()) }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = GhostOnGreen, modifier = Modifier.size(16.dp))
                    Text("Ghost it", color = GhostOnGreen, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
