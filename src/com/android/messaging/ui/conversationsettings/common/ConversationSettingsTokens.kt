package com.android.messaging.ui.conversationsettings.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal val ScreenContentPadding = 16.dp
internal val SectionSpacing = 12.dp
internal val GroupedItemSpacing = 4.dp

private val SettingsCardCornerRadius = 20.dp
private val GroupedItemInnerCornerRadius = 4.dp

internal val SettingsCardShape = RoundedCornerShape(
    size = SettingsCardCornerRadius,
)

internal val GroupedTopItemShape = RoundedCornerShape(
    topStart = SettingsCardCornerRadius,
    topEnd = SettingsCardCornerRadius,
    bottomStart = GroupedItemInnerCornerRadius,
    bottomEnd = GroupedItemInnerCornerRadius,
)

internal val GroupedMiddleItemShape = RoundedCornerShape(
    size = GroupedItemInnerCornerRadius,
)

internal val GroupedBottomItemShape = RoundedCornerShape(
    topStart = GroupedItemInnerCornerRadius,
    topEnd = GroupedItemInnerCornerRadius,
    bottomStart = SettingsCardCornerRadius,
    bottomEnd = SettingsCardCornerRadius,
)
