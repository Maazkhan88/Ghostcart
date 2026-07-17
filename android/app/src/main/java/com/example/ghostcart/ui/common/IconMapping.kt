package com.example.ghostcart.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Central string-to-icon lookup so data models can stay plain data (icon names as strings)
 * while screens render real Material icons. Falls back to a generic bag icon for unknown names
 * rather than crashing, since these names originate from static demo data, not user input.
 */
fun materialIconFor(name: String): ImageVector = when (name) {
    "flight" -> Icons.Filled.Flight
    "shield" -> Icons.Filled.Shield
    "house", "home" -> Icons.Filled.House
    "coffee" -> Icons.Filled.LocalCafe
    "spa" -> Icons.Filled.Spa
    "checkroom" -> Icons.Filled.Checkroom
    "devices" -> Icons.Filled.Devices
    "scooter" -> Icons.Filled.TwoWheeler
    "star" -> Icons.Filled.Star
    "chair" -> Icons.Filled.Chair
    "headphones" -> Icons.Filled.Headphones
    "target" -> Icons.Filled.TrackChanges
    "bag" -> Icons.Filled.ShoppingBag
    "psychology" -> Icons.Filled.Psychology
    "bedtime" -> Icons.Filled.Bedtime
    "sell" -> Icons.Filled.Sell
    "bolt" -> Icons.Filled.Bolt
    "wallet" -> Icons.Filled.AccountBalanceWallet
    "savings" -> Icons.Filled.Savings
    "work" -> Icons.Filled.Work
    "lock" -> Icons.Filled.Lock
    "edit" -> Icons.Filled.Edit
    "palette" -> Icons.Filled.Palette
    "notifications" -> Icons.Filled.Notifications
  "acUnit" -> Icons.Filled.AcUnit
    "delete" -> Icons.Filled.Delete
    "search" -> Icons.Filled.Search
    "person" -> Icons.Filled.Person
    "favorite" -> Icons.Filled.Favorite
    "favoriteBorder" -> Icons.Filled.FavoriteBorder
    "accessTime" -> Icons.Filled.AccessTime
    "download" -> Icons.Filled.Download
    "share" -> Icons.Filled.Share
    "dateRange" -> Icons.Filled.DateRange
    "locationOn" -> Icons.Filled.LocationOn
    "contentCopy" -> Icons.Filled.ContentCopy
    "info" -> Icons.Filled.Info
    "tune" -> Icons.Filled.Tune
    "menuBook" -> Icons.Filled.MenuBook
    "creditCard" -> Icons.Filled.CreditCard
    else -> Icons.Filled.ShoppingBag
}
