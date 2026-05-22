package com.android.messaging.domain.blockedparticipants.usecase

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.datamodel.action.DeleteConversationAction
import javax.inject.Inject

internal interface DeleteDirectChats {
    suspend operator fun invoke(normalizedDestinations: List<String>)
}

internal class DeleteDirectChatsImpl @Inject constructor(
    private val repository: BlockedParticipantsRepository,
) : DeleteDirectChats {

    override suspend operator fun invoke(normalizedDestinations: List<String>) {
        val destinations = normalizedDestinations
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

        if (destinations.isEmpty()) return

        val conversationIds = repository.findDirectConversationIds(destinations)

        conversationIds.forEach { conversationId ->
            DeleteConversationAction.deleteConversation(conversationId, Long.MAX_VALUE)
        }
    }
}
