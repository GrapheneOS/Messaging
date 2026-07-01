package com.android.messaging.ui.photoviewer.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class PhotoViewerSourceBounds(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top

    val centerX: Float
        get() = (left + right) / 2f

    val centerY: Float
        get() = (top + bottom) / 2f
}
