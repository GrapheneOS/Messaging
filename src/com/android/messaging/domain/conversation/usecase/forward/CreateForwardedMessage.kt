package com.android.messaging.domain.conversation.usecase.forward

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.datamodel.data.PendingAttachmentData
import javax.inject.Inject

internal interface CreateForwardedMessage {
    suspend operator fun invoke(
        conversationId: ConversationId,
        messageId: MessageId,
    ): MessageData?
}

internal class CreateForwardedMessageImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val forwardedMessageSubjectFormatter: ForwardedMessageSubjectFormatter,
) : CreateForwardedMessage {

    override suspend operator fun invoke(
        conversationId: ConversationId,
        messageId: MessageId,
    ): MessageData? {
        val message = conversationsRepository
            .getConversationMessage(
                conversationId = conversationId,
                messageId = messageId,
            )
            ?: return null

        val forwardedMessage = MessageData()

        forwardedMessage.mmsSubject = forwardedMessageSubjectFormatter.format(
            subject = message.mmsSubject,
        )

        message
            .parts
            ?.map(::createForwardedPart)
            ?.forEach(forwardedMessage::addPart)

        return forwardedMessage
    }

    private fun createForwardedPart(part: MessagePartData): MessagePartData {
        return when {
            part.isText -> MessagePartData.createTextMessagePart(part.text)

            else -> {
                PendingAttachmentData.createPendingAttachmentData(
                    part.contentType,
                    part.contentUri,
                )
            }
        }
    }
}
