package com.android.messaging.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import com.android.messaging.ui.common.components.ListDetailPaneSide
import com.android.messaging.ui.common.components.LocalIsListDetailPane
import com.android.messaging.ui.common.components.LocalListDetailPaneSide

internal object ListDetailPaneKey : NavMetadataKey<ListDetailPaneSide>

internal fun listDetailPaneMetadata(side: ListDetailPaneSide): Map<String, Any> {
    return metadata {
        put(
            key = ListDetailPaneKey,
            value = side,
        )
    }
}

@Composable
internal fun rememberListDetailPaneNavEntryDecorator(
    showsTwoPanes: Boolean,
): NavEntryDecorator<NavKey> {
    return remember(showsTwoPanes) {
        NavEntryDecorator { entry ->
            when (val side = entry.listDetailPaneSide()) {
                null -> entry.Content()
                else -> ListDetailPaneContent(
                    side = side,
                    showsTwoPanes = showsTwoPanes,
                    entry = entry,
                )
            }
        }
    }
}

@Composable
private fun ListDetailPaneContent(
    side: ListDetailPaneSide,
    showsTwoPanes: Boolean,
    entry: NavEntry<NavKey>,
) {
    CompositionLocalProvider(
        LocalIsListDetailPane provides showsTwoPanes,
        LocalListDetailPaneSide provides side,
    ) {
        entry.Content()
    }
}

private fun NavEntry<NavKey>.listDetailPaneSide(): ListDetailPaneSide? {
    if (ListDetailPaneKey !in metadata) {
        return null
    }

    return metadata[ListDetailPaneKey]
}
