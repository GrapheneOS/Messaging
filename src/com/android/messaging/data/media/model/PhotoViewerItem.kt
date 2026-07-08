package com.android.messaging.data.media.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
internal data class PhotoViewerItem(
    val contentUri: Uri,
    val contentType: String,
    val senderName: String?,
    val senderDestination: String?,
    val receivedTimestampMillis: Long,
    val isDraft: Boolean,
    val canUseActions: Boolean = true,
) {
    val title: String
        get() {
            return senderName
                ?.takeIf { it.isNotBlank() }
                ?: senderDestination
                    ?.takeIf { it.isNotBlank() }
                    .orEmpty()
        }
}
