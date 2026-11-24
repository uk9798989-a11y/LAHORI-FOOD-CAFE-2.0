package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TakeoutDining
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.MenuImageStorageHelper
import com.example.model.MenuItem
import com.example.ui.theme.LahoriWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun formatPkr(amount: Long): String {
    val formatted = "%,d".format(amount)
    return "Rs. $formatted"
}

fun resolveMenuDrawableName(imageKey: String, itemId: String = ""): String? {
    val cleanKey = imageKey.trim().lowercase()
    if (cleanKey.startsWith("menu_")) return cleanKey

    val normalizedId = itemId.trim().lowercase().removePrefix("seed_")
    if (normalizedId.isNotBlank()) {
        val byItemId: String? = when (normalizedId) {
            "lfc_pasta_lahori_special" -> "menu_pasta_lahori_sp"
            "lfc_pasta_flaming" -> "menu_pasta_flaming"
            "lfc_pasta_crunchy" -> "menu_pasta_crunchy"
            "lfc_pasta_creamy" -> "menu_pasta_creamy"
            "lfc_deal_1" -> "menu_deal_1"
            "lfc_deal_2" -> "menu_deal_2"
            "lfc_deal_3" -> "menu_deal_3"
            "lfc_deal_4" -> "menu_deal_4"
            "lfc_deal_5" -> "menu_deal_5"
            "lfc_deal_6" -> "menu_deal_6"
            "lfc_deal_7" -> "menu_deal_7"
            "lfc_deal_8" -> "menu_deal_8"
            "lfc_deal_9" -> "menu_deal_9"
            "lfc_deal_10" -> "menu_deal_10"
            "lfc_deal_11" -> "menu_deal_11"
            "lfc_deal_12" -> "menu_deal_12"
            "lfc_deal_13" -> "menu_deal_13"
            "lfc_deal_14" -> "menu_deal_14"
            "lfc_deal_15" -> "menu_deal_15"
            "lfc_deal_16" -> "menu_deal_16"
            "lfc_deal_17" -> "menu_deal_17"
            "lfc_deal_18" -> "menu_deal_18"
            "lfc_deal_19" -> "menu_deal_19"
            "lfc_deal_birthday" -> "menu_deal_birthday"
            "lfc_burger_zinger" -> "menu_burger_zinger"
            "lfc_burger_signature" -> "menu_burger_lahori_sp"
            "lfc_burger_patty" -> "menu_burger_patty"
            "lfc_burger_double_decker" -> "menu_burger_double_decker"
            "lfc_burger_jalapeno" -> "menu_burger_jalapeno"
            "lfc_burger_beef" -> "menu_burger_beef"
            "lfc_burger_tower" -> "menu_burger_tower"
            "lfc_burger_chicken" -> "menu_burger_chicken"
            "lfc_burger_extra_cheese" -> "menu_extra_cheese_slice"
            "lfc_one_meter_pizza" -> "menu_one_meter_pizza"
            "lfc_ext_lazania" -> "menu_pizza_lazania"
            "lfc_ext_premium_pan" -> "menu_pizza_premium_pan"
            "lfc_ext_pizza_stick" -> "menu_pizza_doner_stick"
            "lfc_ext_peri_peri" -> "menu_pizza_peri_peri"
            "lfc_ext_malai_boti" -> "menu_pizza_malai_boti"
            "lfc_ext_kababish" -> "menu_pizza_bihari_kabab"
            "lfc_ext_crispy" -> "menu_pizza_crunchy_cheese"
            "lfc_trad_lahori_special" -> "menu_pizza_lahori_special"
            "lfc_trad_fajita" -> "menu_pizza_fajita"
            "lfc_trad_tikka" -> "menu_pizza_tikka"
            "lfc_trad_chicken_supreme" -> "menu_pizza_supreme"
            "lfc_trad_hot_spicy" -> "menu_pizza_hot_spicy"
            "lfc_trad_peri_tikka" -> "menu_pizza_peri_peri"
            "lfc_trad_bonfire" -> "menu_pizza_bonfire"
            "lfc_trad_shahi", "lfc_trad_nawabi" -> "menu_pizza_shahi"
            "lfc_trad_malai_boti", "lfc_trad_creamy_mughlai" -> "menu_pizza_malai_boti"
            "lfc_trad_crispy" -> "menu_pizza_crunchy_cheese"
            "lfc_prem_bihari_kabab" -> "menu_pizza_bihari_kabab"
            "lfc_prem_makhni_kabab", "lfc_prem_gola_kabab" -> "menu_pizza_makhni_kabab"
            "lfc_prem_crown_crust" -> "menu_pizza_crown_crust"
            "lfc_prem_royal_crust", "lfc_prem_kabab_crust" -> "menu_pizza_royal_crust"
            "lfc_prem_cheese_crust" -> "menu_pizza_cheese_crust"
            "lfc_prem_double_seekh" -> "menu_pizza_double_seekh"
            "lfc_wrap_lahori_special" -> "menu_roll_lahori_sp"
            "lfc_wrap_bihari_kabab" -> "menu_roll_bihari_kabab"
            "lfc_wrap_tikka_paratha" -> "menu_roll_tikka_paratha"
            "lfc_wrap_zinger_roll" -> "menu_roll_zinger"
            "lfc_wrap_malai_boti" -> "menu_roll_malai_boti"
            "lfc_wrap_grill" -> "menu_wrap_lahori_grilled"
            "lfc_wrap_chicken_shawarma" -> "menu_shawarma_chicken"
            "lfc_wrap_zinger_shawarma" -> "menu_shawarma_zinger"
            "lfc_sand_lahori_special", "lfc_sand_chicken", "lfc_sand_club", "lfc_sand_malai_panini" -> "menu_sandwich_club"
            "lfc_fries_plain", "lfc_fries_masala", "lfc_fries_single" -> "menu_fries_regular"
            "lfc_fries_pizza_loaded", "lfc_fries_crispy_loaded",
            "lfc_fries_loaded_small", "lfc_fries_loaded_large",
            "lfc_fries_crunchy_half", "lfc_fries_crunchy_full" -> "menu_fries_loaded"
            "lfc_fc_crispy_wings" -> "menu_fc_crispy_wings"
            "lfc_fc_nuggets" -> "menu_fc_nuggets"
            "lfc_fc_grilled_wings", "lfc_fc_smoky_wings",
            "lfc_fc_creamy_wings", "lfc_fc_peri_peri_wings" -> "menu_fc_glazed_wings"
            "lfc_fc_hot_shots" -> "menu_fc_hot_shot"
            "lfc_fc_leg_broast", "lfc_fc_chest_broast" -> "menu_fc_broast"
            "lfc_dip_sauce", "lfc_dip_special", "lfc_dip_bonfire",
            "lfc_dip_peri", "lfc_dip_garlic" -> "menu_dips_assorted"
            "lfc_top_chicken" -> "menu_pizza_lahori_special"
            "lfc_top_cheese" -> "menu_extra_cheese_slice"
            "lfc_drink_regular", "lfc_drink_500ml", "lfc_drink_1l",
            "lfc_drink_15l", "lfc_drink_jumbo",
            "lfc_drink_water_small", "lfc_drink_water_large" -> "menu_drinks_assorted"
            else -> null
        }
        if (byItemId != null) return byItemId
    }

    return when (cleanKey) {
        "pasta_bowl" -> "menu_pasta_lahori_sp"
        "combo_deal" -> "menu_deal_3"
        "birthday_deal" -> "menu_deal_birthday"
        "zinger_burger" -> "menu_burger_zinger"
        "extreme_pizza" -> "menu_pizza_lahori_special"
        "wrap_roll" -> "menu_roll_lahori_sp"
        "signature_sandwich" -> "menu_sandwich_club"
        "crispy_fries" -> "menu_fries_regular"
        "hot_wings" -> "menu_fc_crispy_wings"
        "pickled_crunch" -> "menu_dips_assorted"
        "soft_drink" -> "menu_drinks_assorted"
        else -> null
    }
}

