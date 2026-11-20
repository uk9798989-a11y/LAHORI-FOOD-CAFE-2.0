package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CafeOrder
import com.example.model.OrderStatus
import com.example.model.OrderTrackingCalculator
import com.example.model.OrderTrackingSummary
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriRedDark
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWarning
import com.example.ui.theme.LahoriWhite

fun stageIconFor(status: OrderStatus, isPickup: Boolean): ImageVector = when (status) {
    OrderStatus.Confirmed -> Icons.Filled.ReceiptLong
    OrderStatus.Preparing -> Icons.Filled.LocalFireDepartment
    OrderStatus.Dispatched -> if (isPickup) Icons.Filled.ShoppingBag else Icons.Filled.DeliveryDining
    OrderStatus.Delivered -> Icons.Filled.CheckCircle
}

@Composable
fun LiveSnapshotStatusChip(
    isCompleted: Boolean,
    isFromCache: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "snapshot_beacon")
    val pulseScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "snapshot_scale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "snapshot_alpha"
    )

    val (tint, label, icon) = when {
        isCompleted -> Triple(LahoriSuccess, "COMPLETED", Icons.Filled.CheckCircle)
        isFromCache -> Triple(LahoriWarning, "OFFLINE CACHE", Icons.Filled.CloudOff)
        else -> Triple(Color(0xFF22C55E), "LIVE SYNC", Icons.Filled.CloudDone)
    }

    Surface(
        shape = CircleShape,
        color = tint.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.55f)),
        modifier = modifier.testTag("live_snapshot_status_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!isCompleted && !isFromCache) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(tint)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                ),
                color = tint
            )
        }
    }
}

