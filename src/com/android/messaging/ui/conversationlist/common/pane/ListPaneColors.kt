package com.android.messaging.ui.conversationlist.common.pane

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.messaging.ui.common.components.LocalIsListDetailPane

@Composable
internal fun listPaneContentColor(): Color {
    return when {
        LocalIsListDetailPane.current -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.background
    }
}

// The two-pane list already sits on surfaceContainerLow, so a highlighted row needs a color of
// its own to stand out from both the pane and the unhighlighted rows.
@Composable
internal fun listPaneHighlightedItemColor(): Color {
    return when {
        LocalIsListDetailPane.current -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
}
