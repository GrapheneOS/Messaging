package com.android.messaging.domain.photoviewer.usecase

import android.net.Uri
import com.android.messaging.domain.photoviewer.model.ConversationPhotoViewerAttachment
import javax.inject.Inject

internal interface ResolveConversationPhotoViewerInitialOccurrenceIndex {
    operator fun invoke(
        partId: String,
        contentUri: Uri,
        attachments: Sequence<ConversationPhotoViewerAttachment>,
    ): Int
}

internal class ResolveConversationPhotoViewerInitialOccurrenceIndexImpl @Inject constructor(
    private val normalizePhotoViewerUri: NormalizePhotoViewerUri,
) : ResolveConversationPhotoViewerInitialOccurrenceIndex {

    override fun invoke(
        partId: String,
        contentUri: Uri,
        attachments: Sequence<ConversationPhotoViewerAttachment>,
    ): Int {
        if (partId.isBlank()) {
            return 0
        }

        val normalizedContentUri = normalizePhotoViewerUri(uri = contentUri)
        var resolvedOccurrenceIndex = 0
        var clickedPartFound = false

        for (attachment in attachments) {
            if (attachment.partId == partId) {
                clickedPartFound = true
                break
            }

            if (normalizePhotoViewerUri(uri = attachment.contentUri) == normalizedContentUri) {
                resolvedOccurrenceIndex++
            }
        }

        return when {
            clickedPartFound -> resolvedOccurrenceIndex
            else -> 0
        }
    }
}
