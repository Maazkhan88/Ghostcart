package com.example.ghostcart.ui.app

import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Deterministic coverage for [delayUntilHourFrom], the scheduling calculation behind the
 * lunch/dinner reminder. These replace relying only on a multi-day manual device observation
 * to catch drift in when the reminder actually fires.
 *
 * Device-reboot survival and WorkManager's own Doze/maintenance-window batching are library
 * guarantees outside what a JVM unit test can exercise directly; those remain covered by the
 * manual device test matrix (see Phase 1 verification notes), while this suite locks down the
 * pure scheduling math that the self-rescheduling worker chain depends on to avoid drifting.
 */
class ReminderSchedulingTest {

    private fun calendarAt(
        zone: TimeZone = TimeZone.getDefault(),
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0
    ): Calendar = Calendar.getInstance(zone).apply {
        set(year, month, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }

    @Test
    fun schedulingBeforeTheConfiguredHourTargetsLaterTheSameDay() {
        val now = calendarAt(year = 2026, month = Calendar.JULY, day = 19, hour = 8)
        val delay = delayUntilHourFrom(now, hourOfDay = 20)
        assertEquals(TimeUnit.HOURS.toMillis(12), delay)
    }

    @Test
    fun schedulingAfterTheConfiguredHourRollsToTheNextDay() {
        val now = calendarAt(year = 2026, month = Calendar.JULY, day = 19, hour = 21)
        val delay = delayUntilHourFrom(now, hourOfDay = 20)
        assertEquals(TimeUnit.HOURS.toMillis(23), delay)
    }

    @Test
    fun schedulingExactlyAtTheConfiguredHourRollsToTheNextDay() {
        // "now" landing exactly on the target instant must not fire immediately/return 0 -
        // it should roll forward a full day, matching Calendar's `!after(now)` boundary check.
        val now = calendarAt(year = 2026, month = Calendar.JULY, day = 19, hour = 20)
        val delay = delayUntilHourFrom(now, hourOfDay = 20)
        assertEquals(TimeUnit.HOURS.toMillis(24), delay)
    }

    @Test
    fun missedFiringSelfCorrectsToThatDaysHourInsteadOfDrifting() {
        // Simulates a Doze-deferred worker actually waking up well after its intended time
        // (e.g. 08:00 instead of yesterday's 20:00). Recomputing from the real "now" must
        // target *that same day's* configured hour, not compound the earlier miss into a
        // creeping-earlier schedule the way a bare PeriodicWorkRequest anchor can.
        val now = calendarAt(year = 2026, month = Calendar.JULY, day = 20, hour = 8)
        val delay = delayUntilHourFrom(now, hourOfDay = 20)
        val trigger = (now.clone() as Calendar).apply { timeInMillis = now.timeInMillis + delay }
        assertEquals(TimeUnit.HOURS.toMillis(12), delay)
        assertEquals(20, trigger.get(Calendar.HOUR_OF_DAY))
        assertEquals(20, trigger.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun timezoneChangeRecomputesAgainstTheNewLocalHour() {
        val utc = TimeZone.getTimeZone("UTC")
        val dubai = TimeZone.getTimeZone("Asia/Dubai") // UTC+4, no DST

        // Same underlying instant (06:00 UTC == 10:00 Dubai) but scheduling is evaluated
        // against each zone's own local wall clock, as it would be if the device's timezone
        // changed between the last schedule and this calculation.
        val nowUtc = calendarAt(zone = utc, year = 2026, month = Calendar.JULY, day = 19, hour = 6)
        val nowDubai = calendarAt(zone = dubai, year = 2026, month = Calendar.JULY, day = 19, hour = 10)
        assertEquals(nowUtc.timeInMillis, nowDubai.timeInMillis)

        val delayUtc = delayUntilHourFrom(nowUtc, hourOfDay = 20)
        val delayDubai = delayUntilHourFrom(nowDubai, hourOfDay = 20)

        assertEquals(TimeUnit.HOURS.toMillis(14), delayUtc)
        assertEquals(TimeUnit.HOURS.toMillis(10), delayDubai)
    }

    @Test
    fun daylightSavingSpringForwardStillTargetsCorrectWallClockHour() {
        // US Eastern DST begins 2026-03-08 02:00 local (clocks jump 02:00 -> 03:00).
        // "now" is before the jump (still EST, UTC-5); the target 20:00 that same day falls
        // after the jump (EDT, UTC-4), so the real elapsed delay is 1 hour less than the naive
        // wall-clock difference would suggest - Calendar must account for that automatically.
        val zone = TimeZone.getTimeZone("America/New_York")
        val now = calendarAt(zone = zone, year = 2026, month = Calendar.MARCH, day = 8, hour = 1, minute = 30)

        val delay = delayUntilHourFrom(now, hourOfDay = 20)
        val trigger = (now.clone() as Calendar).apply { timeInMillis = now.timeInMillis + delay }

        assertEquals(TimeUnit.HOURS.toMillis(17) + TimeUnit.MINUTES.toMillis(30), delay)
        assertEquals(20, trigger.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, trigger.get(Calendar.MINUTE))
        assertEquals(8, trigger.get(Calendar.DAY_OF_MONTH))
    }
}
