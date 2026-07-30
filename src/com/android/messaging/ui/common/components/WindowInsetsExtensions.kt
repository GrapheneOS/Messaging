package com.android.messaging.ui.common.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

@Composable
fun horizontalSafeDrawingInsets(): PaddingValues {
    val windowInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal)
        .asPaddingValues()
    val paneSide = LocalListDetailPaneSide.current

    if (!LocalIsListDetailPane.current || paneSide == null) {
        return windowInsets
    }

    val layoutDirection = LocalLayoutDirection.current

    return when (paneSide) {
        ListDetailPaneSide.Start -> PaddingValues(
            start = windowInsets.calculateStartPadding(layoutDirection),
        )

        ListDetailPaneSide.End -> PaddingValues(
            end = windowInsets.calculateEndPadding(layoutDirection),
        )
    }
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
internal fun Modifier.consumeOppositePaneInsets(): Modifier {
    val paneSide = LocalListDetailPaneSide.current

    if (!LocalIsListDetailPane.current || paneSide == null) {
        return this
    }

    val oppositeSide = when (paneSide) {
        ListDetailPaneSide.Start -> WindowInsetsSides.End
        ListDetailPaneSide.End -> WindowInsetsSides.Start
    }

    return consumeWindowInsets(insets = WindowInsets.safeDrawing.only(oppositeSide))
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
