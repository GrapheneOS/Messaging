package com.android.messaging.ui.common.components.mediapreview

import androidx.compose.ui.graphics.Color

internal fun mediaOverlayContainerColor(alpha: Float): Color {
    return Color.Black.copy(alpha = alpha)
}

internal fun mediaOverlayContentColor(alpha: Float = 1f): Color {
    return Color.White.copy(alpha = alpha)
}
