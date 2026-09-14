package com.android.messaging.ui.common.components

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

private val ZeroCornerSize = CornerSize(0.dp)

internal val MaterialTheme.contentSurfaceShape: CornerBasedShape
    @Composable @ReadOnlyComposable
    get() {
        val flatShape = shapes.large.copy(
            bottomStart = ZeroCornerSize,
            bottomEnd = ZeroCornerSize,
        )

        if (!LocalIsListDetailPane.current) {
            return flatShape
        }

        return when (LocalListDetailPaneSide.current) {
            ListDetailPaneSide.Start -> flatShape.copy(topEnd = ZeroCornerSize)
            ListDetailPaneSide.End -> flatShape.copy(topStart = ZeroCornerSize)
            null -> flatShape
        }
    }
