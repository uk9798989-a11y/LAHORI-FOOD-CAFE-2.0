package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MenuItem
import com.example.model.SeedMenuData
import com.example.model.UiState
import com.example.ui.components.FoodGridEmptyCard
import com.example.ui.components.FoodGridErrorCard
import com.example.ui.components.KfcItemQuickViewSheet
import com.example.ui.components.KfcProductCard
import com.example.ui.components.KfcProductCardSkeleton
import com.example.ui.components.responsiveFoodGridColumns
import com.example.ui.components.responsiveFoodGridSpacing
import com.example.ui.theme.LahoriRed
import com.example.ui.theme.LahoriWhite

@Composable
fun MenuScreen(
    menuItems: List<MenuItem>,
    favoriteIds: Set<String>,
    cartQuantities: Map<String, Int>,
    searchQuery: String,
    onToggleFavorite: (String) -> Unit,
    onAddToBucket: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
    menuState: UiState<List<MenuItem>> = UiState.Success(menuItems),
    onDecrementFromBucket: ((String) -> Unit)? = null
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var quickViewItem by remember { mutableStateOf<MenuItem?>(null) }
    var quickViewSizeIndex by remember { mutableIntStateOf(0) }

    val effectiveItems = when (menuState) {
        is UiState.Success -> menuState.data
        else -> menuItems
    }

    val dynamicCategories = remember(effectiveItems) {
        val extras = effectiveItems
            .map { it.category.trim().uppercase() }
            .filter { cat -> cat.isNotBlank() && SeedMenuData.categories.none { it.equals(cat, ignoreCase = true) } }
            .distinct()
        SeedMenuData.categories + extras
    }

    val filtered = remember(effectiveItems, selectedCategory, favoritesOnly, searchQuery) {
        effectiveItems.filter { item ->
            val catOk = selectedCategory == "ALL" || item.category.equals(selectedCategory, ignoreCase = true)
            val favOk = !favoritesOnly || favoriteIds.contains(item.id)
            val searchOk = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
            catOk && favOk && searchOk
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val columns = responsiveFoodGridColumns(maxWidth)
        val gridSpacing = responsiveFoodGridSpacing(maxWidth)

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .testTag("menu_responsive_grid"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 116.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            verticalArrangement = Arrangement.spacedBy(gridSpacing)
        ) {
            item(
                key = "menu_header",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "FULL MENU",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${filtered.size}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Live Firestore menu in PKR • Freshly cooked to order",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = !favoritesOnly },
                            label = {
                                Text(
                                    text = "Favorites",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (favoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (favoritesOnly) LahoriRed else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                                selectedLabelColor = MaterialTheme.colorScheme.background
                            ),
                            modifier = Modifier.testTag("menu_favorites_filter_chip")
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("menu_category_chips_row")
                    ) {
                        items(dynamicCategories, key = { it }) { category ->
                            val selected = selectedCategory.equals(category, ignoreCase = true)
                            Surface(
                                onClick = { selectedCategory = category },
                                shape = CircleShape,
                                color = if (selected) LahoriRed else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shadowElevation = if (selected) 4.dp else 0.dp,
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("menu_cat_${category.lowercase().replace(" ", "_")}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
                                        ),
                                        color = if (selected) LahoriWhite else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (menuState is UiState.Error) {
                item(
                    key = "menu_error_banner",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    FoodGridErrorCard(message = menuState.message)
                }
            }

            if (menuState is UiState.Loading && filtered.isEmpty()) {
                items(
                    count = columns * 3,
                    key = { idx -> "menu_skeleton_$idx" }
                ) {
                    KfcProductCardSkeleton()
                }
            } else if (filtered.isEmpty()) {
                item(
                    key = "empty_menu",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    FoodGridEmptyCard(
                        title = if (favoritesOnly) "No favorite items yet" else "No menu items match",
                        subtitle = if (favoritesOnly) {
                            "Tap the heart icon on any food card to save your go-to meals here."
                        } else {
                            "Try switching to ALL categories or clearing your search."
                        },
                        actionLabel = if (selectedCategory != "ALL" || favoritesOnly) "SHOW ALL ITEMS" else null,
                        onActionClick = {
                            selectedCategory = "ALL"
                            favoritesOnly = false
                        }
                    )
                }
            } else {
                items(
                    items = filtered,
                    key = { it.id }
                ) { item ->
                    KfcProductCard(
                        item = item,
                        isFavorite = favoriteIds.contains(item.id),
                        cartQuantities = cartQuantities,
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onAddToBucket = onAddToBucket,
                        onDecrementFromBucket = onDecrementFromBucket,
                        onCardClick = { clicked, sizeIdx ->
                            quickViewItem = clicked
                            quickViewSizeIndex = sizeIdx
                        }
                    )
                }
            }
        }
    }

    val inspectedItem = quickViewItem
    if (inspectedItem != null) {
        KfcItemQuickViewSheet(
            item = inspectedItem,
            initialSizeIndex = quickViewSizeIndex,
            isFavorite = favoriteIds.contains(inspectedItem.id),
            cartQuantities = cartQuantities,
            onToggleFavorite = { onToggleFavorite(inspectedItem.id) },
            onAddToBucket = onAddToBucket,
            onDismiss = { quickViewItem = null }
        )
    }
}