fun resolveArtworkThemeKey(imageKey: String, itemId: String = ""): String {
    val cleanKey = imageKey.trim().lowercase()
    val cleanId = itemId.trim().lowercase()
    return when {
        cleanKey in setOf(
            "pasta_bowl", "combo_deal", "birthday_deal", "zinger_burger",
            "extreme_pizza", "wrap_roll", "crispy_fries", "hot_wings",
            "signature_sandwich", "pickled_crunch", "soft_drink"
        ) -> cleanKey
        cleanKey == "menu_deal_birthday" || cleanId.contains("birthday") -> "birthday_deal"
        cleanKey.startsWith("menu_deal_") || cleanId.contains("deal") -> "combo_deal"
        cleanKey.startsWith("menu_pasta_") || cleanId.contains("pasta") -> "pasta_bowl"
        cleanKey.startsWith("menu_pizza_") || cleanKey == "menu_one_meter_pizza" ||
            cleanId.contains("pizza") || cleanId.contains("_ext_") ||
            cleanId.contains("_trad_") || cleanId.contains("_prem_") -> "extreme_pizza"
        cleanKey.startsWith("menu_burger_") || cleanKey == "menu_extra_cheese_slice" ||
            cleanId.contains("burger") -> "zinger_burger"
        cleanKey.startsWith("menu_roll_") || cleanKey.startsWith("menu_wrap_") ||
            cleanKey.startsWith("menu_shawarma_") || cleanId.contains("wrap") -> "wrap_roll"
        cleanKey.startsWith("menu_fries_") || cleanId.contains("fries") -> "crispy_fries"
        cleanKey.startsWith("menu_fc_") || cleanId.contains("_fc_") -> "hot_wings"
        cleanKey.startsWith("menu_sandwich_") || cleanId.contains("sand") -> "signature_sandwich"
        cleanKey.startsWith("menu_dips_") || cleanId.contains("dip") -> "pickled_crunch"
        cleanKey.startsWith("menu_drinks_") || cleanId.contains("drink") -> "soft_drink"
        else -> cleanKey
    }
}

