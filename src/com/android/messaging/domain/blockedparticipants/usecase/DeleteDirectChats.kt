package com.android.messaging.domain.blockedparticipants.usecase

import com.android.messaging.datamodel.action.DeleteConversationAction
import javax.inject.Inject

internal interface DeleteDirectChats {
    operator fun invoke(conversationIds: List<String>)
}

internal class DeleteDirectChatsImpl @Inject constructor() : DeleteDirectChats {

    override operator fun invoke(conversationIds: List<String>) {
        conversationIds.forEach { conversationId ->
            DeleteConversationAction.deleteConversation(conversationId, Long.MAX_VALUE)
        }
    }
}
