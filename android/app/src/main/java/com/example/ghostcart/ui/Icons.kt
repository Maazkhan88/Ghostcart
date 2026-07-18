package com.example.ghostcart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ghostcart.app.R
import kotlin.math.roundToInt

@Composable
fun ProductIcon(name: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    val approvedAsset = when (name) {
        "sneaker" -> R.drawable.product_sneaker
        "perfume" -> R.drawable.product_perfume
        else -> null
    }

    if (approvedAsset != null) {
        Image(
            painter = painterResource(approvedAsset),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
        return
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.06f

        when (name) {
            "sneaker" -> {
                // Drawing sneaker silhouette
                val path = Path().apply {
                    moveTo(w * 0.1f, h * 0.75f)
                    lineTo(w * 0.9f, h * 0.75f)
                    lineTo(w * 0.9f, h * 0.55f)
                    quadraticTo(w * 0.75f, h * 0.5f, w * 0.65f, h * 0.3f)
                    lineTo(w * 0.45f, h * 0.3f)
                    lineTo(w * 0.35f, h * 0.45f)
                    quadraticTo(w * 0.2f, h * 0.5f, w * 0.1f, h * 0.55f)
                    close()
                }
                drawPath(path, color, style = Stroke(width = strokeWidth))
                // Sole line
                drawLine(
                    color = color,
                    start = Offset(w * 0.1f, h * 0.68f),
                    end = Offset(w * 0.9f, h * 0.68f),
                    strokeWidth = strokeWidth
                )
            }
            "perfume" -> {
                // Perfume bottle
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.2f, h * 0.35f),
                    size = Size(w * 0.6f, h * 0.5f),
                    cornerRadius = CornerRadius(w * 0.1f),
                    style = Stroke(width = strokeWidth)
                )
                // Cap
                drawRect(
                    color = color,
                    topLeft = Offset(w * 0.4f, h * 0.15f),
                    size = Size(w * 0.2f, h * 0.2f),
                    style = Stroke(width = strokeWidth)
                )
                // Label
                drawRect(
                    color = color,
                    topLeft = Offset(w * 0.35f, h * 0.48f),
                    size = Size(w * 0.3f, h * 0.24f),
                    style = Stroke(width = strokeWidth * 0.6f)
                )
            }
            "burger" -> {
                // Bun top
                val pathTop = Path().apply {
                    moveTo(w * 0.15f, h * 0.4f)
                    quadraticTo(w * 0.5f, h * 0.1f, w * 0.85f, h * 0.4f)
                    close()
                }
                drawPath(pathTop, color, style = Stroke(width = strokeWidth))
                // Cheese/Meat middle layer
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.46f),
                    size = Size(w * 0.76f, h * 0.1f),
                    cornerRadius = CornerRadius(w * 0.04f),
                    style = Stroke(width = strokeWidth)
                )
                // Bun bottom
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.15f, h * 0.62f),
                    size = Size(w * 0.7f, h * 0.2f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "headphones" -> {
                // Headband arc
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.15f),
                    size = Size(w * 0.7f, h * 0.7f),
                    style = Stroke(width = strokeWidth)
                )
                // Left ear cup
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.5f),
                    size = Size(w * 0.14f, h * 0.3f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = Stroke(width = strokeWidth)
                )
                // Right ear cup
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.74f, h * 0.5f),
                    size = Size(w * 0.14f, h * 0.3f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "shield" -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    quadraticTo(w * 0.8f, h * 0.15f, w * 0.85f, h * 0.2f)
                    lineTo(w * 0.85f, h * 0.55f)
                    quadraticTo(w * 0.7f, h * 0.8f, w * 0.5f, h * 0.9f)
                    quadraticTo(w * 0.3f, h * 0.8f, w * 0.15f, h * 0.55f)
                    lineTo(w * 0.15f, h * 0.2f)
                    quadraticTo(w * 0.2f, h * 0.15f, w * 0.5f, h * 0.1f)
                }
                drawPath(path, color, style = Stroke(width = strokeWidth))
            }
            "lock" -> {
                // Body
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.2f, h * 0.45f),
                    size = Size(w * 0.6f, h * 0.45f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                // Shackle
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.3f, h * 0.18f),
                    size = Size(w * 0.4f, h * 0.5f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "leaf" -> {
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.85f)
                    quadraticTo(w * 0.15f, h * 0.45f, w * 0.55f, h * 0.15f)
                    quadraticTo(w * 0.85f, h * 0.45f, w * 0.85f, h * 0.85f)
                    quadraticTo(w * 0.55f, h * 0.85f, w * 0.15f, h * 0.85f)
                }
                drawPath(path, color, style = Stroke(width = strokeWidth))
                drawLine(
                    color = color,
                    start = Offset(w * 0.15f, h * 0.85f),
                    end = Offset(w * 0.55f, h * 0.45f),
                    strokeWidth = strokeWidth * 0.7f
                )
            }
            "wallet" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.15f, h * 0.25f),
                    size = Size(w * 0.7f, h * 0.55f),
                    cornerRadius = CornerRadius(w * 0.06f),
                    style = Stroke(width = strokeWidth)
                )
                // Flap
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.55f, h * 0.4f),
                    size = Size(w * 0.32f, h * 0.25f),
                    cornerRadius = CornerRadius(w * 0.03f),
                    style = Stroke(width = strokeWidth * 0.8f)
                )
            }
            "chart" -> {
                drawLine(color, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.2f, h * 0.4f), strokeWidth)
                drawLine(color, Offset(w * 0.5f, h * 0.8f), Offset(w * 0.5f, h * 0.2f), strokeWidth)
                drawLine(color, Offset(w * 0.8f, h * 0.8f), Offset(w * 0.8f, h * 0.5f), strokeWidth)
            }
            "coffee" -> {
                // Cup body: trapezoid, narrower at the base
                val cupPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.42f)
                    lineTo(w * 0.68f, h * 0.42f)
                    lineTo(w * 0.6f, h * 0.82f)
                    quadraticTo(w * 0.6f, h * 0.88f, w * 0.53f, h * 0.88f)
                    lineTo(w * 0.37f, h * 0.88f)
                    quadraticTo(w * 0.3f, h * 0.88f, w * 0.3f, h * 0.82f)
                    close()
                }
                drawPath(cupPath, color, style = Stroke(width = strokeWidth))
                // Handle: small loop on the right side of the cup
                drawArc(
                    color = color,
                    startAngle = -70f,
                    sweepAngle = 200f,
                    useCenter = false,
                    topLeft = Offset(w * 0.66f, h * 0.48f),
                    size = Size(w * 0.2f, h * 0.22f),
                    style = Stroke(width = strokeWidth * 0.8f)
                )
                // Steam wisps
                drawLine(color, Offset(w * 0.38f, h * 0.32f), Offset(w * 0.34f, h * 0.18f), strokeWidth * 0.6f)
                drawLine(color, Offset(w * 0.52f, h * 0.32f), Offset(w * 0.56f, h * 0.18f), strokeWidth * 0.6f)
            }
            "watch" -> {
                // Watch face
                drawCircle(color, radius = w * 0.28f, center = Offset(w * 0.5f, h * 0.52f), style = Stroke(width = strokeWidth))
                // Hands
                drawLine(color, Offset(w * 0.5f, h * 0.52f), Offset(w * 0.5f, h * 0.36f), strokeWidth * 0.7f)
                drawLine(color, Offset(w * 0.5f, h * 0.52f), Offset(w * 0.6f, h * 0.56f), strokeWidth * 0.7f)
                // Top and bottom straps
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.4f, h * 0.08f),
                    size = Size(w * 0.2f, h * 0.16f),
                    cornerRadius = CornerRadius(w * 0.04f),
                    style = Stroke(width = strokeWidth * 0.8f)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.4f, h * 0.76f),
                    size = Size(w * 0.2f, h * 0.16f),
                    cornerRadius = CornerRadius(w * 0.04f),
                    style = Stroke(width = strokeWidth * 0.8f)
                )
                // Side button
                drawLine(color, Offset(w * 0.78f, h * 0.46f), Offset(w * 0.84f, h * 0.46f), strokeWidth * 0.7f)
            }
            "tablet" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.22f, h * 0.1f),
                    size = Size(w * 0.56f, h * 0.8f),
                    cornerRadius = CornerRadius(w * 0.06f),
                    style = Stroke(width = strokeWidth)
                )
                // Home indicator
                drawLine(color, Offset(w * 0.44f, h * 0.83f), Offset(w * 0.56f, h * 0.83f), strokeWidth)
            }
            "donut" -> {
                drawCircle(color, radius = w * 0.32f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = strokeWidth * 2.2f))
                drawCircle(color, radius = w * 0.02f, center = Offset(w * 0.38f, h * 0.26f))
                drawCircle(color, radius = w * 0.02f, center = Offset(w * 0.6f, h * 0.3f))
                drawCircle(color, radius = w * 0.02f, center = Offset(w * 0.48f, h * 0.2f))
            }
            "lipstick" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.38f, h * 0.45f),
                    size = Size(w * 0.24f, h * 0.4f),
                    cornerRadius = CornerRadius(w * 0.03f),
                    style = Stroke(width = strokeWidth)
                )
                val tipPath = Path().apply {
                    moveTo(w * 0.38f, h * 0.45f)
                    lineTo(w * 0.62f, h * 0.45f)
                    lineTo(w * 0.56f, h * 0.2f)
                    lineTo(w * 0.44f, h * 0.2f)
                    close()
                }
                drawPath(tipPath, color, style = Stroke(width = strokeWidth))
            }
            "jar" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.22f, h * 0.4f),
                    size = Size(w * 0.56f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.28f, h * 0.22f),
                    size = Size(w * 0.44f, h * 0.2f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "incense" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.3f, h * 0.78f),
                    size = Size(w * 0.4f, h * 0.1f),
                    cornerRadius = CornerRadius(w * 0.02f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(color, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.5f, h * 0.35f), strokeWidth * 0.6f)
                drawLine(color, Offset(w * 0.5f, h * 0.35f), Offset(w * 0.42f, h * 0.22f), strokeWidth * 0.5f)
                drawLine(color, Offset(w * 0.42f, h * 0.22f), Offset(w * 0.54f, h * 0.12f), strokeWidth * 0.5f)
            }
            "shirt" -> {
                val path = Path().apply {
                    moveTo(w * 0.38f, h * 0.2f)
                    lineTo(w * 0.22f, h * 0.32f)
                    lineTo(w * 0.3f, h * 0.42f)
                    lineTo(w * 0.3f, h * 0.82f)
                    lineTo(w * 0.7f, h * 0.82f)
                    lineTo(w * 0.7f, h * 0.42f)
                    lineTo(w * 0.78f, h * 0.32f)
                    lineTo(w * 0.62f, h * 0.2f)
                    quadraticTo(w * 0.5f, h * 0.28f, w * 0.38f, h * 0.2f)
                    close()
                }
                drawPath(path, color, style = Stroke(width = strokeWidth))
            }
            "bag" -> {
                val path = Path().apply {
                    moveTo(w * 0.22f, h * 0.38f)
                    lineTo(w * 0.78f, h * 0.38f)
                    lineTo(w * 0.72f, h * 0.85f)
                    lineTo(w * 0.28f, h * 0.85f)
                    close()
                }
                drawPath(path, color, style = Stroke(width = strokeWidth))
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.34f, h * 0.12f),
                    size = Size(w * 0.14f, h * 0.32f),
                    style = Stroke(width = strokeWidth * 0.8f)
                )
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.52f, h * 0.12f),
                    size = Size(w * 0.14f, h * 0.32f),
                    style = Stroke(width = strokeWidth * 0.8f)
                )
            }
            "sunglasses" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.14f, h * 0.4f),
                    size = Size(w * 0.3f, h * 0.24f),
                    cornerRadius = CornerRadius(w * 0.06f),
                    style = Stroke(width = strokeWidth)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.56f, h * 0.4f),
                    size = Size(w * 0.3f, h * 0.24f),
                    cornerRadius = CornerRadius(w * 0.06f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(color, Offset(w * 0.44f, h * 0.48f), Offset(w * 0.56f, h * 0.48f), strokeWidth * 0.8f)
                drawLine(color, Offset(w * 0.14f, h * 0.46f), Offset(w * 0.04f, h * 0.4f), strokeWidth * 0.7f)
                drawLine(color, Offset(w * 0.86f, h * 0.46f), Offset(w * 0.96f, h * 0.4f), strokeWidth * 0.7f)
            }
            "speaker" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.28f, h * 0.12f),
                    size = Size(w * 0.44f, h * 0.76f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                drawCircle(color, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.42f), style = Stroke(width = strokeWidth * 0.8f))
                drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.42f), style = Stroke(width = strokeWidth * 0.6f))
                drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.5f, h * 0.68f))
            }
            "gadget" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.28f, h * 0.28f),
                    size = Size(w * 0.44f, h * 0.44f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(color, Offset(w * 0.36f, h * 0.28f), Offset(w * 0.36f, h * 0.18f), strokeWidth * 0.6f)
                drawLine(color, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.5f, h * 0.18f), strokeWidth * 0.6f)
                drawLine(color, Offset(w * 0.64f, h * 0.28f), Offset(w * 0.64f, h * 0.18f), strokeWidth * 0.6f)
                drawLine(color, Offset(w * 0.36f, h * 0.72f), Offset(w * 0.36f, h * 0.82f), strokeWidth * 0.6f)
                drawLine(color, Offset(w * 0.5f, h * 0.72f), Offset(w * 0.5f, h * 0.82f), strokeWidth * 0.6f)
                drawLine(color, Offset(w * 0.64f, h * 0.72f), Offset(w * 0.64f, h * 0.82f), strokeWidth * 0.6f)
            }
        }
    }
}

