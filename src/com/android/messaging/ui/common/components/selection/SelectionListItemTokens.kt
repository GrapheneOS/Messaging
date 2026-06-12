package com.android.messaging.ui.common.components.selection

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal object SelectionListItemTokens {
    val cornerRadius = 18.dp
    val middleCornerRadius = 2.dp
    val avatarSize = 40.dp
    val avatarFallbackSize = 20.dp
    val avatarToTextSpacing = 14.dp
    val rowHorizontalPadding = 16.dp
    val rowVerticalPadding = 14.dp

    private val topShape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomStart = middleCornerRadius,
        bottomEnd = middleCornerRadius,
    )
    private val bottomShape = RoundedCornerShape(
        topStart = middleCornerRadius,
        topEnd = middleCornerRadius,
        bottomStart = cornerRadius,
        bottomEnd = cornerRadius,
    )
    private val middleShape = RoundedCornerShape(size = middleCornerRadius)
    val singleShape = RoundedCornerShape(size = cornerRadius)

    fun shape(
        index: Int,
        totalCount: Int,
    ): RoundedCornerShape {
        return when {
            totalCount <= 1 -> singleShape
            index == 0 -> topShape
            index == totalCount - 1 -> bottomShape
            else -> middleShape
        }
    }
}
