package com.example.ghostcart

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

// Onboarding
@Serializable data object Splash : NavKey
@Serializable data object Auth : NavKey
@Serializable data object ProfileSelect : NavKey
@Serializable data object Personalization : NavKey
@Serializable data object Tutorial : NavKey

// Marketplace / shopping
@Serializable data object Home : NavKey
@Serializable data object Cooldowns : NavKey
@Serializable data object CaptureAlmostBuy : NavKey
@Serializable data object Progress : NavKey
@Serializable data class CategoryBrowse(val categoryId: String) : NavKey
@Serializable data class ProductDetail(val productId: String) : NavKey
@Serializable data object GhostCartList : NavKey
@Serializable data object GhostCheckout : NavKey
@Serializable data object OrderGhostedSuccess : NavKey
@Serializable data object FakeDeliveryTracking : NavKey
@Serializable data class GhostDeliveryTracker(val itemId: String) : NavKey
@Serializable data object PayWithGhostCard : NavKey
@Serializable data object OrderProtected : NavKey
@Serializable data class GhostGiftReveal(val token: String) : NavKey
@Serializable data object Gifts : NavKey

// Ghost Wallet
@Serializable data object WalletHome : NavKey
@Serializable data object WalletSetup : NavKey
@Serializable data object SalaryShield : NavKey
@Serializable data object Goals : NavKey
@Serializable data object WalletActivity : NavKey
@Serializable data object WeeklyStatement : NavKey
@Serializable data object Trends : NavKey
@Serializable data object GhostCardSettings : NavKey
@Serializable data class LegalDocument(val docId: String) : NavKey
@Serializable data object Leaderboard : NavKey
@Serializable data class LeaderboardDetail(val username: String) : NavKey
@Serializable data object Notifications : NavKey
