package com.android.messaging.ui.conversation.mediapicker.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.media.model.ConversationCapturedMedia
import javax.inject.Inject

internal interface ConversationDraftAttachmentMapper {
    fun map(capturedMedia: ConversationCapturedMedia): ConversationDraftAttachment
}

internal class ConversationDraftAttachmentMapperImpl @Inject constructor() :
    ConversationDraftAttachmentMapper {

    override fun map(capturedMedia: ConversationCapturedMedia): ConversationDraftAttachment {
        return ConversationDraftAttachment(
            contentType = capturedMedia.contentType,
            contentUri = capturedMedia.contentUri,
            width = capturedMedia.width,
            height = capturedMedia.height,
        )
    }
}
