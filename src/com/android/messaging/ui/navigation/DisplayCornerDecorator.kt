package com.android.messaging.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.android.messaging.ui.common.components.LocalIsListDetailPane
import com.android.messaging.ui.common.components.displayCornerRadius
import com.android.messaging.ui.common.components.predictiveBackPage

@Composable
internal fun rememberDisplayCornerNavEntryDecorator(
    topContentKey: Any?,
): NavEntryDecorator<NavKey> {
    val cornerRadius = displayCornerRadius()
    val currentTopContentKey by rememberUpdatedState(topContentKey)

    return remember(cornerRadius) {
        NavEntryDecorator { entry ->
            DisplayCorneredContent(
                cornerRadius = cornerRadius,
                isTop = entry.contentKey == currentTopContentKey,
                entry = entry,
            )
        }
    }
}

@Composable
private fun DisplayCorneredContent(
    cornerRadius: Dp,
    isTop: Boolean,
    entry: NavEntry<NavKey>,
) {
    if (LocalIsListDetailPane.current) {
        entry.Content()
        return
    }

    val transition = LocalNavAnimatedContentScope.current.transition

    Box(
        modifier = Modifier
            .predictiveBackPage(
                transition = transition,
                isOpen = isTop,
            )
            .clip(
                shape = RoundedCornerShape(size = cornerRadius),
            ),
    ) {
        entry.Content()
    }
}
