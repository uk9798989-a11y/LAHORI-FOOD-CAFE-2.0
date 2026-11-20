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
        val shortCode = "#${order.id.takeLast(6).uppercase().ifBlank { "ORDER" }}"

        val createdMillis = (order.createdAt ?: order.updatedAt)?.toDate()?.time ?: nowMillis
        val elapsedMinutes = ((nowMillis - createdMillis).coerceAtLeast(0L)) / 60_000L

        val progressFraction = when (status) {
            OrderStatus.Confirmed -> 0.25f
            OrderStatus.Preparing -> 0.55f
            OrderStatus.Dispatched -> 0.85f
            OrderStatus.Delivered -> 1.0f
        }

        val stageHeadline = when (status) {
            OrderStatus.Confirmed -> "ORDER CONFIRMED"
            OrderStatus.Preparing -> "PREPARING IN KITCHEN"
            OrderStatus.Dispatched -> if (isPickup) "READY FOR PICKUP" else "OUT FOR DELIVERY"
            OrderStatus.Delivered -> if (isPickup) "ORDER PICKED UP" else "ORDER DELIVERED"
        }

        val stageSubtitle = when (status) {
            OrderStatus.Confirmed ->
                "Your order $shortCode is confirmed at ${order.branchName.ifBlank { "Lahori Food Cafe" }} and queued for the kitchen."
            OrderStatus.Preparing ->
                "Chefs are freshly cooking, grilling, and packing your Lahori favorites right now."
            OrderStatus.Dispatched -> if (isPickup) {
                "Your order is packed piping hot and waiting at the branch pickup counter!"
            } else {
                "Your rider has left ${order.branchName.ifBlank { "the branch" }} and is heading to your doorstep."
            }
            OrderStatus.Delivered -> if (isPickup) {
                "Picked up and completed. Enjoy your Lahori Food Cafe meal!"
            } else {
                "Delivered to your doorstep! Enjoy your Lahori Food Cafe feast."
            }
        }

        val (etaLabel, etaSubtext) = computeEta(status = status, isPickup = isPickup, elapsedMinutes = elapsedMinutes)

        val steps = OrderStatus.entries
        val currentIndex = steps.indexOf(status)
        val milestones = steps.mapIndexed { idx, step ->
            val stepCompleted = idx < currentIndex || (idx == currentIndex && isCompleted)
            val stepCurrent = idx == currentIndex && !isCompleted
            val stepUpcoming = idx > currentIndex

            OrderTrackingMilestone(
                status = step,
                title = when (step) {
                    OrderStatus.Confirmed -> "Order Confirmed"
                    OrderStatus.Preparing -> "Preparing in Kitchen"
                    OrderStatus.Dispatched -> if (isPickup) "Ready for Pickup" else "Out for Delivery"
                    OrderStatus.Delivered -> if (isPickup) "Picked Up" else "Delivered"
                },
                shortLabel = when (step) {
                    OrderStatus.Confirmed -> "Confirmed"
                    OrderStatus.Preparing -> "Preparing"
                    OrderStatus.Dispatched -> if (isPickup) "Ready" else "On Way"
                    OrderStatus.Delivered -> if (isPickup) "Picked Up" else "Delivered"
                },
                subtitle = when (step) {
                    OrderStatus.Confirmed -> "Order received and verified on the branch kitchen screen."
                    OrderStatus.Preparing -> "Freshly frying, baking, and sealing your order hot."
                    OrderStatus.Dispatched -> if (isPickup) {
                        "Hot and ready for immediate collection at the counter."
                    } else {
                        "Rider picked up your package and is on the delivery route."
                    }
                    OrderStatus.Delivered -> if (isPickup) {
                        "Collected by customer from the branch."
                    } else {
                        "Handed over at your delivery address — FREE Delivery."
                    }
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
            OrderStatus.Confirmed -> {
                if (isPickup) {
                    val minM = (18L - elapsedMinutes).coerceIn(10L, 20L)
                    val maxM = (minM + 7L).coerceAtMost(30L)
                    "$minM–$maxM min" to "Est. pickup ready time"
                } else {
                    val minM = (30L - elapsedMinutes).coerceIn(15L, 35L)
                    val maxM = (minM + 10L).coerceAtMost(45L)
                    "$minM–$maxM min" to "Est. free delivery arrival"
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
            OrderStatus.Dispatched -> {
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
