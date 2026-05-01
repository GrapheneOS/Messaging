package com.android.messaging.ui.conversation.mediapicker.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.data.media.model.ConversationMediaItem
import javax.inject.Inject

internal interface ConversationDraftAttachmentMapper {
    fun map(mediaItem: ConversationMediaItem): ConversationDraftAttachment

    fun map(capturedMedia: ConversationCapturedMedia): ConversationDraftAttachment
}

internal class ConversationDraftAttachmentMapperImpl @Inject constructor() :
    ConversationDraftAttachmentMapper {

    override fun map(mediaItem: ConversationMediaItem): ConversationDraftAttachment {
        return ConversationDraftAttachment(
            contentType = mediaItem.contentType,
            contentUri = mediaItem.contentUri,
            width = mediaItem.width,
            height = mediaItem.height,
        )
    }

    override fun map(capturedMedia: ConversationCapturedMedia): ConversationDraftAttachment {
        return ConversationDraftAttachment(
            contentType = capturedMedia.contentType,
            contentUri = capturedMedia.contentUri,
            width = capturedMedia.width,
            height = capturedMedia.height,
        )
    }
}
