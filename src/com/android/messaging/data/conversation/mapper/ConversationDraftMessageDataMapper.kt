package com.android.messaging.data.conversation.mapper

import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.util.LogUtil
import javax.inject.Inject

internal interface ConversationDraftMessageDataMapper {
    fun map(
        conversationId: String,
        draft: ConversationDraft,
        forceMms: Boolean = false,
    ): MessageData
}

internal class ConversationDraftMessageDataMapperImpl @Inject constructor() :
    ConversationDraftMessageDataMapper {

    override fun map(
        conversationId: String,
        draft: ConversationDraft,
        forceMms: Boolean,
    ): MessageData {
        val selfParticipantId = draft.selfParticipantId.takeIf { it.isNotBlank() }
        val messageParts = draft.attachments.mapNotNull(::createMessagePartDataOrNull)
        val isMms = forceMms || draft.subjectText.isNotBlank() || messageParts.isNotEmpty()

        val message = when {
            isMms -> MessageData.createDraftMmsMessage(
                conversationId,
                selfParticipantId,
                draft.messageText,
                draft.subjectText,
            )

            else -> MessageData.createDraftSmsMessage(
                conversationId,
                selfParticipantId,
                draft.messageText,
            )
        }

        messageParts.forEach(message::addPart)

        return message
    }

    private fun createMessagePartDataOrNull(
        attachment: ConversationDraftAttachment,
    ): MessagePartData? {
        if (attachment.contentType.isBlank() || attachment.contentUri.isBlank()) {
            LogUtil.w(TAG, "Dropping draft attachment with blank contentType or contentUri")
            return null
        }

        val captionText = attachment.captionText.takeIf { it.isNotBlank() }
        val contentUri = attachment.contentUri.toUri()
        val width = toLegacyPartDimension(size = attachment.width)
        val height = toLegacyPartDimension(size = attachment.height)

        return when {
            captionText != null -> {
                MessagePartData.createMediaMessagePart(
                    captionText,
                    attachment.contentType,
                    contentUri,
                    width,
                    height,
                )
            }

            else -> {
                MessagePartData.createMediaMessagePart(
                    attachment.contentType,
                    contentUri,
                    width,
                    height,
                )
            }
        }
    }

    private fun toLegacyPartDimension(size: Int?): Int {
        return size ?: MessagePartData.UNSPECIFIED_SIZE
    }

    private companion object {
        private const val TAG = "ConversationDraftMessageDataMapper"
    }
}
