package com.example.ghostcart.ui.app

import java.util.Calendar

/**
 * Milliseconds from [now] until the next occurrence of [hourOfDay]:00:00 in [now]'s
 * calendar/timezone. Pure and clock-injectable so scheduling correctness (before/after
 * the target hour, timezone changes, DST transitions) is covered by deterministic unit
 * tests instead of relying only on a multi-day device observation.
 */
fun delayUntilHourFrom(now: Calendar, hourOfDay: Int): Long {
    val next = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, hourOfDay)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
    }
    return (next.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
}
