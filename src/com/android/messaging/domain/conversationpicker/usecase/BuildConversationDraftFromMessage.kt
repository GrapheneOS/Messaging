package com.android.messaging.domain.conversationpicker.usecase

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.util.ContentType
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

internal interface BuildConversationDraftFromMessage {
    operator fun invoke(message: MessageData): ConversationDraft
}

internal class BuildConversationDraftFromMessageImpl @Inject constructor() :
    BuildConversationDraftFromMessage {

    override fun invoke(message: MessageData): ConversationDraft {
        val attachments = message.parts
            .filter { part ->
                ContentType.isMediaType(part.contentType) && part.contentUri != null
            }
            .map { part ->
                ConversationDraftAttachment(
                    contentType = part.contentType,
                    contentUri = part.contentUri.toString(),
                    captionText = part.text.orEmpty(),
                )
            }
            .toImmutableList()

        return ConversationDraft(
            messageText = message.messageText,
            subjectText = message.mmsSubject.orEmpty(),
            attachments = attachments,
        )
    }
}
