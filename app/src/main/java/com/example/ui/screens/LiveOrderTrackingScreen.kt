package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BranchDirectory
import com.example.model.CafeBranch
import com.example.model.CafeOrder
import com.example.model.OrderReview
import com.example.model.OrderStatus
import com.example.model.OrderTrackingCalculator
import com.example.model.OrderTrackingSummary
import com.example.model.TrackedOrderSnapshot
import com.example.model.UiState
import com.example.ui.components.LiveRouteProgressCanvas
import com.example.ui.components.LiveSnapshotStatusChip
import com.example.ui.components.formatPkr
import com.example.ui.components.stageIconFor
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriRedDark
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveOrderTrackingScreen(
    trackedSnapshotState: UiState<TrackedOrderSnapshot>,
    allActiveOrders: List<CafeOrder>,
    reviewsByOrderId: Map<String, OrderReview>,
    onSelectTrackedOrder: (String) -> Unit,
    onRateOrder: (CafeOrder) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    branches: List<CafeBranch> = BranchDirectory.defaultBranches,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Track live snapshot transitions while the user watches the dashboard
    var previousObservedOrderId by remember { mutableStateOf<String?>(null) }
    var previousObservedStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var liveTransitionBanner by remember { mutableStateOf<String?>(null) }

    val currentSnapshot = (trackedSnapshotState as? UiState.Success)?.data
    val currentStatus = currentSnapshot?.order?.let { OrderStatus.fromString(it.status) }

    LaunchedEffect(currentSnapshot?.order?.id, currentStatus) {
        val snap = currentSnapshot ?: return@LaunchedEffect
        val statusNow = OrderStatus.fromString(snap.order.status)
        if (previousObservedOrderId == snap.order.id &&
            previousObservedStatus != null &&
            previousObservedStatus != statusNow
        ) {
            val isPickup = snap.order.orderType.equals("PICKUP", ignoreCase = true)
            val label = when (statusNow) {
                OrderStatus.Confirmed -> "Order Confirmed"
                OrderStatus.Preparing -> "Preparing in Kitchen"
                OrderStatus.Dispatched -> if (isPickup) "Ready for Pickup" else "Out for Delivery"
                OrderStatus.Delivered -> if (isPickup) "Picked Up" else "Delivered"
            }
            liveTransitionBanner = "Live update received: $label"
            delay(5000L)
            liveTransitionBanner = null
        }
        previousObservedOrderId = snap.order.id
        previousObservedStatus = statusNow
    }

    // Periodic tick to keep elapsed minutes & relative sync age fresh while active
    var nowTickMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(currentSnapshot?.order?.id, currentStatus) {
        nowTickMillis = System.currentTimeMillis()
        while (currentStatus != null && currentStatus != OrderStatus.Delivered) {
            delay(5_000L)
            nowTickMillis = System.currentTimeMillis()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("live_tracker_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Orders",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "ORDER TRACKING",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real-time Firestore snapshot dashboard",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (currentSnapshot != null) {
                            LiveSnapshotStatusChip(
                                isCompleted = OrderStatus.fromString(currentSnapshot.order.status) == OrderStatus.Delivered,
                                isFromCache = currentSnapshot.isFromCache
                            )
                        }
                    }

                    if (allActiveOrders.size > 1 && currentSnapshot != null) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allActiveOrders, key = { it.id }) { candidate ->
                                val isCurrent = candidate.id == currentSnapshot.order.id
                                val candidateStatus = OrderStatus.fromString(candidate.status)
                                FilterChip(
                                    selected = isCurrent,
                                    onClick = { onSelectTrackedOrder(candidate.id) },
                                    label = {
                                        Text(
                                            text = "#${candidate.id.takeLast(6).uppercase()} • ${candidateStatus.displayTitle}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LahoriRed,
                                        selectedLabelColor = LahoriWhite
                                    ),
                                    modifier = Modifier.testTag("tracker_order_chip_${candidate.id}")
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (trackedSnapshotState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = LahoriRed)
                            Text(
                                text = "Connecting to real-time Firestore order stream...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, LahoriRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Unable to Track Order",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = LahoriRed
                                )
                                Text(
                                    text = trackedSnapshotState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onBack,
                                    colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("BACK TO ORDERS")
                                }
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val snapshot = trackedSnapshotState.data
                    val order = snapshot.order
                    val summary = remember(order, nowTickMillis) {
                        OrderTrackingCalculator.buildSummary(order, nowTickMillis)
                    }
                    val syncAgeText = remember(snapshot.lastSyncedAtMillis, nowTickMillis) {
                        OrderTrackingCalculator.formatRelativeSyncAge(snapshot.lastSyncedAtMillis, nowTickMillis)
                    }
                    val branch = remember(order.branchId, branches) {
                        BranchDirectory.byId(order.branchId, branches)
                    }
                    val existingReview = reviewsByOrderId[order.id]
                    val mapsUrl = remember(order, branch) {
                        if (summary.isPickup) {
                            branch.mapUrl.ifBlank {
                                if (branch.hasCoordinates()) {
                                    "https://www.google.com/maps/search/?api=1&query=${branch.latitude},${branch.longitude}"
                                } else {
                                    "https://www.google.com/maps/search/?api=1&query=${Uri.encode(branch.shortAddress)}"
                                }
                            }
                        } else {
                            order.deliveryGoogleMapsUrl().ifBlank { branch.mapUrl }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .testTag("live_order_tracking_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 0. Animated Live Snapshot Change Banner
                        item(key = "live_transition_flash") {
                            AnimatedVisibility(
                                visible = liveTransitionBanner != null,
                                enter = fadeIn(tween(220)) + expandVertically(),
                                exit = fadeOut(tween(220)) + shrinkVertically()
                            ) {
                                liveTransitionBanner?.let { bannerText ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = LahoriSuccess.copy(alpha = 0.16f),
                                        border = BorderStroke(1.2.dp, LahoriSuccess),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("live_status_transition_banner")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Bolt,
                                                contentDescription = null,
                                                tint = LahoriSuccess,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = bannerText,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = if (isDark) Color(0xFF4ADE80) else LahoriSuccess
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 1. Hero Real-Time Status Dashboard Card with Route Canvas & ETA
                        item(key = "hero_live_status") {
                            LiveOrderHeroCard(
                                order = order,
                                summary = summary,
                                syncAgeText = syncAgeText,
                                isFromCache = snapshot.isFromCache
                            )
                        }

                        // 2. Quick Actions & Live Metrics Strip
                        item(key = "live_metrics_and_actions") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricPillCard(
                                    label = "ELAPSED",
                                    value = if (summary.elapsedMinutes <= 0L) "Just now" else "${summary.elapsedMinutes} min",
                                    icon = Icons.Filled.Schedule,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricPillCard(
                                    label = "DELIVERY FEE",
                                    value = if (order.deliveryFeePkr == 0L) "FREE (Rs. 0)" else formatPkr(order.deliveryFeePkr),
                                    icon = Icons.Filled.LocalShipping,
                                    accentColor = if (order.deliveryFeePkr == 0L) LahoriSuccess else LahoriRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // 3. Step-by-Step Vertical Real-Time Milestone Timeline
                        item(key = "vertical_milestone_timeline") {
                            LiveMilestoneTimelineCard(
                                summary = summary,
                                order = order,
                                isDark = isDark
                            )
                        }

                        // 4. Branch & Fulfillment Details with Open in Google Maps action
                        item(key = "branch_fulfillment_card") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Storefront,
                                                contentDescription = null,
                                                tint = LahoriRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = if (summary.isPickup) "PICKUP BRANCH INFO" else "BRANCH & DELIVERY INFO",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (order.deliveryFeePkr == 0L && !summary.isPickup) {
                                            Surface(
                                                color = LahoriSuccess.copy(alpha = 0.14f),
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    text = "FREE DELIVERY",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 9.sp
                                                    ),
                                                    color = if (isDark) Color(0xFF4ADE80) else LahoriSuccess,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = LahoriRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                text = "${order.orderType} • ${order.branchName}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = order.deliveryAddress,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (order.hasDeliveryLocation()) {
                                                Text(
                                                    text = "GPS Pin: ${order.formattedCoordinates()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isDark) LahoriGold else LahoriRed
                                                )
                                            }
                                            Text(
                                                text = "Recipient: ${order.customerName} (${order.customerPhone.ifBlank { "Verified account" }})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (mapsUrl.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("No map app available to open location")
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, LahoriRed.copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("tracker_open_map_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Map,
                                                contentDescription = null,
                                                tint = LahoriRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (summary.isPickup) "OPEN BRANCH ON GOOGLE MAPS" else "VIEW DELIVERY LOCATION ON MAP",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (isDark) LahoriWhite else LahoriRed
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Itemized Order Summary
                        item(key = "order_items_receipt") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ORDER SUMMARY (${summary.shortOrderCode})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    order.items.forEach { line ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${line.quantity}x ${line.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = formatPkr(line.lineTotalPkr),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatPkr(order.subtotalPkr), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Delivery Charges", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = if (order.deliveryFeePkr == 0L) "Rs. 0 (FREE)" else formatPkr(order.deliveryFeePkr),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (order.deliveryFeePkr == 0L) {
                                                if (isDark) Color(0xFF4ADE80) else LahoriSuccess
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                    if (order.discountPkr > 0L) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Promo (${order.promoCode.ifBlank { "DISCOUNT" }})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDark) Color(0xFF4ADE80) else LahoriSuccess
                                            )
                                            Text(
                                                text = "- ${formatPkr(order.discountPkr)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isDark) Color(0xFF4ADE80) else LahoriSuccess
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "TOTAL",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = formatPkr(order.totalPkr),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            color = LahoriRed
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Real-Time Firestore Snapshot Telemetry Footer
                        item(key = "firestore_snapshot_telemetry") {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("firestore_snapshot_telemetry_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Sync,
                                        contentDescription = null,
                                        tint = if (snapshot.isFromCache) LahoriGold else LahoriSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (snapshot.isFromCache) {
                                                "Showing cached snapshot — reconnecting for live updates"
                                            } else {
                                                "Connected to live Firestore snapshot stream"
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$syncAgeText • Last branch update ${formatOrderDate(order.updatedAt ?: order.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // 7. Delivered Rating Card
                        if (summary.status == OrderStatus.Delivered) {
                            item(key = "delivered_review_section") {
                                if (existingReview != null) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, LahoriGold),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Filled.Star, contentDescription = null, tint = LahoriGold)
                                                    Text(
                                                        text = "RATED ${existingReview.rating}/5",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                TextButton(onClick = { onRateOrder(order) }) {
                                                    Text("EDIT", color = LahoriRed)
                                                }
                                            }
                                            if (existingReview.comment.isNotBlank()) {
                                                Text(
                                                    text = "\"${existingReview.comment}\"",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { onRateOrder(order) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("tracker_rate_order_button")
                                    ) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = LahoriGold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "RATE YOUR DELIVERED ORDER",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveSyncBadge(isActive: Boolean) {
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val badgeTint = if (isActive) LahoriSuccess else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = CircleShape,
        color = badgeTint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, badgeTint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(if (isActive) pulseScale else 1f)
                    .alpha(if (isActive) pulseAlpha else 1f)
                    .clip(CircleShape)
                    .background(badgeTint)
            )
            Text(
                text = if (isActive) "LIVE" else "COMPLETED",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                ),
                color = badgeTint
            )
        }
    }
}

@Composable
private fun MetricPillCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = LahoriRed
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LiveOrderHeroCard(
    order: CafeOrder,
    summary: OrderTrackingSummary,
    syncAgeText: String,
    isFromCache: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.progressFraction,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "order_progress"
    )
    val stageIcon = stageIconFor(summary.status, summary.isPickup)

    val infiniteTransition = rememberInfiniteTransition(label = "hero_beacon")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (summary.isCompleted) 1.0f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_scale"
    )

    val gradientColors = if (summary.isCompleted) {
        listOf(Color(0xFF0A3A1E), Color(0xFF146636), LahoriSuccess)
    } else {
        listOf(LahoriBlack, Color(0xFF4F0714), LahoriRedDark, LahoriRed)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, if (summary.isCompleted) Color(0xFF4ADE80).copy(alpha = 0.6f) else LahoriGold.copy(alpha = 0.7f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_order_hero_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = LahoriGold, shape = CircleShape) {
                                Text(
                                    text = "ORDER ${summary.shortOrderCode}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = LahoriBlack,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = LahoriWhite.copy(alpha = 0.16f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = summary.etaLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = LahoriWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        AnimatedContent(
                            targetState = summary.stageHeadline,
                            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(200)) },
                            label = "hero_stage_headline"
                        ) { headline ->
                            Text(
                                text = headline,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = LahoriWhite
                            )
                        }
                        Text(
                            text = summary.stageSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = LahoriWhite.copy(alpha = 0.88f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        shape = CircleShape,
                        color = LahoriWhite.copy(alpha = 0.16f),
                        border = BorderStroke(1.5.dp, LahoriGold.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .size(64.dp)
                            .scale(iconScale)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = stageIcon,
                                contentDescription = summary.stageHeadline,
                                tint = LahoriWhite,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Custom Canvas 4-stage live progress route
                LiveRouteProgressCanvas(summary = summary)

                // Percentage bar & sync metadata
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = LahoriGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${summary.etaSubtext} • ${if (isFromCache) "Cached" else syncAgeText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LahoriWhite.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = LahoriGold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = LahoriGold,
                        trackColor = LahoriWhite.copy(alpha = 0.25f),
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveMilestoneTimelineCard(
    summary: OrderTrackingSummary,
    order: CafeOrder,
    isDark: Boolean
) {
    val pulseTransition = rememberInfiniteTransition(label = "timeline_pulse")
    val activeHaloScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.26f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "active_halo_scale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_milestone_timeline")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE ORDER TIMELINE",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Placed ${formatOrderDate(order.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            val milestones = summary.milestones
            val currentIndex = milestones.indexOfFirst { it.isCurrent }.let {
                if (it >= 0) it else if (summary.isCompleted) milestones.lastIndex else 0
            }

            milestones.forEachIndexed { index, milestone ->
                val reached = milestone.isCompleted || milestone.isCurrent
                val stepIcon = if (milestone.isCompleted && !milestone.isCurrent) {
                    Icons.Filled.Check
                } else {
                    stageIconFor(milestone.status, summary.isPickup)
                }

                val circleColor by animateColorAsState(
                    targetValue = when {
                        milestone.isCompleted -> LahoriSuccess
                        milestone.isCurrent -> LahoriRed
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    label = "step_color_$index"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                            if (milestone.isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .scale(activeHaloScale)
                                        .clip(CircleShape)
                                        .background(LahoriRed.copy(alpha = 0.22f))
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = circleColor,
                                border = if (reached) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = stepIcon,
                                        contentDescription = milestone.title,
                                        tint = if (reached) LahoriWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (index < milestones.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(34.dp)
                                    .background(
                                        if (index < currentIndex || summary.isCompleted) LahoriSuccess
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp, bottom = if (index < milestones.lastIndex) 12.dp else 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = milestone.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (reached) FontWeight.ExtraBold else FontWeight.Medium
                                ),
                                color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            when {
                                milestone.isCurrent -> {
                                    Surface(
                                        color = LahoriRed.copy(alpha = 0.14f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "IN PROGRESS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = if (isDark) Color(0xFFFF8599) else LahoriRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                milestone.isCompleted -> {
                                    Surface(
                                        color = LahoriSuccess.copy(alpha = 0.14f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "DONE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = if (isDark) Color(0xFF4ADE80) else LahoriSuccess,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = milestone.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
