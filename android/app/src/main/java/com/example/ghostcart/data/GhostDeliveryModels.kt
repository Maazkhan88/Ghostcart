package com.example.ghostcart.data

import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * One source of truth for the simulated delivery lifecycle.  UI, WorkManager and
 * restoration all call this model instead of maintaining independent counters.
 */
enum class GhostDeliveryState {
    PLACED,
    PREPARING,
    RIDER_PICKING_UP,
    OUT_FOR_DELIVERY,
    RIDER_NEARBY,
    DELIVERED,
    RESOLVED_SKIPPED,
    RESOLVED_BUY_FROM_SOURCE,
    RESOLVED_BOUGHT_ALREADY,
    RESTARTED,
    CANCELLED;

    val isTerminal: Boolean
        get() = this in setOf(
            DELIVERED,
            RESOLVED_SKIPPED,
            RESOLVED_BUY_FROM_SOURCE,
            RESOLVED_BOUGHT_ALREADY,
            CANCELLED
        )
}

enum class GhostOrderResolution {
    SKIPPED,
    BUY_FROM_SOURCE,
    BOUGHT_ALREADY
}

data class GhostDeliveryDuration(
    val label: String,
    val durationMillis: Long
)

val marketplaceGhostDeliveryDurations = listOf(
    GhostDeliveryDuration("15 minutes", TimeUnit.MINUTES.toMillis(15)),
    GhostDeliveryDuration("1 hour", TimeUnit.HOURS.toMillis(1)),
    GhostDeliveryDuration("24 hours", TimeUnit.HOURS.toMillis(24)),
    GhostDeliveryDuration("1 week", TimeUnit.DAYS.toMillis(7))
)

val foodGhostDeliveryDurations = listOf(
    GhostDeliveryDuration("15 minutes", TimeUnit.MINUTES.toMillis(15)),
    GhostDeliveryDuration("30 minutes", TimeUnit.MINUTES.toMillis(30)),
    GhostDeliveryDuration("1 hour", TimeUnit.HOURS.toMillis(1)),
    GhostDeliveryDuration("24 hours", TimeUnit.HOURS.toMillis(24)),
    GhostDeliveryDuration("1 week", TimeUnit.DAYS.toMillis(7))
)

data class GhostDeliverySnapshot(
    val state: GhostDeliveryState,
    val progress: Float,
    val elapsedMillis: Long,
    val remainingMillis: Long
)

private data class StageThreshold(val fraction: Double, val state: GhostDeliveryState)

private val activeThresholds = listOf(
    StageThreshold(0.00, GhostDeliveryState.PLACED),
    StageThreshold(0.10, GhostDeliveryState.PREPARING),
    StageThreshold(0.25, GhostDeliveryState.RIDER_PICKING_UP),
    StageThreshold(0.45, GhostDeliveryState.OUT_FOR_DELIVERY),
    StageThreshold(0.80, GhostDeliveryState.RIDER_NEARBY),
    StageThreshold(1.00, GhostDeliveryState.DELIVERED)
)

fun ghostDeliverySnapshot(
    nowMillis: Long,
    startMillis: Long,
    endMillis: Long,
    persistedState: GhostDeliveryState? = null
): GhostDeliverySnapshot {
    if (persistedState != null && persistedState in setOf(
            GhostDeliveryState.RESOLVED_SKIPPED,
            GhostDeliveryState.RESOLVED_BUY_FROM_SOURCE,
            GhostDeliveryState.RESOLVED_BOUGHT_ALREADY,
            GhostDeliveryState.CANCELLED
        )
    ) {
        return GhostDeliverySnapshot(persistedState, 1f, 0L, 0L)
    }

    val safeStart = startMillis.coerceAtLeast(0L)
    val safeEnd = endMillis.coerceAtLeast(safeStart + 1L)
    val duration = safeEnd - safeStart
    val elapsed = (nowMillis - safeStart).coerceIn(0L, duration)
    val fraction = (elapsed.toDouble() / duration.toDouble()).coerceIn(0.0, 1.0)
    val stage = activeThresholds.last { fraction >= it.fraction }.state
    return GhostDeliverySnapshot(
        state = stage,
        progress = fraction.toFloat(),
        elapsedMillis = elapsed,
        remainingMillis = (safeEnd - nowMillis).coerceAtLeast(0L)
    )
}

data class GhostStageSchedule(
    val state: GhostDeliveryState,
    val triggerAtMillis: Long,
    val fraction: Double
)

fun ghostStageSchedule(startMillis: Long, endMillis: Long): List<GhostStageSchedule> {
    val safeEnd = endMillis.coerceAtLeast(startMillis + 1L)
    val duration = safeEnd - startMillis
    return activeThresholds.map { threshold ->
        GhostStageSchedule(
            state = threshold.state,
            triggerAtMillis = startMillis + (duration * threshold.fraction).roundToLong(),
            fraction = threshold.fraction
        )
    }
}

data class SimulatedRoutePoint(val x: Float, val y: Float)

/** Deterministic fictional route; it never reads or changes device location. */
fun simulatedRoute(seed: Long, pointCount: Int = 7): List<SimulatedRoutePoint> {
    val random = Random(seed)
    return List(pointCount.coerceAtLeast(2)) { index ->
        val x = index.toFloat() / (pointCount - 1).coerceAtLeast(1)
        val baseline = 0.62f - (x * 0.24f)
        SimulatedRoutePoint(x, (baseline + random.nextFloat() * 0.24f - 0.12f).coerceIn(0.12f, 0.88f))
    }
}

fun interpolateRoutePosition(points: List<SimulatedRoutePoint>, progress: Float): SimulatedRoutePoint {
    if (points.isEmpty()) return SimulatedRoutePoint(0f, 0.5f)
    if (points.size == 1) return points.first()
    val bounded = progress.coerceIn(0f, 1f)
    val scaled = bounded * (points.lastIndex)
    val fromIndex = scaled.toInt().coerceAtMost(points.lastIndex - 1)
    val segmentProgress = scaled - fromIndex
    val from = points[fromIndex]
    val to = points[fromIndex + 1]
    return SimulatedRoutePoint(
        x = from.x + (to.x - from.x) * segmentProgress,
        y = from.y + (to.y - from.y) * segmentProgress
    )
}

fun isFoodDeliveryCategory(category: String): Boolean {
    val normalized = category.lowercase()
    return listOf("food", "coffee", "drink", "delivery", "healthy", "restaurant")
        .any(normalized::contains)
}
