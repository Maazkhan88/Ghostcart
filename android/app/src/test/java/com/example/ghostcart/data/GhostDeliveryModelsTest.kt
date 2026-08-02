package com.example.ghostcart.data

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GhostDeliveryModelsTest {
    private val start = 1_000_000L
    private val duration = TimeUnit.MINUTES.toMillis(100)
    private val end = start + duration

    @Test
    fun computesEveryRequiredStageAtDeterministicThresholds() {
        assertEquals(GhostDeliveryState.PLACED, stageAt(0.00))
        assertEquals(GhostDeliveryState.PREPARING, stageAt(0.10))
        assertEquals(GhostDeliveryState.RIDER_PICKING_UP, stageAt(0.25))
        assertEquals(GhostDeliveryState.OUT_FOR_DELIVERY, stageAt(0.45))
        assertEquals(GhostDeliveryState.RIDER_NEARBY, stageAt(0.80))
        assertEquals(GhostDeliveryState.DELIVERED, stageAt(1.00))
    }

    @Test
    fun supportsEveryStandardDurationAndCustomDuration() {
        val durations = listOf(
            TimeUnit.MINUTES.toMillis(15),
            TimeUnit.HOURS.toMillis(1),
            TimeUnit.HOURS.toMillis(24),
            TimeUnit.DAYS.toMillis(7),
            TimeUnit.DAYS.toMillis(3) + TimeUnit.HOURS.toMillis(5)
        )
        durations.forEach { selected ->
            val schedule = ghostStageSchedule(start, start + selected)
            assertEquals(6, schedule.size)
            assertEquals(GhostDeliveryState.PLACED, schedule.first().state)
            assertEquals(GhostDeliveryState.DELIVERED, schedule.last().state)
            assertEquals(start + selected, schedule.last().triggerAtMillis)
            assertTrue(schedule.zipWithNext().all { (a, b) -> b.triggerAtMillis >= a.triggerAtMillis })
        }
    }

    @Test
    fun terminalResolutionCannotMoveBackwardsWhenDeviceClockChanges() {
        val snapshot = ghostDeliverySnapshot(
            nowMillis = start - TimeUnit.DAYS.toMillis(30),
            startMillis = start,
            endMillis = end,
            persistedState = GhostDeliveryState.RESOLVED_SKIPPED
        )
        assertEquals(GhostDeliveryState.RESOLVED_SKIPPED, snapshot.state)
        assertEquals(1f, snapshot.progress)
    }

    @Test
    fun routeIsStablePerOrderAndInterpolatesWithinBounds() {
        val first = simulatedRoute(seed = 42)
        val restored = simulatedRoute(seed = 42)
        val anotherOrder = simulatedRoute(seed = 43)
        assertEquals(first, restored)
        assertNotEquals(first, anotherOrder)
        listOf(0f, .1f, .45f, .8f, 1f).forEach { progress ->
            val point = interpolateRoutePosition(first, progress)
            assertTrue(point.x in 0f..1f)
            assertTrue(point.y in 0f..1f)
        }
    }

    @Test
    fun tutorialRecordsAreExcludedFromWalletSummaryEvenIfCorruptedIntoHistory() {
        val tutorial = AlmostBuy(
            id = "tutorial",
            name = "Coffee and donut",
            amountCents = 3_100,
            category = "Food & delivery",
            trigger = "Tutorial",
            createdAtMillis = start,
            coolingUntilMillis = end,
            status = AlmostBuyStatus.SKIPPED,
            tutorialOnly = true
        )
        assertEquals(ProgressSummary(0, 0, 0, 0, 0), listOf(tutorial).progressSummary())
    }

    private fun stageAt(fraction: Double): GhostDeliveryState =
        ghostDeliverySnapshot(start + (duration * fraction).toLong(), start, end).state
}
