package com.android.messaging.ui.common.components

import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

@Composable
internal fun displayCornerRadius(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize

    return remember(view, density, containerSize) {
        val radiusPx = view.rootWindowInsets
            ?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
            ?.radius
            ?: 0

        with(density) {
            radiusPx.toDp()
        }
    }
}
