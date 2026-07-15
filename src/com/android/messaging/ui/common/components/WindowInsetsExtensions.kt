package com.android.messaging.ui.common.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

@Composable
fun horizontalSafeDrawingInsets(): PaddingValues {
    return WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal)
        .asPaddingValues()
}

@Composable
fun safeDrawingContentPadding(
    top: Dp,
    bottom: Dp,
    horizontal: Dp,
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val horizontalInsets = horizontalSafeDrawingInsets()

    return PaddingValues(
        top = top,
        bottom = bottom,
        start = horizontal + horizontalInsets.calculateStartPadding(layoutDirection),
        end = horizontal + horizontalInsets.calculateEndPadding(layoutDirection),
    )
}

@Composable
fun bottomBarInsets(): WindowInsets {
    return WindowInsets.systemBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
}

@Composable
fun imeAwareBottomBarInsets(): WindowInsets {
    return WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
}
