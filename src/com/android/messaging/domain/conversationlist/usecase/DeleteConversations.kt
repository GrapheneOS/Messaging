package com.android.messaging.domain.conversationlist.usecase

import com.android.messaging.datamodel.action.DeleteConversationAction
import com.android.messaging.domain.conversationlist.model.ConversationListActionTarget
import javax.inject.Inject

internal interface DeleteConversations {
    operator fun invoke(conversations: Collection<ConversationListActionTarget>)
}

internal class DeleteConversationsImpl @Inject constructor() : DeleteConversations {

    override operator fun invoke(conversations: Collection<ConversationListActionTarget>) {
        conversations
            .asSequence()
            .filter { conversation ->
                conversation.conversationId.isNotBlank()
            }
            .forEach { conversation ->
                DeleteConversationAction.deleteConversation(
                    conversation.conversationId,
                    conversation.cutoffTimestampMillis,
                )
            }
    }
}
