package com.android.messaging.ui.blockedparticipants.common

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

internal val ScreenContentPadding = 8.dp

internal val ItemHorizontalPadding = 8.dp
internal val ItemVerticalPadding = 8.dp

internal val ItemDividerHorizontalInset = 40.dp

private val ZeroCornerSize = CornerSize(0.dp)

internal val MaterialTheme.contentSurfaceShape: CornerBasedShape
    @Composable @ReadOnlyComposable
    get() = shapes.large.copy(
        bottomStart = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
    )

internal val MaterialTheme.blockedParticipantItemShape: CornerBasedShape
    @Composable @ReadOnlyComposable
    get() = RoundedCornerShape(percent = 50)
