package com.example.ghostcart.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AlmostBuyModelsTest {
    @Test
    fun progressSummary_onlyCountsSkippedItemsAsMoneyKept() {
        val items = listOf(
            item("cooling", 10_000, AlmostBuyStatus.COOLING),
            item("skipped", 20_000, AlmostBuyStatus.SKIPPED),
            item("bought", 30_000, AlmostBuyStatus.BOUGHT_INTENTIONALLY)
        )

        val summary = items.progressSummary()

        assertEquals(60_000, summary.totalAlmostSpentCents)
        assertEquals(10_000, summary.activeCoolingCents)
        assertEquals(20_000, summary.confirmedMoneyKeptCents)
        assertEquals(1, summary.activeCount)
        assertEquals(2, summary.resolvedCount)
    }

    @Test
    fun progressSummary_emptyHistoryStartsAtZero() {
        assertEquals(
            ProgressSummary(0, 0, 0, 0, 0),
            emptyList<AlmostBuy>().progressSummary()
        )
    }

    private fun item(id: String, amount: Long, status: AlmostBuyStatus) = AlmostBuy(
        id = id,
        name = id,
        amountCents = amount,
        category = "Other",
        trigger = "Other",
        createdAtMillis = 1,
        coolingUntilMillis = 2,
        status = status
    )
}
