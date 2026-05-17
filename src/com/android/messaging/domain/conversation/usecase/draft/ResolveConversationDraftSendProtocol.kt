package com.android.messaging.domain.conversation.usecase.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.send.ConversationSendData
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.util.LogUtil
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

internal interface ResolveConversationDraftSendProtocol {
    suspend operator fun invoke(
        conversationId: String?,
        draft: ConversationDraft,
    ): ConversationDraftSendProtocol
}

internal class ResolveConversationDraftSendProtocolImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val getConversationDraftSendProtocol: GetConversationDraftSendProtocol,
) : ResolveConversationDraftSendProtocol {

    @Suppress("TooGenericExceptionCaught")
    override suspend operator fun invoke(
        conversationId: String?,
        draft: ConversationDraft,
    ): ConversationDraftSendProtocol {
        return try {
            val sendData = resolveConversationSendData(
                conversationId = conversationId,
                draft = draft,
            )

            when (sendData) {
                null -> fallbackDraftSendProtocol(draft = draft)
                else -> {
                    getConversationDraftSendProtocol(
                        draft = draft,
                        sendData = sendData,
                    )
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            LogUtil.e(
                TAG,
                "Failed to resolve draft send protocol for conversation $conversationId",
                exception,
            )

            fallbackDraftSendProtocol(draft = draft)
        }
    }

    private suspend fun resolveConversationSendData(
        conversationId: String?,
        draft: ConversationDraft,
    ): ConversationSendData? {
        return when {
            draft.hasContent && !conversationId.isNullOrBlank() -> {
                conversationsRepository.getConversationSendData(
                    conversationId = conversationId,
                    requestedSelfParticipantId = draft.selfParticipantId,
                )
            }

            else -> null
        }
    }

    private fun fallbackDraftSendProtocol(
        draft: ConversationDraft,
    ): ConversationDraftSendProtocol {
        return when {
            draft.isMms -> ConversationDraftSendProtocol.MMS
            else -> ConversationDraftSendProtocol.SMS
        }
    }

    private companion object {
        private const val TAG = "ResolveDraftSendProtocol"
    }
}
