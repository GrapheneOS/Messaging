package com.android.messaging.domain.conversation.usecase.draft

import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapper
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.send.ConversationSendData
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.datamodel.action.InsertNewMessageAction
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.domain.conversation.usecase.draft.exception.BlankConversationIdException
import com.android.messaging.domain.conversation.usecase.draft.exception.ConversationRecipientsNotLoadedException
import com.android.messaging.domain.conversation.usecase.draft.exception.ConversationSimNotReadyException
import com.android.messaging.domain.conversation.usecase.draft.exception.DraftDispatchFailedException
import com.android.messaging.domain.conversation.usecase.draft.exception.EmptyConversationDraftException
import com.android.messaging.domain.conversation.usecase.draft.exception.MissingSelfPhoneNumberForGroupMmsException
import com.android.messaging.domain.conversation.usecase.draft.exception.SendConversationDraftException
import com.android.messaging.domain.conversation.usecase.draft.exception.TooManyVideoAttachmentsException
import com.android.messaging.domain.conversation.usecase.draft.exception.UnknownConversationRecipientException
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.sms.MmsUtils
import com.android.messaging.util.ContentType
import com.android.messaging.util.PhoneUtils
import com.android.messaging.util.core.extension.unitFlow
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

internal interface SendConversationDraft {
    operator fun invoke(
        conversationId: String,
        draft: ConversationDraft,
    ): Flow<Unit>
}

internal class SendConversationDraftImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val getConversationDraftSendProtocol: GetConversationDraftSendProtocol,
    private val conversationDraftMessageDataMapper: ConversationDraftMessageDataMapper,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : SendConversationDraft {

    override operator fun invoke(
        conversationId: String,
        draft: ConversationDraft,
    ): Flow<Unit> {
        return unitFlow {
            try {
                validateAndSendDraft(
                    conversationId = conversationId,
                    draft = draft,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SendConversationDraftException) {
                throw exception
            } catch (exception: Exception) {
                throw DraftDispatchFailedException(
                    conversationId = conversationId,
                    cause = exception,
                )
            }
        }.flowOn(ioDispatcher)
    }

    private fun validateAndSendDraft(
        conversationId: String,
        draft: ConversationDraft,
    ) {
        validateDraftBasics(
            conversationId = conversationId,
            draft = draft,
        )

        val sendData = conversationsRepository.getConversationSendData(
            conversationId = conversationId,
            requestedSelfParticipantId = draft.selfParticipantId,
        ) ?: throw ConversationRecipientsNotLoadedException(
            conversationId = conversationId,
        )

        val selfSubId = resolveSelfSubId(sendData = sendData)
        val sendProtocol = getConversationDraftSendProtocol(
            draft = draft,
            sendData = sendData,
        )
        val shouldSendAsMms = sendProtocol == ConversationDraftSendProtocol.MMS

        validateDraftForSend(
            conversationId = conversationId,
            draft = draft,
            sendData = sendData,
            selfSubId = selfSubId,
            shouldSendAsMms = shouldSendAsMms,
        )

        val message = conversationDraftMessageDataMapper.map(
            conversationId = conversationId,
            draft = draft,
            forceMms = shouldSendAsMms,
        )

        message.consolidateText()

        insertNewMessageWithLegacySelfLock(
            message = message,
            sendData = sendData,
        )
    }

    private fun validateDraftForSend(
        conversationId: String,
        draft: ConversationDraft,
        sendData: ConversationSendData,
        selfSubId: Int,
        shouldSendAsMms: Boolean,
    ) {
        validateKnownRecipients(
            conversationId = conversationId,
            sendData = sendData,
        )

        validateGroupMmsSelfNumber(
            conversationId = conversationId,
            sendData = sendData,
            selfSubId = selfSubId,
            shouldSendAsMms = shouldSendAsMms,
        )
        validateVideoAttachmentLimit(
            conversationId = conversationId,
            attachments = draft.attachments,
        )
    }

    private fun validateDraftBasics(
        conversationId: String,
        draft: ConversationDraft,
    ) {
        if (conversationId.isBlank()) {
            throw BlankConversationIdException()
        }

        if (!draft.hasContent) {
            throw EmptyConversationDraftException(
                conversationId = conversationId,
            )
        }
    }

    private fun validateKnownRecipients(
        conversationId: String,
        sendData: ConversationSendData,
    ) {
        if (!sendData.participants.isLoaded) {
            throw ConversationRecipientsNotLoadedException(
                conversationId = conversationId,
            )
        }

        val hasUnknownSenders = sendData.participants.any { it.isUnknownSender }

        if (hasUnknownSenders) {
            throw UnknownConversationRecipientException(
                conversationId = conversationId,
            )
        }
    }

    private fun resolveSelfSubId(sendData: ConversationSendData): Int {
        return sendData.selfParticipant?.subId ?: ParticipantData.DEFAULT_SELF_SUB_ID
    }

    private fun validateGroupMmsSelfNumber(
        conversationId: String,
        sendData: ConversationSendData,
        selfSubId: Int,
        shouldSendAsMms: Boolean,
    ) {
        if (!sendData.metadata.isGroupConversation || !shouldSendAsMms) {
            return
        }

        try {
            val selfPhoneNumber = PhoneUtils.get(selfSubId).getSelfRawNumber(true)
            if (selfPhoneNumber.isNullOrBlank()) {
                throw MissingSelfPhoneNumberForGroupMmsException(
                    conversationId = conversationId,
                    selfSubId = selfSubId,
                )
            }
        } catch (exception: IllegalStateException) {
            throw ConversationSimNotReadyException(
                conversationId = conversationId,
                selfSubId = selfSubId,
                cause = exception,
            )
        }
    }

    private fun validateVideoAttachmentLimit(
        conversationId: String,
        attachments: Iterable<ConversationDraftAttachment>,
    ) {
        val videoAttachmentCount = attachments.count { attachment ->
            ContentType.isVideoType(attachment.contentType)
        }

        if (videoAttachmentCount > MmsUtils.MAX_VIDEO_ATTACHMENT_COUNT) {
            throw TooManyVideoAttachmentsException(
                conversationId = conversationId,
                videoAttachmentCount = videoAttachmentCount,
            )
        }
    }

    private fun insertNewMessageWithLegacySelfLock(
        message: MessageData,
        sendData: ConversationSendData,
    ) {
        val selfParticipant = sendData.selfParticipant

        val systemDefaultSubId = PhoneUtils.getDefault().defaultSmsSubscriptionId

        val messageHasSelfParticipant = message.selfId != null
        val conversationUsesDefaultSelf = selfParticipant?.isDefaultSelf == true
        val systemDefaultSubIdIsResolved = systemDefaultSubId !=
            ParticipantData.DEFAULT_SELF_SUB_ID

        val shouldLockToSystemDefaultSubId = messageHasSelfParticipant &&
            conversationUsesDefaultSelf &&
            systemDefaultSubIdIsResolved

        when {
            shouldLockToSystemDefaultSubId -> {
                InsertNewMessageAction.insertNewMessage(message, systemDefaultSubId)
            }

            else -> {
                InsertNewMessageAction.insertNewMessage(message)
            }
        }
    }
}
