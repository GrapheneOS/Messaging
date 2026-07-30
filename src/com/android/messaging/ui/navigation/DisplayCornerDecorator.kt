package com.android.messaging.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.common.components.LocalIsListDetailPane
import com.android.messaging.ui.common.components.displayCornerRadius

@Composable
internal fun rememberDisplayCornerNavEntryDecorator(): NavEntryDecorator<NavKey> {
    val cornerRadius = displayCornerRadius()

    return remember(cornerRadius) {
        NavEntryDecorator { entry ->
            DisplayCorneredContent(
                cornerRadius = cornerRadius,
                entry = entry,
            )
        }
    }
}

@Composable
private fun DisplayCorneredContent(
    cornerRadius: Dp,
    entry: NavEntry<NavKey>,
) {
    if (LocalIsListDetailPane.current) {
        entry.Content()
        return
    }

    Box(modifier = Modifier.clip(shape = RoundedCornerShape(size = cornerRadius))) {
        entry.Content()
    }
}
