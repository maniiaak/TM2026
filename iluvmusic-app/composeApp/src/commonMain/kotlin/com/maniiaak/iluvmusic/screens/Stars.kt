package com.maniiaak.iluvmusic.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kmp_app_template.composeapp.generated.resources.Res
import kmp_app_template.composeapp.generated.resources.custom_half_star
import kmp_app_template.composeapp.generated.resources.custom_star
import kmp_app_template.composeapp.generated.resources.custom_star2
import kmp_app_template.composeapp.generated.resources.custom_star3
import kmp_app_template.composeapp.generated.resources.custom_star4
import kmp_app_template.composeapp.generated.resources.custom_star5
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.roundToInt

private val starIcons = listOf(
    Res.drawable.custom_star,
    Res.drawable.custom_star2,
    Res.drawable.custom_star3,
    Res.drawable.custom_star4,
    Res.drawable.custom_star5
)

@Composable
fun StarRow(rating: Double, starSize: Dp = 18.dp) {
    val halfUnits = (rating * 2.0).roundToInt().coerceIn(0, 10)
    val fullStars = halfUnits / 2
    val hasHalfStar = halfUnits % 2 == 1
    val halfStarIndex = if (hasHalfStar) fullStars else -1

    Row {
        repeat(5) { index ->
            val isHalf = index == halfStarIndex
            val isFilled = isHalf || index < fullStars

            Icon(
                imageVector = vectorResource(if (isHalf) Res.drawable.custom_half_star else starIcons[index]),
                contentDescription = null,
                tint = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(starSize)
            )
        }
    }
}