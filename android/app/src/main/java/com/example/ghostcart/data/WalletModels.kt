package com.example.ghostcart.data

data class SavingsGoal(
    val id: String,
    val name: String,
    val tagline: String,
    val current: Int,
    val target: Int,
    val iconName: String
) {
    val percent: Int get() = if (target <= 0) 0 else ((current.toFloat() / target) * 100).toInt().coerceIn(0, 100)
}

data class WalletTransaction(
    val id: String,
    val name: String,
    val category: String,
    val amount: Int,
    val whenLabel: String,
    val iconName: String
)

data class WalletConfig(
    val monthlySalary: Int = 12000,
    val monthlySavingsGoal: Int = 2500,
    val temptationBudget: Int = 1500,
    val startingBalance: Int = 0,
    val salaryShieldEnabled: Boolean = true,
    val coolingNotificationsEnabled: Boolean = true,
    val lunchReminderEnabled: Boolean = false,
    val dinnerReminderEnabled: Boolean = false,
    val autoAllocateToGoals: Boolean = true,
    val cardFrozen: Boolean = false,
    val cardTheme: String = "Dark",
    val cardName: String = "Ghost Membership",
    val cardholderName: String = "Set your name",
    val salaryShieldPercent: Int = 20,
    val ghostId: String = "GC-DEMO-MEMBER-ID",
    val memberSince: String = "Jul 2026"
) {
    /** Compatibility for retired prototype settings; new UI exposes each preference separately. */
    val walletNotificationsEnabled: Boolean
        get() = coolingNotificationsEnabled || lunchReminderEnabled || dinnerReminderEnabled
}

/** Legacy reference values retained only for unreachable prototype screens. */
object WalletDemoData {
    const val currentBalance = 8420
    const val protectedThisMonth = 1240
    const val ghostedOrdersThisMonth = 24
    const val savingsGoalPercent = 68

    val goals = listOf(
        SavingsGoal(
            id = "travel",
            name = "Travel Fund",
            tagline = "Dream trips start here.",
            current = 4250,
            target = 8000,
            iconName = "flight"
        ),
        SavingsGoal(
            id = "emergency",
            name = "Emergency Fund",
            tagline = "Peace of mind, always.",
            current = 2100,
            target = 5000,
            iconName = "shield"
        ),
        SavingsGoal(
            id = "rent",
            name = "Rent Goal",
            tagline = "Stay ahead, stress free.",
            current = 6200,
            target = 10000,
            iconName = "house"
        )
    )

    val recentSaves = listOf(
        WalletTransaction("t1", "Midnight Burger Meal", "Food & Drinks", 45, "Today", "burger"),
        WalletTransaction("t2", "Luxury Perfume", "Beauty", 120, "Yesterday", "perfume"),
        WalletTransaction("t3", "Wireless Earbuds", "Electronics", 210, "2 days ago", "headphones"),
        WalletTransaction("t4", "White Sneakers", "Fashion", 185, "2 days ago", "sneaker"),
        WalletTransaction("t5", "Pepperoni Pizza", "Food & Drinks", 35, "3 days ago", "burger"),
        WalletTransaction("t6", "Noise Cancelling Headphones", "Electronics", 320, "5 days ago", "headphones")
    )

    val weeklyChart = listOf(85, 120, 95, 160, 130, 180, 172)
    val weeklyChartDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    const val weeklyTotalProtected = 942
    const val weeklyGhostedOrders = 8
    const val weeklyAverageOrder = 118
    const val weeklyChangePercent = 32

    val mostGhostedCategories = listOf(
        Triple("Food & Coffee", 2450, 31),
        Triple("Beauty & Perfume", 1980, 25),
        Triple("Fashion & Shoes", 1530, 19),
        Triple("Gadgets & Tech", 1210, 15)
    )

    val topTriggers = listOf(
        Triple("Boredom", "psychology", 28),
        Triple("Late-night scrolling", "bedtime", 26),
        Triple("FOMO offers", "sell", 24),
        Triple("Stress", "bolt", 22)
    )
}
