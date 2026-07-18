package com.example.ghostcart.ui.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryTimelineTest {
    @Test
    fun followsDeviceClockAcrossEveryDeliveryStep() {
        val placedAt = 1_000_000L
        val minute = 60_000L

        assertEquals(0, deliveryStepAt(placedAt, placedAt, 1))
        assertEquals(1, deliveryStepAt(placedAt + minute, placedAt, 1))
        assertEquals(2, deliveryStepAt(placedAt + 2 * minute, placedAt, 1))
        assertEquals(3, deliveryStepAt(placedAt + 3 * minute, placedAt, 1))
        assertEquals(4, deliveryStepAt(placedAt + 4 * minute, placedAt, 1))
        assertEquals(4, deliveryStepAt(placedAt + 20 * minute, placedAt, 1))
    }

    @Test
    fun missingOrderHasNoActiveDelivery() {
        assertEquals(-1, deliveryStepAt(nowMillis = 5_000L, placedAtMillis = 0L, intervalMinutes = 5))
    }
}
