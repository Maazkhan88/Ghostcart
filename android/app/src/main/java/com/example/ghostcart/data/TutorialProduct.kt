package com.example.ghostcart.data

import android.content.Context
import com.ghostcart.app.R

/** Local-only practice product. It is never appended to Marketplace catalogues or uploaded. */
fun tutorialPracticeProduct(context: Context): MarketplaceProduct = MarketplaceProduct(
    id = TUTORIAL_PRODUCT_ID,
    name = "Coffee and donut",
    category = "Food & delivery",
    price = 31,
    iconName = "donut",
    description = "A warm coffee and glazed donut, on the house for your first Ghost Cart practice run.",
    brand = "Ghost Café · Tutorial",
    sourceDomain = "Ghost Café · Tutorial",
    imageUrl = "android.resource://${context.packageName}/${R.drawable.tutorial_coffee_donut_combo}",
    sourceUrl = null,
    ghostCount = 0,
    isUserGhosted = false,
    lastGhostedAtMillis = null
)
