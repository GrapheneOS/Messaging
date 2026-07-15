package com.android.messaging.ui.common.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat.LTR

@Composable
internal fun String.asLtrText(): String {
    val layoutDirection = LocalLayoutDirection.current
    val isRtlContext = layoutDirection == LayoutDirection.Rtl

    return remember(this, isRtlContext) {
        BidiFormatter
            .getInstance(isRtlContext)
            .unicodeWrap(this, LTR)
    }
}
