package com.android.messaging.data.conversation.mapper

import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UriUtil
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationMessageDataDraftMapper {
    fun map(
        messageData: MessageData,
        fallbackSelfParticipantId: ParticipantId? = null,
    ): ConversationDraft
}

internal class ConversationMessageDataDraftMapperImpl @Inject constructor() :
    ConversationMessageDataDraftMapper {

    override fun map(
        messageData: MessageData,
        fallbackSelfParticipantId: ParticipantId?,
    ): ConversationDraft {
        return ConversationDraft(
            messageText = messageData.messageText,
            subjectText = messageData.mmsSubject.orEmpty(),
            selfParticipantId = ParticipantId.fromOrNull(messageData.selfId)
                ?: fallbackSelfParticipantId,
            attachments = messageData.parts
                .asSequence()
                .filter { it.isAttachment }
                .mapNotNull(::createDraftAttachmentOrNull)
                .toImmutableList(),
        )
    }

    private fun createDraftAttachmentOrNull(
        part: MessagePartData,
    ): ConversationDraftAttachment? {
        val contentType = part.contentType?.takeIf { it.isNotBlank() }
        val contentUri = part.contentUri?.toString()?.takeIf { it.isNotBlank() }

        return when {
            isMediaStoreUri(contentUri) -> {
                LogUtil.w(TAG, "Dropping draft attachment backed by MediaStore URI")
                null
            }

            contentType != null && contentUri != null -> {
                ConversationDraftAttachment(
                    contentType = contentType,
                    contentUri = contentUri,
                    captionText = part.text.orEmpty(),
                    width = normalizePartDimension(size = part.width),
                    height = normalizePartDimension(size = part.height),
                )
            }

            else -> {
                LogUtil.w(
                    TAG,
                    "Dropping draft attachment with blank contentType or contentUri",
                )

                null
            }
        }
    }

    private fun normalizePartDimension(size: Int): Int? {
        return size.takeIf { it != MessagePartData.UNSPECIFIED_SIZE }
    }

    /**
     * The app holds no media read permissions, so a MediaStore URI is unreadable unless a grant
     * came with it, and a grant never survives into a persisted draft. No live path stores one
     * anyway - every picker copies its selection into scratch space first - so these only turn up
     * in drafts written by the pre-Compose gallery picker. Drop them rather than restore an
     * attachment that can only fail.
     */
    private fun isMediaStoreUri(uri: String?): Boolean {
        return uri != null && UriUtil.isMediaStoreUri(uri.toUri())
    }

    private companion object {
        private const val TAG = "ConversationMsgDataDraftMapper"
    }
}
