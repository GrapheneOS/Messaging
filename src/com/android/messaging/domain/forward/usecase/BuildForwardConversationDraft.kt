package com.android.messaging.domain.forward.usecase

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.util.ContentType
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

internal interface BuildForwardConversationDraft {
    operator fun invoke(message: MessageData): ConversationDraft
}

internal class BuildForwardConversationDraftImpl @Inject constructor() :
    BuildForwardConversationDraft {

    override fun invoke(message: MessageData): ConversationDraft {
        val attachments = message.parts
            .filter { part ->
                ContentType.isMediaType(part.contentType) && part.contentUri != null
            }
            .map { part ->
                ConversationDraftAttachment(
                    contentType = part.contentType,
                    contentUri = part.contentUri.toString(),
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