private data class ProductPhotoCrop(
    val resourceId: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

@Composable
fun ProductPhoto(
    productName: String,
    fallbackIconName: String,
    modifier: Modifier = Modifier
) {
    val normalizedName = productName
        .lowercase()
        .replace(Regex("#\\d+"), "")
        .trim()

    val directAsset = when {
        normalizedName.contains("spanish latte") -> R.drawable.product_marketplace_spanish_latte
        normalizedName.contains("midnight burger") -> R.drawable.product_marketplace_midnight_burger
        normalizedName.contains("blackout burger combo") -> R.drawable.product_marketplace_blackout_burger_combo
        normalizedName.contains("coffee & croissant combo") -> R.drawable.product_marketplace_croissant_coffee
        normalizedName.contains("pizza combo") -> R.drawable.product_marketplace_pizza_combo
        normalizedName.contains("ghost cappuccino") -> R.drawable.product_marketplace_ghost_cappuccino
        normalizedName.contains("noise cancelling headphones") -> R.drawable.product_marketplace_noise_cancelling_headphones
        normalizedName.contains("acai bowl") -> R.drawable.product_marketplace_acai_bowl
        normalizedName.contains("white sneaker") -> R.drawable.product_marketplace_white_sneakers
        normalizedName.contains("gaming tablet") -> R.drawable.product_marketplace_gaming_tablet
        normalizedName.contains("minimal smartwatch") -> R.drawable.product_marketplace_minimal_smartwatch
        normalizedName.contains("wireless earbuds") -> R.drawable.product_marketplace_wireless_earbuds
        normalizedName.contains("luxury perfume") -> R.drawable.product_marketplace_luxury_perfume
        normalizedName.contains("smartwatch pro") -> R.drawable.product_marketplace_smartwatch_pro
        normalizedName.contains("ghost cart phone case") -> R.drawable.product_merch_phone_case
        normalizedName.contains("ghost cart laptop sleeve") -> R.drawable.product_merch_laptop_sleeve
        normalizedName.contains("ghost cart travel tumbler") -> R.drawable.product_merch_travel_tumbler
        normalizedName.contains("ghost cart classic cap") -> R.drawable.product_merch_classic_cap
        normalizedName.contains("ghost cart tote bag") -> R.drawable.product_merch_tote_bag
        normalizedName.contains("ghost cart steel bottle") -> R.drawable.product_merch_steel_bottle
        normalizedName.contains("ghost cart notebook") -> R.drawable.product_merch_notebook
        normalizedName.contains("ghost cart pen") -> R.drawable.product_merch_pen
        normalizedName.contains("ghost cart ceramic mug") -> R.drawable.product_merch_ceramic_mug
        normalizedName.contains("ghost cart lunch box") -> R.drawable.product_merch_lunch_box
        normalizedName.contains("ghost cart running sneakers") -> R.drawable.product_merch_running_sneakers
        normalizedName.contains("ghost cart logo t-shirt") -> R.drawable.product_merch_logo_tshirt
        normalizedName.contains("ghost cart protein shaker") -> R.drawable.product_merch_protein_shaker
        normalizedName.contains("ghost cart backpack") -> R.drawable.product_merch_backpack
        normalizedName.contains("ghost cart bucket hat") -> R.drawable.product_merch_bucket_hat
        normalizedName.contains("ghost cart minimal watch") -> R.drawable.product_merch_minimal_watch
        normalizedName.contains("ghost cart gym duffel") -> R.drawable.product_merch_gym_duffel
        normalizedName.contains("ghost cart supplement jar") -> R.drawable.product_merch_supplement_jar
        normalizedName.contains("ghost cart logo hoodie") -> R.drawable.product_merch_logo_hoodie
        normalizedName.contains("ghost cart beanie") -> R.drawable.product_merch_beanie
        normalizedName.contains("headphone") -> R.drawable.product_marketplace_headphones
        normalizedName.contains("tablet") -> R.drawable.product_marketplace_tablet
        normalizedName.contains("perfume") || normalizedName.contains("cologne") -> R.drawable.product_perfume
        else -> null
    }

    if (directAsset != null) {
        Image(
            painter = painterResource(directAsset),
            contentDescription = "$productName product photo",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
        return
    }

    val localCrop = when {
        normalizedName.contains("late combo") -> ProductPhotoCrop(R.drawable.product_reference_food, 90, 595, 228, 255)
        normalizedName.contains("midnight burger") -> ProductPhotoCrop(R.drawable.product_reference_food, 345, 595, 228, 255)
        normalizedName.contains("pizza") -> ProductPhotoCrop(R.drawable.product_reference_food, 596, 595, 252, 255)
        normalizedName.contains("acai") -> ProductPhotoCrop(R.drawable.product_reference_food, 90, 1050, 228, 225)
        normalizedName.contains("latte") || normalizedName.contains("macchiato") || normalizedName.contains("matcha") || normalizedName.contains("boba") ->
            ProductPhotoCrop(R.drawable.product_reference_food, 344, 1050, 228, 225)
        normalizedName.contains("earbud") -> ProductPhotoCrop(R.drawable.product_reference_home, 122, 920, 185, 145)
        normalizedName.contains("smartwatch") -> ProductPhotoCrop(R.drawable.product_reference_home, 344, 920, 185, 145)
        normalizedName.contains("tablet") -> ProductPhotoCrop(R.drawable.product_reference_home, 568, 920, 185, 145)
        normalizedName.contains("headphone") -> ProductPhotoCrop(R.drawable.product_reference_home, 790, 920, 152, 145)
        normalizedName.contains("lipstick") || normalizedName.contains("lip glow") ->
            ProductPhotoCrop(R.drawable.product_photo_atlas, 760, 285, 255, 290)
        else -> null
    }

    if (localCrop != null) {
        ProductAtlasPhoto(crop = localCrop, modifier = modifier)
        return
    }

    // Never fetch uncontrolled search-result imagery. Until a product has an
    // approved/licensed asset, render a bundled category photograph.
    ProductAtlasPhoto(crop = fallbackProductPhoto(fallbackIconName), modifier = modifier)
}

private fun fallbackProductPhoto(iconName: String): ProductPhotoCrop = when (iconName) {
    "headphones", "speaker", "watch", "tablet", "gadget", "devices" ->
        ProductPhotoCrop(R.drawable.product_reference_home, 790, 920, 152, 145)
    "lipstick", "jar", "incense", "perfume" ->
        ProductPhotoCrop(R.drawable.product_photo_atlas, 585, 285, 230, 290)
    "shirt", "bag", "sunglasses", "sneaker" ->
        ProductPhotoCrop(R.drawable.product_photo_atlas, 35, 285, 300, 290)
    "coffee" -> ProductPhotoCrop(R.drawable.product_reference_food, 344, 1050, 228, 225)
    "leaf" -> ProductPhotoCrop(R.drawable.product_reference_food, 90, 1050, 228, 225)
    else -> ProductPhotoCrop(R.drawable.product_reference_food, 345, 595, 228, 255)
}

@Composable
private fun ProductAtlasPhoto(
    crop: ProductPhotoCrop,
    modifier: Modifier = Modifier
) {
    val atlas = ImageBitmap.imageResource(crop.resourceId)
    Canvas(modifier = modifier) {
        drawImage(
            image = atlas,
            srcOffset = IntOffset(crop.x, crop.y),
            srcSize = IntSize(crop.width, crop.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.High
        )
    }
}

@Composable
fun GhostMascotPose(poseName: String, modifier: Modifier = Modifier) {
    val asset = when (poseName) {
        "cart" -> R.drawable.mascot_cart
        "wallet" -> R.drawable.mascot_wallet
        "cooldown" -> R.drawable.mascot_cooldown
        "thumbsup" -> R.drawable.mascot_thumbsup
        "trio" -> R.drawable.mascot_trio
        "phoneList" -> R.drawable.mascot_phone_list
        "checkoutPhone" -> R.drawable.mascot_checkout_phone
        "combo" -> R.drawable.mascot_combo
        "waveAlt" -> R.drawable.mascot_wave_alt
        else -> R.drawable.mascot_wave
    }

    Image(
        painter = painterResource(asset),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun GhostCartWordmark(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Ghost Cart",
    tint: Color = Color.Unspecified
) {
    Image(
        painter = painterResource(R.drawable.ghost_cart_logo_horizontal),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        alignment = androidx.compose.ui.Alignment.Center,
        colorFilter = if (tint == Color.Unspecified) null else ColorFilter.tint(tint),
        modifier = modifier
    )
}

/** Official UAE Dirham symbol. Use in place of the "AED" text prefix wherever a price is shown. */
@Composable
fun DirhamGlyph(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    Image(
        painter = painterResource(R.drawable.currency_dirham),
        contentDescription = "AED",
        contentScale = ContentScale.Fit,
        colorFilter = if (tint == Color.Unspecified) null else androidx.compose.ui.graphics.ColorFilter.tint(tint),
        modifier = modifier
    )
}
