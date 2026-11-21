package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BranchMenuOverride
import com.example.model.DEFAULT_FREE_DELIVERY_MIN_ORDER_PKR
import com.example.model.DeliverySettings
import com.example.model.MenuItem
import com.example.ui.components.FoodArtworkBox
import com.example.ui.components.formatPkr
import com.example.ui.theme.LahoriBlack
import com.example.ui.theme.LahoriGold
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriSuccess
import com.example.ui.theme.LahoriWhite

/**
 * Branch Admin independent menu pricing, availability, and branch delivery fee manager.
 * Writes strictly to /branches/{branchId}/menu_overrides/{itemId} and /branches/{branchId}.deliveryFeePkr
 * so changes never affect other branches or the global catalog.
 */
@Composable
fun BranchMenuPriceManagerTab(
    branchId: String,
    branchName: String,
    branchLabel: String,
    currentDeliveryFeePkr: Long,
    freeDeliveryMinOrderPkr: Long = DEFAULT_FREE_DELIVERY_MIN_ORDER_PKR,
    globalMenuItems: List<MenuItem>,
    branchOverrides: List<BranchMenuOverride>,
    isSavingDeliveryFee: Boolean,
    isSavingOverride: Boolean,
    onUpdateBranchDeliveryFee: (Long, onDone: () -> Unit) -> Unit,
    onSaveBranchOverride: (itemId: String, pricePkr: Long, available: Boolean, onDone: () -> Unit) -> Unit,
    onResetBranchOverride: (itemId: String, onDone: () -> Unit) -> Unit
) {
    val overridesByItemId = remember(branchOverrides) {
        branchOverrides.associateBy { it.itemId }
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var editingItem by remember { mutableStateOf<MenuItem?>(null) }

    var deliveryFeeInput by remember(branchId, currentDeliveryFeePkr) {
        mutableStateOf(currentDeliveryFeePkr.toString())
    }
    var feeError by remember { mutableStateOf<String?>(null) }
    var feeSavedMsg by remember { mutableStateOf<String?>(null) }

    val categories = remember(globalMenuItems) {
        listOf("ALL") + globalMenuItems.map { it.category }.distinct()
    }

    val filteredItems = remember(globalMenuItems, searchQuery, selectedCategory) {
        globalMenuItems.filter { item ->
            val matchesCat = selectedCategory == "ALL" || item.category.equals(selectedCategory, ignoreCase = true)
            val q = searchQuery.trim()
            val matchesQuery = q.isBlank() ||
                item.name.contains(q, ignoreCase = true) ||
                item.category.contains(q, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    val customCount = remember(branchOverrides) { branchOverrides.size }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "branch_delivery_fee_card") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LahoriBlack,
                border = BorderStroke(1.5.dp, LahoriGold),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("branch_delivery_fee_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LahoriGold.copy(alpha = 0.18f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeliveryDining,
                                contentDescription = null,
                                tint = LahoriGold,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${branchLabel.uppercase()} DELIVERY FEE",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = LahoriGold
                            )
                            Text(
                                text = "Applies only to $branchName orders under ${formatPkr(freeDeliveryMinOrderPkr)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LahoriWhite.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LahoriSuccess.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, LahoriSuccess.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "FREE DELIVERY THRESHOLD: Orders >= ${formatPkr(freeDeliveryMinOrderPkr)} automatically get Rs. 0 FREE delivery (managed globally by Super Admin).",
                            style = MaterialTheme.typography.labelSmall,
                            color = LahoriWhite,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = deliveryFeeInput,
                            onValueChange = { raw ->
                                deliveryFeeInput = raw.filter { it.isDigit() }
                                feeError = null
                                feeSavedMsg = null
                            },
                            label = { Text("Branch Delivery Fee (PKR)", color = LahoriGold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("branch_delivery_fee_input")
                        )
                        Button(
                            onClick = {
                                val parsed = deliveryFeeInput.toLongOrNull()
                                if (parsed == null || parsed < 0L || parsed > 10_000L) {
                                    feeError = "Enter a valid fee between 0 and 10,000 PKR."
                                } else {
                                    feeError = null
                                    onUpdateBranchDeliveryFee(parsed) {
                                        feeSavedMsg = "Saved ${formatPkr(parsed)} delivery fee for $branchLabel"
                                    }
                                }
                            },
                            enabled = !isSavingDeliveryFee,
                            colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("save_branch_delivery_fee_btn")
                        ) {
                            if (isSavingDeliveryFee) {
                                CircularProgressIndicator(color = LahoriWhite, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Text("SAVE FEE", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    if (feeError != null) {
                        Text(feeError!!, style = MaterialTheme.typography.bodySmall, color = LahoriRed)
                    }
                    if (feeSavedMsg != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LahoriSuccess, modifier = Modifier.size(16.dp))
                            Text(feeSavedMsg!!, style = MaterialTheme.typography.bodySmall, color = LahoriSuccess)
                        }
                    }
                }
            }
        }

        item(key = "branch_menu_header") {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LahoriRed.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, LahoriRed.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${branchLabel.uppercase()} MENU & PRICE OVERRIDES",
                            style = MaterialTheme.typography.labelLarge,
                            color = LahoriRed
                        )
                        Text(
                            text = "Edit prices or toggle availability strictly for your branch. Other branches keep their own prices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = LahoriBlack) {
                        Text(
                            text = "$customCount CUSTOM",
                            style = MaterialTheme.typography.labelSmall,
                            color = LahoriGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item(key = "branch_menu_search_and_filter") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search menu items to adjust price...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("branch_menu_search_input")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { "cat_$it" }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LahoriRed,
                                selectedLabelColor = LahoriWhite
                            )
                        )
                    }
                }
            }
        }

        items(filteredItems, key = { it.id }) { item ->
            val override = overridesByItemId[item.id]
            val effectivePrice = override?.pricePkr ?: item.pricePkr
            val effectiveAvailable = override?.available ?: item.available
            val hasCustomPrice = override != null && override.pricePkr != item.pricePkr
            val hasAnyOverride = override != null

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (hasAnyOverride) 1.5.dp else 1.dp,
                    color = if (hasAnyOverride) LahoriGold else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("branch_menu_item_${item.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FoodArtworkBox(
                            imageKey = item.imageKey,
                            imageUrl = item.imageUrl,
                            contentDescription = item.name,
                            compact = true,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (hasCustomPrice) {
                                    Surface(color = LahoriGold, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "BRANCH PRICE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = LahoriBlack,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = formatPkr(effectivePrice),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = LahoriRed
                                )
                                if (hasCustomPrice) {
                                    Text(
                                        text = "Global: ${formatPkr(item.pricePkr)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "${item.category} • ${if (effectiveAvailable) "Available at $branchLabel" else "Marked Sold Out at $branchLabel"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (effectiveAvailable) LahoriSuccess else LahoriRed
                            )
                        }

                        Switch(
                            checked = effectiveAvailable,
                            onCheckedChange = { newAvailable ->
                                onSaveBranchOverride(item.id, effectivePrice, newAvailable) {}
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = LahoriSuccess),
                            modifier = Modifier.testTag("branch_item_avail_${item.id}")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { editingItem = item },
                            colors = ButtonDefaults.buttonColors(containerColor = LahoriBlack, contentColor = LahoriGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("branch_edit_price_${item.id}")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("EDIT BRANCH PRICE", style = MaterialTheme.typography.labelMedium)
                        }

                        if (hasAnyOverride) {
                            OutlinedButton(
                                onClick = { onResetBranchOverride(item.id) {} },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("branch_reset_price_${item.id}")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("RESET", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { target ->
        val existingOverride = overridesByItemId[target.id]
        BranchPriceEditDialog(
            item = target,
            branchLabel = branchLabel,
            initialPricePkr = existingOverride?.pricePkr ?: target.pricePkr,
            initialAvailable = existingOverride?.available ?: target.available,
            isSaving = isSavingOverride,
            onDismiss = { editingItem = null },
            onSave = { newPrice, newAvail ->
                onSaveBranchOverride(target.id, newPrice, newAvail) {
                    editingItem = null
                }
            }
        )
    }
}

@Composable
private fun BranchPriceEditDialog(
    item: MenuItem,
    branchLabel: String,
    initialPricePkr: Long,
    initialAvailable: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (pricePkr: Long, available: Boolean) -> Unit
) {
    var priceText by remember(item.id) { mutableStateOf(initialPricePkr.toString()) }
    var available by remember(item.id) { mutableStateOf(initialAvailable) }
    var errorText by remember(item.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "Branch Price Override",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.name} • $branchLabel only",
                    style = MaterialTheme.typography.bodySmall,
                    color = LahoriRed
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Global Catalog Price: ${formatPkr(item.pricePkr)}. Setting a custom price here only updates customers ordering from $branchLabel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { raw ->
                        priceText = raw.filter { it.isDigit() }
                        errorText = null
                    },
                    label = { Text("$branchLabel Price (PKR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("branch_override_price_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available at $branchLabel", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = available,
                        onCheckedChange = { available = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = LahoriSuccess)
                    )
                }
                if (errorText != null) {
                    Text(errorText!!, style = MaterialTheme.typography.bodySmall, color = LahoriRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = priceText.toLongOrNull()
                    if (parsed == null || parsed <= 0L || parsed > 1_000_000L) {
                        errorText = "Enter a valid price in PKR (1 – 1,000,000)."
                    } else {
                        onSave(parsed, available)
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                modifier = Modifier.testTag("branch_override_save_btn")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = LahoriWhite, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text("SAVE PRICE")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

/**
 * Super Admin Global Delivery Policy Card inside the Branches tab.
 * Controls /app_settings/delivery (freeDeliveryMinOrderPkr default Rs. 800, defaultDeliveryFeePkr default Rs. 150).
 */
@Composable
fun SuperAdminDeliverySettingsCard(
    deliverySettings: DeliverySettings,
    isSaving: Boolean,
    onSave: (freeDeliveryMinOrderPkr: Long, defaultDeliveryFeePkr: Long, onDone: () -> Unit) -> Unit
) {
    var minOrderInput by remember(deliverySettings.freeDeliveryMinOrderPkr) {
        mutableStateOf(deliverySettings.freeDeliveryMinOrderPkr.toString())
    }
    var defaultFeeInput by remember(deliverySettings.defaultDeliveryFeePkr) {
        mutableStateOf(deliverySettings.defaultDeliveryFeePkr.toString())
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var savedMsg by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LahoriBlack,
        border = BorderStroke(1.5.dp, LahoriGold),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("super_admin_delivery_settings_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(shape = RoundedCornerShape(10.dp), color = LahoriGold.copy(alpha = 0.2f)) {
                    Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = null,
                        tint = LahoriGold,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GLOBAL FREE DELIVERY POLICY",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = LahoriGold
                    )
                    Text(
                        text = "Orders >= ${formatPkr(deliverySettings.freeDeliveryMinOrderPkr)} unlock FREE delivery across all branches. Individual branches can customize their fee below this threshold.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LahoriWhite.copy(alpha = 0.82f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minOrderInput,
                    onValueChange = { raw ->
                        minOrderInput = raw.filter { it.isDigit() }
                        errorMsg = null
                        savedMsg = null
                    },
                    label = { Text("Free Delivery Min (PKR)", color = LahoriGold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("super_free_delivery_min_input")
                )
                OutlinedTextField(
                    value = defaultFeeInput,
                    onValueChange = { raw ->
                        defaultFeeInput = raw.filter { it.isDigit() }
                        errorMsg = null
                        savedMsg = null
                    },
                    label = { Text("Default Fee (PKR)", color = LahoriGold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("super_default_delivery_fee_input")
                )
            }

            if (errorMsg != null) {
                Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = LahoriRed)
            }
            if (savedMsg != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LahoriSuccess, modifier = Modifier.size(16.dp))
                    Text(savedMsg!!, style = MaterialTheme.typography.bodySmall, color = LahoriSuccess)
                }
            }

            Button(
                onClick = {
                    val minOrder = minOrderInput.toLongOrNull()
                    val defaultFee = defaultFeeInput.toLongOrNull()
                    when {
                        minOrder == null || minOrder < 0L || minOrder > 100_000L ->
                            errorMsg = "Enter a valid Free Delivery minimum order (0 – 100,000 PKR)."
                        defaultFee == null || defaultFee < 0L || defaultFee > 10_000L ->
                            errorMsg = "Enter a valid default delivery fee (0 – 10,000 PKR)."
                        else -> {
                            errorMsg = null
                            onSave(minOrder, defaultFee) {
                                savedMsg = "Updated policy: FREE delivery on orders >= ${formatPkr(minOrder)} (default fee ${formatPkr(defaultFee)})"
                            }
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = LahoriRed, contentColor = LahoriWhite),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_global_delivery_settings_btn")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = LahoriWhite, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SAVING POLICY...", style = MaterialTheme.typography.labelLarge)
                } else {
                    Text("SAVE GLOBAL DELIVERY POLICY", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
