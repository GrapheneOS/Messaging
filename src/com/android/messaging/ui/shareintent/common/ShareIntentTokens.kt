package com.android.messaging.ui.shareintent.common

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

internal val ScreenContentPadding = 8.dp

internal val ItemDividerHorizontalInset = 40.dp

internal val AvatarSize = 48.dp

internal val FallbackIconSize = 20.dp

internal val SelectedBarBottomPadding = 8.dp

internal val SelectedChipSpacing = 8.dp

internal val SelectedChipAvatarSize = 56.dp

internal val SelectedChipLabelSpacing = 4.dp

internal val SelectedChipLabelHeight = 16.dp

internal val SelectedChipRemoveBadgeSize = 18.dp

internal val SelectedChipRemoveIconSize = 12.dp

internal val SelectedSendButtonSize = 56.dp

internal val SelectedSendButtonCornerRadius = 18.dp

private val ListRowHorizontalPadding = 8.dp

internal val SelectedBarHeight =
    SelectedChipAvatarSize +
        SelectedChipLabelSpacing +
        SelectedChipLabelHeight +
        SelectedBarBottomPadding

internal val SelectedBarStartPadding =
    ScreenContentPadding +
        ListRowHorizontalPadding +
        (AvatarSize - SelectedChipAvatarSize) / 2

internal val SelectedBarEndPadding =
    SelectedBarStartPadding +
        SelectedSendButtonSize +
        SelectedChipSpacing

private val ZeroCornerSize = CornerSize(0.dp)

internal val MaterialTheme.contentSurfaceShape: CornerBasedShape
    @Composable @ReadOnlyComposable
    get() = shapes.large.copy(
        bottomStart = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
    )
