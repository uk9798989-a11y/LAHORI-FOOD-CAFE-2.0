package com.example.model

import com.google.firebase.Timestamp

data class TrackedOrderSnapshot(
    val order: CafeOrder,
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false,
    val lastSyncedAtMillis: Long = System.currentTimeMillis()
)

data class OrderTrackingMilestone(
    val status: OrderStatus,
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
    val isUpcoming: Boolean
)

data class OrderTrackingSummary(
    val orderId: String,
    val shortOrderCode: String,
    val status: OrderStatus,
    val isPickup: Boolean,
    val isCompleted: Boolean,
    val isRejected: Boolean,
    val rejectionReason: String,
    val progressFraction: Float,
    val stageHeadline: String,
    val stageSubtitle: String,
    val etaLabel: String,
    val etaSubtext: String,
    val elapsedMinutes: Long,
    val milestones: List<OrderTrackingMilestone>
)

object OrderTrackingCalculator {

    fun buildSummary(
        order: CafeOrder,
        nowMillis: Long = System.currentTimeMillis()
    ): OrderTrackingSummary {
        val status = OrderStatus.fromString(order.status)
        val isPickup = order.orderType.equals(OrderType.PICKUP.name, ignoreCase = true)
        val isCompleted = status == OrderStatus.Delivered
        val isRejected = status == OrderStatus.Rejected
        val shortCode = "#${order.id.takeLast(6).uppercase().ifBlank { "ORDER" }}"

        val createdMillis = (order.createdAt ?: order.updatedAt)?.toDate()?.time ?: nowMillis
        val elapsedMinutes = ((nowMillis - createdMillis).coerceAtLeast(0L)) / 60_000L

        val progressFraction = when (status) {
            OrderStatus.Pending -> 0.14f
            OrderStatus.Confirmed -> 0.32f
            OrderStatus.Preparing -> 0.54f
            OrderStatus.Ready -> 0.72f
            OrderStatus.OutForDelivery -> 0.88f
            OrderStatus.Delivered -> 1.0f
            OrderStatus.Rejected -> 0.0f
        }

        val stageHeadline = when (status) {
            OrderStatus.Pending -> "WAITING FOR RESTAURANT CONFIRMATION"
            OrderStatus.Confirmed -> "ORDER CONFIRMED"
            OrderStatus.Preparing -> "YOUR ORDER IS BEING PREPARED"
            OrderStatus.Ready -> "YOUR ORDER IS READY"
            OrderStatus.OutForDelivery -> if (isPickup) "READY FOR PICKUP" else "YOUR ORDER IS ON THE WAY"
            OrderStatus.Delivered -> if (isPickup) "ORDER PICKED UP" else "ORDER DELIVERED"
            OrderStatus.Rejected -> "ORDER REJECTED"
        }

        val branchDisplay = order.branchName.ifBlank { "Lahori Food Cafe" }
        val stageSubtitle = when (status) {
            OrderStatus.Pending ->
                "Order $shortCode sent to $branchDisplay. Waiting for branch confirmation."
            OrderStatus.Confirmed ->
                "Order Confirmed! $branchDisplay has accepted your order and queued it for the kitchen."
            OrderStatus.Preparing ->
                "Chefs are freshly cooking, grilling, and packing your Lahori favorites right now."
            OrderStatus.Ready -> if (isPickup) {
                "Your order is hot, freshly packed, and ready at the pickup counter!"
            } else {
                "Your order is freshly packed and ready for rider pickup at $branchDisplay."
            }
            OrderStatus.OutForDelivery -> if (isPickup) {
                "Your order is packed piping hot and waiting at the branch pickup counter!"
            } else {
                "Your rider has left $branchDisplay and is heading straight to your doorstep."
            }
            OrderStatus.Delivered -> if (isPickup) {
                "Picked up and completed. Enjoy your Lahori Food Cafe meal!"
            } else {
                "Delivered to your doorstep! Enjoy your Lahori Food Cafe feast."
            }
            OrderStatus.Rejected -> if (order.rejectionReason.isNotBlank()) {
                "Rejected by $branchDisplay: ${order.rejectionReason}"
            } else {
                "Unfortunately, $branchDisplay could not accept this order. Please contact the branch or try again."
            }
        }

        val (etaLabel, etaSubtext) = computeEta(status = status, isPickup = isPickup, elapsedMinutes = elapsedMinutes)

        val steps = OrderStatus.trackingSteps
        val currentIndex = if (isRejected) -1 else steps.indexOf(status)
        val milestones = steps.mapIndexed { idx, step ->
            val stepCompleted = !isRejected && (idx < currentIndex || (idx == currentIndex && isCompleted))
            val stepCurrent = !isRejected && idx == currentIndex && !isCompleted
            val stepUpcoming = isRejected || idx > currentIndex

            OrderTrackingMilestone(
                status = step,
                title = when (step) {
                    OrderStatus.Pending -> "Waiting for Confirmation"
                    OrderStatus.Confirmed -> "Order Confirmed"
                    OrderStatus.Preparing -> "Preparing in Kitchen"
                    OrderStatus.Ready -> "Order Ready"
                    OrderStatus.OutForDelivery -> if (isPickup) "Ready for Pickup" else "Out for Delivery"
                    OrderStatus.Delivered -> if (isPickup) "Picked Up" else "Delivered"
                    OrderStatus.Rejected -> "Order Rejected"
                },
                shortLabel = when (step) {
                    OrderStatus.Pending -> "Pending"
                    OrderStatus.Confirmed -> "Confirmed"
                    OrderStatus.Preparing -> "Preparing"
                    OrderStatus.Ready -> "Ready"
                    OrderStatus.OutForDelivery -> if (isPickup) "Pickup" else "On Way"
                    OrderStatus.Delivered -> if (isPickup) "Picked Up" else "Delivered"
                    OrderStatus.Rejected -> "Rejected"
                },
                subtitle = when (step) {
                    OrderStatus.Pending -> "Sent to branch manager for immediate review and acceptance."
                    OrderStatus.Confirmed -> "Accepted by the branch manager and queued for kitchen prep."
                    OrderStatus.Preparing -> "Freshly frying, baking, and sealing your order hot."
                    OrderStatus.Ready -> if (isPickup) {
                        "Packed hot and ready at the branch counter."
                    } else {
                        "Sealed hot and awaiting rider handoff."
                    }
                    OrderStatus.OutForDelivery -> if (isPickup) {
                        "Hot and ready for immediate collection at the counter."
                    } else {
                        "Rider picked up your package and is on the delivery route."
                    }
                    OrderStatus.Delivered -> if (isPickup) {
                        "Collected by customer from the branch."
                    } else {
                        val feeLabel = if (order.deliveryFeePkr == 0L) "FREE Delivery" else "Rs. ${order.deliveryFeePkr} Delivery"
                        "Handed over at your delivery address ($feeLabel)."
                    }
                    OrderStatus.Rejected -> "Order was rejected by the branch."
                },
                isCompleted = stepCompleted,
                isCurrent = stepCurrent,
                isUpcoming = stepUpcoming
            )
        }

        return OrderTrackingSummary(
            orderId = order.id,
            shortOrderCode = shortCode,
            status = status,
            isPickup = isPickup,
            isCompleted = isCompleted,
            isRejected = isRejected,
            rejectionReason = order.rejectionReason,
            progressFraction = progressFraction,
            stageHeadline = stageHeadline,
            stageSubtitle = stageSubtitle,
            etaLabel = etaLabel,
            etaSubtext = etaSubtext,
            elapsedMinutes = elapsedMinutes,
            milestones = milestones
        )
    }

