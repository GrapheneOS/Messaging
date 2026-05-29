package com.android.messaging.domain.conversationsettings.usecase

import com.android.messaging.data.conversation.repository.ConversationsRepository
import javax.inject.Inject

internal fun interface SetConversationArchived {
    operator fun invoke(conversationId: String, archived: Boolean)
}

internal class SetConversationArchivedImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
) : SetConversationArchived {

    override fun invoke(
        conversationId: String,
        archived: Boolean,
    ) {
        if (conversationId.isEmpty()) return

        if (archived) {
            conversationsRepository.archiveConversation(conversationId)
        } else {
            conversationsRepository.unarchiveConversation(conversationId)
        }
    }
}
