package com.android.messaging.domain.conversation.usecase.notification

import com.android.messaging.data.conversation.repository.ConversationNotificationRepository
import javax.inject.Inject

internal interface IsConversationSnoozed {
    operator fun invoke(conversationId: String): Boolean
}

internal class IsConversationSnoozedImpl @Inject constructor(
    private val repository: ConversationNotificationRepository,
) : IsConversationSnoozed {

    override fun invoke(conversationId: String): Boolean {
        return repository.isSnoozed(conversationId)
    }
}
