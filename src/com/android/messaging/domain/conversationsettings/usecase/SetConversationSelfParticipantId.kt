package com.android.messaging.domain.conversationsettings.usecase

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.subscription.repository.ConversationSimSelectionRepository
import javax.inject.Inject

internal fun interface SetConversationSelfParticipantId {
    suspend operator fun invoke(conversationId: ConversationId, selfParticipantId: ParticipantId)
}

internal class SetConversationSelfParticipantIdImpl @Inject constructor(
    private val simSelectionRepository: ConversationSimSelectionRepository,
    private val conversationsRepository: ConversationsRepository,
) : SetConversationSelfParticipantId {

    override suspend fun invoke(
        conversationId: ConversationId,
        selfParticipantId: ParticipantId,
    ) {
        if (conversationId.isBlank() || selfParticipantId.isBlank()) return

        simSelectionRepository.setSelectedSelfId(
            conversationId = conversationId,
            selfId = selfParticipantId,
        )
        conversationsRepository.setConversationSelfId(
            conversationId = conversationId,
            selfId = selfParticipantId,
        )
    }
}
