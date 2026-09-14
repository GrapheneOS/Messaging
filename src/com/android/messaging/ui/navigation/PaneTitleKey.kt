package com.android.messaging.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata

internal object PaneTitleKey : NavMetadataKey<Int>

internal fun paneTitleMetadata(@StringRes titleResId: Int): Map<String, Any> {
    return metadata {
        put(
            key = PaneTitleKey,
            value = titleResId,
        )
    }
}

@Composable
internal fun rememberPaneTitleNavEntryDecorator(): NavEntryDecorator<NavKey> {
    return remember {
        NavEntryDecorator { entry ->
            when (val titleResId = entry.paneTitleResId()) {
                null -> entry.Content()
                else -> PaneTitledContent(titleResId = titleResId, entry = entry)
            }
        }
    }
}

@Composable
private fun PaneTitledContent(
    @StringRes titleResId: Int,
    entry: NavEntry<NavKey>,
) {
    val title = stringResource(titleResId)

    Box(modifier = Modifier.semantics { paneTitle = title }) {
        entry.Content()
    }
}

private fun NavEntry<NavKey>.paneTitleResId(): Int? {
    if (PaneTitleKey !in metadata) {
        return null
    }

    return metadata[PaneTitleKey]
}
