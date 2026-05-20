package com.android.messaging.data.conversation.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.util.LogUtil
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationMessageDataDraftMapper {
    fun map(
        messageData: MessageData,
        fallbackSelfParticipantId: String? = null,
    ): ConversationDraft
}

internal class ConversationMessageDataDraftMapperImpl @Inject constructor() :
    ConversationMessageDataDraftMapper {

    override fun map(
        messageData: MessageData,
        fallbackSelfParticipantId: String?,
    ): ConversationDraft {
        return ConversationDraft(
            messageText = messageData.messageText,
            subjectText = messageData.mmsSubject.orEmpty(),
            selfParticipantId = messageData
                .selfId
                ?.takeIf { it.isNotBlank() }
                ?: fallbackSelfParticipantId.orEmpty(),
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
            isPhotoPickerUri(contentUri) -> {
                LogUtil.w(TAG, "Dropping draft attachment backed by photo picker URI")
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

    private fun isPhotoPickerUri(uri: String?): Boolean {
        return uri?.startsWith(prefix = PHOTO_PICKER_URI_PREFIX) == true
    }

    private companion object {
        private const val TAG = "ConversationMsgDataDraftMapper"
        private const val PHOTO_PICKER_URI_PREFIX = "content://media/picker/"
    }
}