fun resolveMenuDrawableRes(
    context: android.content.Context,
    imageKey: String,
    itemId: String = ""
): Int? {
    val resName = resolveMenuDrawableName(imageKey, itemId) ?: return null
    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
    return resId.takeIf { it != 0 }
}

@Composable
fun FoodArtworkBox(
    imageKey: String,
    modifier: Modifier = Modifier,
    imageUrl: String = "",
    previewModel: Any? = null,
    contentDescription: String? = null,
    compact: Boolean = false,
    itemId: String = ""
) {
    val context = LocalContext.current
    val trimmedUrl = imageUrl.trim()
    val bundledDrawable = remember(context, imageKey, itemId) {
        resolveMenuDrawableRes(context, imageKey, itemId)
    }
    val themeKey = remember(imageKey, itemId) { resolveArtworkThemeKey(imageKey, itemId) }
    val isInlineCloudPhoto = previewModel == null && MenuImageStorageHelper.isDataImageUri(trimmedUrl)

    if (isInlineCloudPhoto) {
        val decodedBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = trimmedUrl) {
            value = withContext(Dispatchers.IO) {
                MenuImageStorageHelper.decodeDataImageBitmap(trimmedUrl)
            }
        }
        val bmp = decodedBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else if (bundledDrawable != null) {
            Image(
                painter = painterResource(id = bundledDrawable),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            DefaultFoodArtworkCanvas(
                imageKey = themeKey,
                compact = compact,
                modifier = modifier
            )
        }
        return
    }

    val resolvedModel: Any? = previewModel ?: trimmedUrl.takeIf { it.isNotBlank() }
    if (resolvedModel != null) {
        val request = remember(resolvedModel, context) {
            ImageRequest.Builder(context)
                .data(resolvedModel)
                .crossfade(true)
                .build()
        }
        SubcomposeAsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                if (bundledDrawable != null) {
                    Image(
                        painter = painterResource(id = bundledDrawable),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DefaultFoodArtworkCanvas(
                        imageKey = themeKey,
                        compact = compact,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            error = {
                if (bundledDrawable != null) {
                    Image(
                        painter = painterResource(id = bundledDrawable),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DefaultFoodArtworkCanvas(
                        imageKey = themeKey,
                        compact = compact,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        )
    } else if (bundledDrawable != null) {
        Image(
            painter = painterResource(id = bundledDrawable),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        DefaultFoodArtworkCanvas(
            imageKey = themeKey,
            compact = compact,
            modifier = modifier
        )
    }
}

@Composable
private fun DefaultFoodArtworkCanvas(
    imageKey: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (colors, icon, accentTint) = remember(imageKey) {
        when (imageKey) {
            "pasta_bowl" -> Triple(
                listOf(Color(0xFFFFF5DC), Color(0xFFFFCA6C), Color(0xFFE8611A)),
                Icons.Filled.DinnerDining,
                Color(0xFF9C3006)
            )
            "combo_deal" -> Triple(
                listOf(Color(0xFFFFEFE0), Color(0xFFFF9C4A), Color(0xFFD60027)),
                Icons.Filled.TakeoutDining,
                Color(0xFF94001B)
            )
            "birthday_deal" -> Triple(
                listOf(Color(0xFFFFEAF2), Color(0xFFFF8CB2), Color(0xFFE4002B)),
                Icons.Filled.Cake,
                Color(0xFF990024)
            )
            "zinger_burger" -> Triple(
                listOf(Color(0xFFFFF1DF), Color(0xFFFFC178), Color(0xFFFF7A3D)),
                Icons.Filled.LunchDining,
                Color(0xFFB32400)
            )
            "extreme_pizza" -> Triple(
                listOf(Color(0xFFFFEED9), Color(0xFFFFAE52), Color(0xFFE4002B)),
                Icons.Filled.LocalPizza,
                Color(0xFF9E140B)
            )
            "wrap_roll" -> Triple(
                listOf(Color(0xFFFFF4DE), Color(0xFFF7B963), Color(0xFFC95E18)),
                Icons.Filled.Fastfood,
                Color(0xFF7D3405)
            )
            "crispy_fries" -> Triple(
                listOf(Color(0xFFFFFBE0), Color(0xFFFFD84D), Color(0xFFE68A00)),
                Icons.Filled.TakeoutDining,
                Color(0xFFA85200)
            )
            "hot_wings" -> Triple(
                listOf(Color(0xFFFFE5DF), Color(0xFFFF8261), Color(0xFFD60027)),
                Icons.Filled.LocalFireDepartment,
                Color(0xFF99001A)
            )
            "signature_sandwich" -> Triple(
                listOf(Color(0xFFFFF6DE), Color(0xFFFFD37A), Color(0xFFE08824)),
                Icons.Filled.BakeryDining,
                Color(0xFF7D4806)
            )
            "pickled_crunch" -> Triple(
                listOf(Color(0xFFEBF9E0), Color(0xFFA6E07B), Color(0xFF2D8C45)),
                Icons.Filled.Fastfood,
                Color(0xFF145224)
            )
            "soft_drink" -> Triple(
                listOf(Color(0xFFE3F5FF), Color(0xFF69C0FF), Color(0xFF0E68B8)),
                Icons.Filled.LocalDrink,
                Color(0xFF084278)
            )
            else -> Triple(
                listOf(Color(0xFFFFE6EB), Color(0xFFFF889E), Color(0xFFE4002B)),
                Icons.Filled.Restaurant,
                Color(0xFF8A0019)
            )
        }
    }

    Box(
        modifier = modifier
            .background(Brush.radialGradient(colors = colors)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.34f),
                radius = size.minDimension * 0.46f,
                center = Offset(size.width * 0.5f, size.height * 0.52f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = size.minDimension * 0.28f,
                center = Offset(size.width * 0.80f, size.height * 0.22f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = size.minDimension * 0.22f,
                center = Offset(size.width * 0.18f, size.height * 0.82f)
            )
        }
        Surface(
            shape = CircleShape,
            color = LahoriWhite,
            shadowElevation = if (compact) 4.dp else 10.dp,
            modifier = Modifier.size(if (compact) 36.dp else 78.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier.size(if (compact) 20.dp else 44.dp)
                )
            }
        }
    }
}

@Composable
fun MenuItemCard(
    item: MenuItem,
    isFavorite: Boolean,
    cartQuantities: Map<String, Int>,
    onToggleFavorite: () -> Unit,
    onAddToBucket: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
    onDecrementFromBucket: ((String) -> Unit)? = null,
    onCardClick: ((MenuItem, Int) -> Unit)? = null
) {
    KfcProductCard(
        item = item,
        isFavorite = isFavorite,
        cartQuantities = cartQuantities,
        onToggleFavorite = onToggleFavorite,
        onAddToBucket = onAddToBucket,
        modifier = modifier,
        onDecrementFromBucket = onDecrementFromBucket,
        onCardClick = onCardClick
    )
}