    private fun computeEta(
        status: OrderStatus,
        isPickup: Boolean,
        elapsedMinutes: Long
    ): Pair<String, String> {
        return when (status) {
            OrderStatus.Pending -> "1–3 min" to "Waiting for branch confirmation"
            OrderStatus.Confirmed -> {
                if (isPickup) {
                    val minM = (18L - elapsedMinutes).coerceIn(10L, 20L)
                    val maxM = (minM + 7L).coerceAtMost(30L)
                    "$minM–$maxM min" to "Est. pickup ready time"
                } else {
                    val minM = (30L - elapsedMinutes).coerceIn(15L, 35L)
                    val maxM = (minM + 10L).coerceAtMost(45L)
                    "$minM–$maxM min" to "Est. delivery arrival"
                }
            }
            OrderStatus.Preparing -> {
                if (isPickup) {
                    val minM = (12L - elapsedMinutes / 2).coerceIn(5L, 15L)
                    val maxM = (minM + 6L).coerceAtMost(22L)
                    "$minM–$maxM min" to "Cooking in kitchen"
                } else {
                    val minM = (20L - elapsedMinutes / 2).coerceIn(10L, 25L)
                    val maxM = (minM + 8L).coerceAtMost(35L)
                    "$minM–$maxM min" to "Preparing & packing"
                }
            }
            OrderStatus.Ready -> {
                if (isPickup) {
                    "Ready Now" to "Waiting at pickup counter"
                } else {
                    "5–12 min" to "Ready for rider pickup"
                }
            }
            OrderStatus.OutForDelivery -> {
                if (isPickup) {
                    "Ready Now" to "Waiting at pickup counter"
                } else {
                    val minM = (10L - (elapsedMinutes / 4)).coerceIn(3L, 12L)
                    val maxM = (minM + 5L).coerceAtMost(18L)
                    "$minM–$maxM min" to "Rider en route to you"
                }
            }
            OrderStatus.Delivered -> {
                if (isPickup) {
                    "Completed" to "Picked up from branch"
                } else {
                    "Arrived" to "Delivered to doorstep"
                }
            }
            OrderStatus.Rejected -> "Rejected" to "Order was not accepted"
        }
    }

    fun formatRelativeSyncAge(lastSyncedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffSeconds = ((nowMillis - lastSyncedAtMillis).coerceAtLeast(0L)) / 1000L
        return when {
            diffSeconds < 5L -> "Synced just now"
            diffSeconds < 60L -> "Synced ${diffSeconds}s ago"
            diffSeconds < 3600L -> "Synced ${diffSeconds / 60L}m ago"
            else -> "Synced ${diffSeconds / 3600L}h ago"
        }
    }

    fun latestUpdatedMillis(order: CafeOrder): Long? =
        (order.updatedAt ?: order.createdAt)?.let { ts: Timestamp -> ts.toDate().time }
}