@Composable
fun LiveRouteProgressCanvas(
    summary: OrderTrackingSummary,
    modifier: Modifier = Modifier
) {
    val steps = summary.milestones
    val activeIdx = steps.indexOfFirst { it.isCurrent }.let {
        if (it >= 0) it else if (summary.isCompleted) steps.lastIndex else 0
    }
    val segmentRatio = if (steps.size <= 1) 1f else activeIdx.toFloat() / (steps.size - 1).toFloat()
    val animatedFill by animateFloatAsState(
        targetValue = segmentRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "route_fill"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "route_pulse")
    val haloRadiusFactor by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_radius"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_alpha"
    )
    val beaconSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_sweep"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("live_route_progress_canvas")
        ) {
            val horizontalPad = 18.dp.toPx()
            val trackY = size.height * 0.5f
            val startX = horizontalPad
            val endX = (size.width - horizontalPad).coerceAtLeast(startX + 1f)
            val trackSpan = endX - startX

            // Base dashed upcoming track
            drawLine(
                color = LahoriWhite.copy(alpha = 0.24f),
                start = Offset(startX, trackY),
                end = Offset(endX, trackY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )

            // Active filled track
            val filledEndX = startX + (trackSpan * animatedFill)
            if (filledEndX > startX) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = if (summary.isCompleted) {
                            listOf(LahoriGold, Color(0xFF4ADE80))
                        } else {
                            listOf(LahoriGold, Color(0xFFFFE57F), LahoriWhite)
                        },
                        startX = startX,
                        endX = filledEndX
                    ),
                    start = Offset(startX, trackY),
                    end = Offset(filledEndX, trackY),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Traveling energy dot toward next milestone while active
            if (!summary.isCompleted && activeIdx < steps.lastIndex) {
                val stepWidth = trackSpan / (steps.size - 1).coerceAtLeast(1)
                val segStart = startX + (activeIdx * stepWidth)
                val dotX = segStart + (stepWidth * beaconSweep)
                drawCircle(
                    color = LahoriGold.copy(alpha = 0.9f),
                    radius = 4.5.dp.toPx(),
                    center = Offset(dotX, trackY)
                )
            }

            // Stage milestone nodes
            val count = steps.size.coerceAtLeast(2)
            steps.forEachIndexed { idx, milestone ->
                val nodeX = startX + (trackSpan * idx / (count - 1))
                val center = Offset(nodeX, trackY)

                if (milestone.isCurrent) {
                    drawCircle(
                        color = LahoriGold.copy(alpha = haloAlpha),
                        radius = 10.dp.toPx() * haloRadiusFactor,
                        center = center
                    )
                    drawCircle(
                        color = LahoriGold,
                        radius = 9.5.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = LahoriBlack,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                } else if (milestone.isCompleted) {
                    drawCircle(
                        color = if (summary.isCompleted) Color(0xFF4ADE80) else LahoriGold,
                        radius = 7.5.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = LahoriBlack,
                        radius = 2.8.dp.toPx(),
                        center = center
                    )
                } else {
                    drawCircle(
                        color = Color(0xFF2B2B2B),
                        radius = 6.5.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = LahoriWhite.copy(alpha = 0.45f),
                        radius = 2.5.dp.toPx(),
                        center = center
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEach { milestone ->
                val labelColor = when {
                    milestone.isCurrent -> LahoriGold
                    milestone.isCompleted -> LahoriWhite
                    else -> LahoriWhite.copy(alpha = 0.55f)
                }
                Text(
                    text = milestone.shortLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = if (milestone.isCurrent || milestone.isCompleted) FontWeight.ExtraBold else FontWeight.Medium
                    ),
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ActiveOrderMiniTrackerStrip(
    order: CafeOrder,
    onTrackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = remember(order) { OrderTrackingCalculator.buildSummary(order) }
    val animatedProgress by animateFloatAsState(
        targetValue = summary.progressFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "mini_tracker_progress"
    )
    val icon = stageIconFor(summary.status, summary.isPickup)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LahoriBlack,
        border = BorderStroke(1.5.dp, LahoriGold.copy(alpha = 0.8f)),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTrackClick)
            .testTag("explore_live_order_strip")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(LahoriBlack, Color(0xFF3E0611), LahoriRedDark)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = LahoriGold,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = summary.stageHeadline,
                                tint = LahoriBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "LIVE ${summary.shortOrderCode}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                ),
                                color = LahoriGold
                            )
                            Text(
                                text = "• ${summary.etaLabel}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = LahoriWhite.copy(alpha = 0.8f)
                            )
                        }
                        AnimatedContent(
                            targetState = summary.stageHeadline,
                            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                            label = "strip_headline"
                        ) { headline ->
                            Text(
                                text = headline,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = LahoriWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${order.branchName} • Tap for real-time tracker",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LahoriWhite.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = LahoriRed,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "TRACK",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = LahoriWhite
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Live Tracker",
                            tint = LahoriWhite,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                color = LahoriGold,
                trackColor = LahoriWhite.copy(alpha = 0.22f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
fun CurrentOrderDashboardCard(
    order: CafeOrder,
    activeOrderCount: Int,
    onOpenFullDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = remember(order) { OrderTrackingCalculator.buildSummary(order) }
    val icon = stageIconFor(summary.status, summary.isPickup)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = LahoriBlack,
        border = BorderStroke(1.5.dp, LahoriGold.copy(alpha = 0.75f)),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("current_order_dashboard_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(LahoriBlack, Color(0xFF470713), LahoriRedDark)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = LahoriGold, shape = CircleShape) {
                        Text(
                            text = "CURRENT ORDER ${summary.shortOrderCode}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            ),
                            color = LahoriBlack,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (activeOrderCount > 1) {
                        Surface(color = LahoriWhite.copy(alpha = 0.16f), shape = CircleShape) {
                            Text(
                                text = "+${activeOrderCount - 1} more",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = LahoriWhite,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                LiveSnapshotStatusChip(isCompleted = summary.isCompleted, isFromCache = false)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AnimatedContent(
                        targetState = summary.stageHeadline,
                        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) },
                        label = "dashboard_card_stage"
                    ) { headline ->
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            ),
                            color = LahoriWhite
                        )
                    }
                    Text(
                        text = summary.stageSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LahoriWhite.copy(alpha = 0.86f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LahoriWhite.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = LahoriGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = summary.etaLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = LahoriWhite
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = LahoriGold,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "ETA",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = LahoriGold
                            )
                        }
                    }
                }
            }

            LiveRouteProgressCanvas(summary = summary)

            Button(
                onClick = onOpenFullDashboard,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LahoriGold,
                    contentColor = LahoriBlack
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_full_tracking_dashboard_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OPEN LIVE TRACKING DASHBOARD",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
