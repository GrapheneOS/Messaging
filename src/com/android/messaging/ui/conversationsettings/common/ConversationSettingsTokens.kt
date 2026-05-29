package com.android.messaging.ui.conversationsettings.common

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

internal val ScreenContentPadding = 16.dp
internal val SectionSpacing = 12.dp
internal val GroupedItemSpacing = 4.dp

private val GroupedItemInnerCornerRadius = 4.dp
private val GroupedItemInnerCornerSize = CornerSize(GroupedItemInnerCornerRadius)

internal val MaterialTheme.settingsCardShape: CornerBasedShape
    @Composable @ReadOnlyComposable
    get() = shapes.medium

internal val MaterialTheme.groupedMiddleItemShape: CornerBasedShape
    @Composable @ReadOnlyComposable
    get() = RoundedCornerShape(GroupedItemInnerCornerRadius)

internal val MaterialTheme.groupedTopItemShape: CornerBasedShape
    @Composable @ReadOnlyComposable
    get() = shapes.medium.copy(
        bottomStart = GroupedItemInnerCornerSize,
        bottomEnd = GroupedItemInnerCornerSize,
    )

internal val MaterialTheme.groupedBottomItemShape: CornerBasedShape
    @Composable @ReadOnlyComposable
    get() = shapes.medium.copy(
        topStart = GroupedItemInnerCornerSize,
        topEnd = GroupedItemInnerCornerSize,
    )
