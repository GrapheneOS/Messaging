package com.android.messaging.data.media.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
internal data class PhotoViewerItem(
    val contentUri: Uri,
    val contentType: String,
    val isIncoming: Boolean,
    val senderName: String?,
    val senderDestination: String?,
    val receivedTimestampMillis: Long,
    val isDraft: Boolean,
    val canUseActions: Boolean = true,
)
