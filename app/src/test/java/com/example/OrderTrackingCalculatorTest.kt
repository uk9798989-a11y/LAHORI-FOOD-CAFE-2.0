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
        status: OrderStatus = OrderStatus.Pending,
        orderType: OrderType = OrderType.DELIVERY,
        createdMillis: Long = 1_700_000_000_000L,
        rejectionReason: String = ""
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
        rejectionReason = rejectionReason,
        createdAt = Timestamp(Date(createdMillis)),
        updatedAt = Timestamp(Date(createdMillis))
    )

    @Test
    fun buildSummary_deliveryStages_haveExpectedProgressHeadlinesAndMilestones() {
        val created = 1_700_000_000_000L
        val now = created + 6 * 60_000L // 6 minutes elapsed

        val pending = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Pending, orderType = OrderType.DELIVERY, createdMillis = created),
            nowMillis = now
        )
        assertEquals("#A1B2C3", pending.shortOrderCode)
        assertEquals(0.14f, pending.progressFraction, 0.001f)
        assertEquals("WAITING FOR RESTAURANT CONFIRMATION", pending.stageHeadline)
        assertFalse(pending.isCompleted)
        assertFalse(pending.isRejected)
        assertEquals(6, pending.milestones.size)
        assertTrue(pending.milestones[0].isCurrent)
        assertTrue(pending.milestones[1].isUpcoming)

        val confirmed = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Confirmed, orderType = OrderType.DELIVERY, createdMillis = created),
            nowMillis = now
        )
        assertEquals(0.32f, confirmed.progressFraction, 0.001f)
        assertEquals("ORDER CONFIRMED", confirmed.stageHeadline)
        assertEquals(6L, confirmed.elapsedMinutes)
        assertFalse(confirmed.isCompleted)
        assertFalse(confirmed.isPickup)
        assertTrue(confirmed.milestones[0].isCompleted)
        assertTrue(confirmed.milestones[1].isCurrent)
        assertTrue(confirmed.milestones[2].isUpcoming)
        assertEquals("Out for Delivery", confirmed.milestones[4].title)

        val preparing = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Preparing, orderType = OrderType.DELIVERY, createdMillis = created),
            nowMillis = now
        )
        assertEquals(0.54f, preparing.progressFraction, 0.001f)
        assertEquals("YOUR ORDER IS BEING PREPARED", preparing.stageHeadline)
        assertTrue(preparing.milestones[1].isCompleted)
        assertTrue(preparing.milestones[2].isCurrent)
        assertTrue(preparing.milestones[3].isUpcoming)

        val ready = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Ready, orderType = OrderType.DELIVERY, createdMillis = created),
            nowMillis = now
        )
        assertEquals(0.72f, ready.progressFraction, 0.001f)
        assertEquals("YOUR ORDER IS READY", ready.stageHeadline)
        assertTrue(ready.milestones[2].isCompleted)
        assertTrue(ready.milestones[3].isCurrent)

        val outForDelivery = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.OutForDelivery, orderType = OrderType.DELIVERY, createdMillis = created),
            nowMillis = now
        )
        assertEquals(0.88f, outForDelivery.progressFraction, 0.001f)
        assertEquals("YOUR ORDER IS ON THE WAY", outForDelivery.stageHeadline)
        assertTrue(outForDelivery.milestones[3].isCompleted)
        assertTrue(outForDelivery.milestones[4].isCurrent)
        assertTrue(outForDelivery.milestones[5].isUpcoming)
        assertTrue(outForDelivery.etaLabel.contains("min"))

        val delivered = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Delivered, orderType = OrderType.DELIVERY, createdMillis = created),
            nowMillis = now
        )
        assertEquals(1.0f, delivered.progressFraction, 0.001f)
        assertEquals("ORDER DELIVERED", delivered.stageHeadline)
        assertEquals("Arrived", delivered.etaLabel)
        assertTrue(delivered.isCompleted)
        assertTrue(delivered.milestones.all { it.isCompleted && !it.isCurrent && !it.isUpcoming })

        val rejected = OrderTrackingCalculator.buildSummary(
            sampleOrder(
                status = OrderStatus.Rejected,
                orderType = OrderType.DELIVERY,
                createdMillis = created,
                rejectionReason = "Kitchen closed"
            ),
            nowMillis = now
        )
        assertEquals(0.0f, rejected.progressFraction, 0.001f)
        assertEquals("ORDER REJECTED", rejected.stageHeadline)
        assertEquals("Rejected", rejected.etaLabel)
        assertTrue(rejected.isRejected)
        assertEquals("Kitchen closed", rejected.rejectionReason)
        assertTrue(rejected.stageSubtitle.contains("Kitchen closed"))
        assertTrue(rejected.milestones.all { it.isUpcoming && !it.isCompleted && !it.isCurrent })
    }

    @Test
    fun buildSummary_pickupStages_adaptLabelsAndReadyNowEta() {
        val created = 1_700_000_000_000L
        val now = created + 10 * 60_000L

        val ready = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Ready, orderType = OrderType.PICKUP, createdMillis = created),
            nowMillis = now
        )
        assertTrue(ready.isPickup)
        assertEquals("YOUR ORDER IS READY", ready.stageHeadline)
        assertEquals("Ready Now", ready.etaLabel)

        val readyForPickup = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.OutForDelivery, orderType = OrderType.PICKUP, createdMillis = created),
            nowMillis = now
        )
        assertEquals("READY FOR PICKUP", readyForPickup.stageHeadline)
        assertEquals("Ready Now", readyForPickup.etaLabel)
        assertEquals("Ready for Pickup", readyForPickup.milestones[4].title)
        assertEquals("Picked Up", readyForPickup.milestones[5].title)

        val pickedUp = OrderTrackingCalculator.buildSummary(
            sampleOrder(status = OrderStatus.Delivered, orderType = OrderType.PICKUP, createdMillis = created),
            nowMillis = now
        )
        assertTrue(pickedUp.isCompleted)
        assertEquals("ORDER PICKED UP", pickedUp.stageHeadline)
        assertEquals("Completed", pickedUp.etaLabel)
    }

    @Test
    fun legacyDispatchedStatus_mapsCleanlyToOutForDelivery() {
        assertEquals(OrderStatus.OutForDelivery, OrderStatus.fromString("Dispatched"))
        assertEquals(OrderStatus.OutForDelivery, OrderStatus.fromString("out_for_delivery"))
        assertEquals(OrderStatus.Pending, OrderStatus.fromString("unknown_status"))
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
