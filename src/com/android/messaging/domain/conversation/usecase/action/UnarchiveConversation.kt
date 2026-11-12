package com.android.messaging.domain.conversation.usecase.action

import com.android.messaging.data.conversation.event.ConversationArchiveEvents
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.repository.ConversationsRepository
import javax.inject.Inject

internal fun interface UnarchiveConversation {
    suspend operator fun invoke(conversationId: ConversationId)
}

internal class UnarchiveConversationImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val conversationArchiveEvents: ConversationArchiveEvents,
) : UnarchiveConversation {

    override suspend fun invoke(conversationId: ConversationId) {
        conversationArchiveEvents.notifyUnarchived(conversationId = conversationId)
        conversationsRepository.unarchiveConversation(conversationId = conversationId)
    }
}
