package com.example.ghostcart.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val DECISION_REMINDERS_CHANNEL_ID = "decision_reminders"
const val FRIENDS_GIFTS_CHANNEL_ID = "friends_and_gifts"
const val GHOST_CART_UPDATES_CHANNEL_ID = "ghost_cart_updates"

/** Creates the user-manageable notification categories without prompting for permission. */
fun ensureGhostNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannels(
        listOf(
            NotificationChannel(
                GHOST_DELIVERY_CHANNEL_ID,
                "Ghost Delivery updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Stages for simulated Ghost Orders and Ghost Deliveries" },
            NotificationChannel(
                DECISION_REMINDERS_CHANNEL_ID,
                "Decision reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Reminders to resolve a delivered or cooling decision" },
            NotificationChannel(
                FRIENDS_GIFTS_CHANNEL_ID,
                "Friends and gifts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Shared almost-buys, friend responses and simulated gifts" },
            NotificationChannel(
                GHOST_CART_UPDATES_CHANNEL_ID,
                "Ghost Cart updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Optional Ghost Cart product and service updates" }
        )
    )
}
