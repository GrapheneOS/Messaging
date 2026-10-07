package com.android.messaging.domain.conversation.usecase.action

import com.android.messaging.data.conversation.event.ConversationArchiveEvents
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.repository.ConversationsRepository
import javax.inject.Inject

internal fun interface ArchiveConversation {
    suspend operator fun invoke(conversationId: ConversationId)
}

internal class ArchiveConversationImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val conversationArchiveEvents: ConversationArchiveEvents,
) : ArchiveConversation {

    override suspend fun invoke(conversationId: ConversationId) {
        conversationArchiveEvents.notifyArchived(conversationId = conversationId)
        conversationsRepository.archiveConversation(conversationId = conversationId)
    }
}
