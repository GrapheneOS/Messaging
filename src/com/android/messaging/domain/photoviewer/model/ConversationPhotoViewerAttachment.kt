package com.android.messaging.domain.photoviewer.model

import android.net.Uri

internal data class ConversationPhotoViewerAttachment(
    val partId: String,
    val contentUri: Uri,
)
