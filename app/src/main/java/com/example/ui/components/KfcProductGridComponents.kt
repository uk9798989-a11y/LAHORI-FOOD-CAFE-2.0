package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MenuItem
import com.example.model.SeedMenuData
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriRedDark
import com.example.ui.theme.LahoriWhite

/**
 * Computes the adaptive column count for the responsive food menu grid based on
 * the container's available width:
 * - Compact phones (< 600.dp): 2 columns (classic KFC mobile 2-up showcase)
 * - Medium / foldables / landscape (600.dp .. 899.dp): 3 columns
 * - Expanded tablets / desktop (>= 900.dp): 4 columns
 */
fun responsiveFoodGridColumns(maxWidth: Dp): Int = when {
    maxWidth >= 900.dp -> 4
    maxWidth >= 600.dp -> 3
    else -> 2
}

fun responsiveFoodGridSpacing(maxWidth: Dp): Dp = when {
    maxWidth >= 900.dp -> 16.dp
    maxWidth >= 600.dp -> 14.dp
    maxWidth < 360.dp -> 10.dp
    else -> 12.dp
}

/**
 * Signature 3-bar KFC-inspired heritage crimson stripe crest placed centered
 * along the top edge of product cards and quick-view sheets.
 */
@Composable
fun KfcHeritageTopStripes(
    modifier: Modifier = Modifier,
    stripeWidth: Dp = 8.dp,
    stripeHeight: Dp = 10.dp,
    gap: Dp = 5.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.Top
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(stripeWidth)
                    .height(stripeHeight)
                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(LahoriRed)
            )
        }
    }
}

/**
 * High-impact KFC-style product card designed for responsive Jetpack Compose grids
 * backed by live Firestore menu snapshots & per-branch price/availability overrides.
 */
