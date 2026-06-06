package com.android.messaging.domain.shareintent.usecase

import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.PendingAttachmentData
import com.android.messaging.util.ContentType
import javax.inject.Inject

internal interface BuildSharedMessageData {
    operator fun invoke(draft: ConversationDraft): MessageData
}

internal class BuildSharedMessageDataImpl @Inject constructor() : BuildSharedMessageData {

    override fun invoke(draft: ConversationDraft): MessageData {
        return MessageData.createSharedMessage(
            draft.messageText,
            draft.subjectText,
        ).apply {
            draft.attachments
                .filter { attachment ->
                    ContentType.isMediaType(attachment.contentType)
                }
                .forEach { attachment ->
                    addPart(
                        PendingAttachmentData.createPendingAttachmentData(
                            attachment.contentType,
                            attachment.contentUri.toUri(),
                        ),
                    )
                }
        }
    }
}
