package com.android.messaging.ui.photoviewer.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.data.media.model.PhotoViewerItem

@Composable
internal fun photoViewerParticipantTitle(
    item: PhotoViewerItem?,
): String {
    val title = when {
        item == null -> null
        item.isIncoming -> {
            item.senderName
                ?.takeIf { it.isNotBlank() }
                ?: item.senderDestination?.takeIf { it.isNotBlank() }
        }

        else -> stringResource(id = R.string.unknown_self_participant)
    }

    return title ?: stringResource(id = R.string.unknown_sender)
}