@Composable
fun KfcProductCard(
    item: MenuItem,
    isFavorite: Boolean,
    cartQuantities: Map<String, Int>,
    onToggleFavorite: () -> Unit,
    onAddToBucket: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
    onDecrementFromBucket: ((String) -> Unit)? = null,
    onCardClick: ((MenuItem, Int) -> Unit)? = null
) {
    val sizeOptions = remember(item.id, item.name) { SeedMenuData.sizeOptionsFor(item) }
    var selectedSizeIndex by remember(item.id) { mutableIntStateOf(0) }
    val safeIndex = if (sizeOptions.isNotEmpty()) selectedSizeIndex.coerceIn(0, sizeOptions.lastIndex) else -1
    val selectedOption = if (safeIndex >= 0) sizeOptions[safeIndex] else null

    val effectivePricePkr = selectedOption?.pricePkr ?: item.pricePkr
    val resolvedCartItem = remember(item, selectedOption) {
        if (selectedOption != null) {
            val suffix = selectedOption.label.lowercase().replace(" ", "_")
            item.copy(
                id = "${item.id}_$suffix",
                name = "${item.name} (${selectedOption.label})",
                pricePkr = selectedOption.pricePkr
            )
        } else {
            item
        }
    }
    val quantityInCart = cartQuantities[resolvedCartItem.id] ?: 0

    val cardInteraction = remember { MutableInteractionSource() }
    val cardPressed by cardInteraction.collectIsPressedAsState()
    val cardHovered by cardInteraction.collectIsHoveredAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (cardPressed) 0.985f else if (cardHovered) 1.02f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "kfc_card_scale"
    )
    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "kfc_heart_scale"
    )
    val heartTint by animateColorAsState(
        targetValue = if (isFavorite) LahoriRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        label = "kfc_heart_tint"
    )
    val isHotBadge = remember(item.badgeText) {
        val lower = item.badgeText.lowercase()
        lower.contains("hot") || lower.contains("spicy") || lower.contains("best") || lower.contains("deal")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .shadow(
                elevation = if (cardHovered) 12.dp else 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = if (quantityInCart > 0) LahoriRed.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.22f)
            )
            .clickable(
                interactionSource = cardInteraction,
                indication = androidx.compose.material3.ripple(color = LahoriRed.copy(alpha = 0.14f)),
                onClick = {
                    if (onCardClick != null) {
                        onCardClick(item, safeIndex.coerceAtLeast(0))
                    } else if (item.available) {
                        onAddToBucket(resolvedCartItem)
                    }
                }
            )
            .testTag("menu_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (quantityInCart > 0) 1.5.dp else 1.dp,
            color = if (quantityInCart > 0) LahoriRed.copy(alpha = 0.75f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Signature 3-Stripe KFC Top Crest
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.TopCenter
            ) {
                KfcHeritageTopStripes(
                    stripeWidth = 9.dp,
                    stripeHeight = 8.dp,
                    gap = 5.dp
                )
            }

            // 2. Responsive Studio Food Image Stage + Overlapping Price Tag
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.22f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    FoodArtworkBox(
                        imageKey = item.imageKey,
                        imageUrl = item.imageUrl,
                        contentDescription = item.name,
                        itemId = item.id,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle bottom scrim for razor-sharp badge contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.18f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )

                    // Top-Left Promo / Hot Ribbon
                    if (item.badgeText.isNotBlank()) {
                        Surface(
                            color = if (isHotBadge) LahoriBlack.copy(alpha = 0.88f) else LahoriRedDark.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LahoriGold.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                if (isHotBadge) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = LahoriGold,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    text = item.badgeText.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.4.sp
                                    ),
                                    color = LahoriGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Top-Right Favorite Heart Button (48dp accessible touch target)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                    ) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("favorite_star_${item.id}")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = if (isFavorite) "Remove ${item.name} from favorites" else "Add ${item.name} to favorites",
                                        tint = heartTint,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer {
                                                scaleX = heartScale
                                                scaleY = heartScale
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Sold Out Dimming Stamp
                    if (!item.available) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.58f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LahoriBlack.copy(alpha = 0.92f),
                                border = BorderStroke(1.5.dp, LahoriRed)
                            ) {
                                Text(
                                    text = "SOLD OUT",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    ),
                                    color = LahoriWhite,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Overlapping KFC Crimson Price Tag (breaks the image bottom boundary for depth)
                Surface(
                    color = LahoriRed,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                    shadowElevation = 5.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 8.dp, y = 10.dp)
                ) {
                    Text(
                        text = formatPkr(effectivePricePkr),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = LahoriWhite,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Live Bucket Count Pill on Bottom-Right if already in cart
                if (quantityInCart > 0) {
                    Surface(
                        color = LahoriBlack,
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, LahoriGold),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-8).dp, y = 10.dp)
                    ) {
                        Text(
                            text = "${quantityInCart}x",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = LahoriGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // 3. Card Details & KFC Action Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = item.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.7.sp
                    ),
                    color = LahoriRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        lineHeight = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Interactive Size / Portion Selector
                if (sizeOptions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sizeOptions.forEachIndexed { idx, opt ->
                            val selected = idx == safeIndex
                            Surface(
                                onClick = { selectedSizeIndex = idx },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) LahoriRed else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (selected) null else BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = opt.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
                                        ),
                                        color = if (selected) LahoriWhite else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 4. Iconic KFC Crimson CTA Button OR Live Quantity Stepper
                AnimatedContent(
                    targetState = item.available && quantityInCart > 0 && onDecrementFromBucket != null,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "kfc_cart_cta_transition"
                ) { showStepper ->
                    if (showStepper) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LahoriRed,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { onDecrementFromBucket?.invoke(resolvedCartItem.id) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("decrement_bucket_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Remove,
                                        contentDescription = "Remove one ${item.name}",
                                        tint = LahoriWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = "$quantityInCart IN BUCKET",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = LahoriWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                IconButton(
                                    onClick = { onAddToBucket(resolvedCartItem) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("add_to_bucket_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add one more ${item.name}",
                                        tint = LahoriWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { onAddToBucket(resolvedCartItem) },
                            enabled = item.available,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LahoriRed,
                                contentColor = LahoriWhite,
                                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_to_bucket_${item.id}")
                        ) {
                            Text(
                                text = when {
                                    !item.available -> "SOLD OUT"
                                    quantityInCart > 0 -> "+ IN BUCKET ($quantityInCart)"
                                    else -> "+ ADD TO BUCKET"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.4.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated KFC-style pulsing skeleton card displayed while Firestore menu snapshots load.
 */
@Composable
fun KfcProductCardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "kfc_skeleton_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    val shimmerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = pulseAlpha)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("kfc_product_skeleton_card"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                KfcHeritageTopStripes(stripeWidth = 9.dp, stripeHeight = 8.dp, gap = 5.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .aspectRatio(1.22f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerColor)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(shimmerColor)
                )
            }
        }
    }
}

/**
 * Full-detail KFC-style Quick-View Modal Bottom Sheet opened when tapping a product card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KfcItemQuickViewSheet(
    item: MenuItem,
    initialSizeIndex: Int = 0,
    isFavorite: Boolean,
    cartQuantities: Map<String, Int>,
    onToggleFavorite: () -> Unit,
    onAddToBucket: (MenuItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sizeOptions = remember(item.id, item.name) { SeedMenuData.sizeOptionsFor(item) }
    var selectedSizeIndex by remember(item.id, initialSizeIndex) {
        mutableIntStateOf(
            if (sizeOptions.isNotEmpty()) initialSizeIndex.coerceIn(0, sizeOptions.lastIndex) else 0
        )
    }
    val selectedOption = sizeOptions.getOrNull(selectedSizeIndex)
    val effectivePricePkr = selectedOption?.pricePkr ?: item.pricePkr
    val resolvedCartItem = remember(item, selectedOption) {
        if (selectedOption != null) {
            val suffix = selectedOption.label.lowercase().replace(" ", "_")
            item.copy(
                id = "${item.id}_$suffix",
                name = "${item.name} (${selectedOption.label})",
                pricePkr = selectedOption.pricePkr
            )
        } else {
            item
        }
    }
    val quantityInCart = cartQuantities[resolvedCartItem.id] ?: 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .testTag("kfc_item_quick_view_sheet")
        ) {
            // Top KFC Heritage Crest + Close Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                KfcHeritageTopStripes(
                    stripeWidth = 12.dp,
                    stripeHeight = 12.dp,
                    gap = 6.dp,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .minimumInteractiveComponentSize()
                        .testTag("close_quick_view_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close item details",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Large Hero Food Photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(1.45f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                FoodArtworkBox(
                    imageKey = item.imageKey,
                    imageUrl = item.imageUrl,
                    contentDescription = item.name,
                    itemId = item.id,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.badgeText.isNotBlank()) {
                    Surface(
                        color = LahoriBlack.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LahoriGold),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = item.badgeText.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = LahoriGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                                tint = if (isFavorite) LahoriRed else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        ),
                        color = LahoriRed
                    )
                    Surface(
                        color = LahoriRed,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = formatPkr(effectivePricePkr),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = LahoriWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (sizeOptions.isNotEmpty()) {
                    Text(
                        text = "CHOOSE SIZE / PORTION",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizeOptions.forEachIndexed { idx, opt ->
                            val selected = idx == selectedSizeIndex
                            Surface(
                                onClick = { selectedSizeIndex = idx },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) LahoriRed else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (selected) null else BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = opt.label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (selected) LahoriWhite else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formatPkr(opt.pricePkr),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) LahoriWhite.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        onAddToBucket(resolvedCartItem)
                        onDismiss()
                    },
                    enabled = item.available,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LahoriRed,
                        contentColor = LahoriWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("quick_view_add_to_bucket")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!item.available) {
                            "CURRENTLY SOLD OUT"
                        } else if (quantityInCart > 0) {
                            "ADD ANOTHER ($quantityInCart IN BUCKET) • ${formatPkr(effectivePricePkr)}"
                        } else {
                            "ADD TO BUCKET • ${formatPkr(effectivePricePkr)}"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FoodGridEmptyCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = LahoriRed.copy(alpha = 0.12f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.RestaurantMenu,
                        contentDescription = null,
                        tint = LahoriRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, LahoriRed),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = LahoriRed
                    )
                }
            }
        }
    }
}

@Composable
fun FoodGridErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, LahoriRed.copy(alpha = 0.65f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = LahoriRed,
                modifier = Modifier.size(28.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Unable to sync live Firestore menu",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
