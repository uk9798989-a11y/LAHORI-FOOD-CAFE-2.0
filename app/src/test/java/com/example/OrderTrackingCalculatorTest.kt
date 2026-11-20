package com.example

import com.example.model.CafeOrder
import com.example.model.OrderStatus
import com.example.model.OrderTrackingCalculator
import com.example.model.OrderType
import com.example.model.TrackedOrderSnapshot
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class OrderTrackingCalculatorTest {

    private fun sampleOrder(
        id: String = "ord_99a1b2c3",
        status: OrderStatus = OrderStatus.Confirmed,
        orderType: OrderType = OrderType.DELIVERY,
        createdMillis: Long = 1_700_000_000_000L
    ): CafeOrder = CafeOrder(
        id = id,
        userId = "uid_123",
        customerName = "Ali",
        branchId = "branch_tmp",
        branchName = "Lahori Food Cafe TMP",
        orderType = orderType.name,
        status = status.name,
        subtotalPkr = 1450,
        deliveryFeePkr = 0,
        totalPkr = 1450,
        createdAt = Timestamp(Date(createdMillis)),
        updatedAt = Timestamp(Date(createdMillis))
    )

    @Test
    fun buildSummary_deliveryStages_haveExpectedProgressHeadlinesAndMilestones() {
        val created = 1_700_000_000_000L
        val now = created + 6 * 60_000L // 6 minutes elapsed

        val confirmed = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Confirmed, orderType = OrderType.DELIVERY, createdMillis = created),
            nowMillis = now
        )
        assertEquals("#A1B2C3", confirmed.shortOrderCode)
        assertEquals(0.25f, confirmed.progressFraction, 0.001f)
        assertEquals("ORDER CONFIRMED", confirmed.stageHeadline)
        assertEquals(6L, confirmed.elapsedMinutes)
        assertFalse(confirmed.isCompleted)
        assertFalse(confirmed.isPickup)
        assertEquals(4, confirmed.milestones.size)
        assertTrue(confirmed.milestones[0].isCurrent)
        assertTrue(confirmed.milestones[1].isUpcoming)
        assertEquals("Out for Delivery", confirmed.milestones[2].title)

        val preparing = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Preparing, orderType = OrderType.DELIVERY, createdMillis = created),
            nowMillis = now
        )
        assertEquals(0.55f, preparing.progressFraction, 0.001f)
        assertEquals("PREPARING IN KITCHEN", preparing.stageHeadline)
        assertTrue(preparing.milestones[0].isCompleted)
        assertTrue(preparing.milestones[1].isCurrent)
        assertTrue(preparing.milestones[2].isUpcoming)

        val dispatched = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Dispatched, orderType = OrderType.DELIVERY, createdMillis = created),
            nowMillis = now
        )
        assertEquals(0.85f, dispatched.progressFraction, 0.001f)
        assertEquals("OUT FOR DELIVERY", dispatched.stageHeadline)
        assertTrue(dispatched.milestones[0].isCompleted)
        assertTrue(dispatched.milestones[1].isCompleted)
        assertTrue(dispatched.milestones[2].isCurrent)
        assertTrue(dispatched.milestones[3].isUpcoming)
        assertTrue(dispatched.etaLabel.contains("min"))

        val delivered = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Delivered, orderType = OrderType.DELIVERY, createdMillis = created),
            nowMillis = now
        )
        assertEquals(1.0f, delivered.progressFraction, 0.001f)
        assertEquals("ORDER DELIVERED", delivered.stageHeadline)
        assertEquals("Arrived", delivered.etaLabel)
        assertTrue(delivered.isCompleted)
        assertTrue(delivered.milestones.all { it.isCompleted && !it.isCurrent && !it.isUpcoming })
    }

    @Test
    fun buildSummary_pickupStages_adaptLabelsAndReadyNowEta() {
        val created = 1_700_000_000_000L
        val now = created + 10 * 60_000L

        val ready = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Dispatched, orderType = OrderType.PICKUP, createdMillis = created),
            nowMillis = now
        )
        assertTrue(ready.isPickup)
        assertEquals("READY FOR PICKUP", ready.stageHeadline)
        assertEquals("Ready Now", ready.etaLabel)
        assertEquals("Ready for Pickup", ready.milestones[2].title)
        assertEquals("Picked Up", ready.milestones[3].title)

        val pickedUp = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Delivered, orderType = OrderType.PICKUP, createdMillis = created),
            nowMillis = now
        )
        assertTrue(pickedUp.isCompleted)
        assertEquals("ORDER PICKED UP", pickedUp.stageHeadline)
        assertEquals("Completed", pickedUp.etaLabel)
    }

    @Test
    fun formatRelativeSyncAge_formatsSecondsMinutesAndHours() {
        val base = 1_700_000_000_000L
        assertEquals("Synced just now", OrderTrackingCalculator.formatRelativeSyncAge(base, base))
        assertEquals("Synced just now", OrderTrackingCalculator.formatRelativeSyncAge(base, base + 4_000L))
        assertEquals("Synced 15s ago", OrderTrackingCalculator.formatRelativeSyncAge(base, base + 15_000L))
        assertEquals("Synced 3m ago", OrderTrackingCalculator.formatRelativeSyncAge(base, base + 180_000L))
        assertEquals("Synced 2h ago", OrderTrackingCalculator.formatRelativeSyncAge(base, base + 7_200_000L))
    }

    @Test
    fun trackedOrderSnapshot_preservesMetadataFlags() {
        val order = sampleOrder(status = OrderStatus.Preparing)
        val snapshot = TrackedOrderSnapshot(
            order = order,
            isFromCache = true,
            hasPendingWrites = false,
            lastSyncedAtMillis = 1_700_000_005_000L
        )
        assertEquals(order.id, snapshot.order.id)
        assertTrue(snapshot.isFromCache)
        assertFalse(snapshot.hasPendingWrites)
        assertEquals(1_700_000_005_000L, snapshot.lastSyncedAtMillis)
    }
}
