package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import javax.inject.Inject

internal interface ConversationListActionsDelegate {
    suspend fun setArchived(conversationIds: List<ConversationId>, isArchived: Boolean)
    suspend fun setPinned(conversationIds: List<ConversationId>, isPinned: Boolean)
    suspend fun setRead(conversationIds: List<ConversationId>, isRead: Boolean)
    suspend fun block(conversationId: ConversationId, destination: String): Boolean
    suspend fun unblock(conversationId: ConversationId, destination: String)
    fun delete(items: List<ConversationListItem>)
    fun snooze(conversationIds: List<ConversationId>, option: SnoozeOption)
    fun unsnooze(conversationIds: List<ConversationId>)
}

internal class ConversationListActionsDelegateImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val conversationListRepository: ConversationListRepository,
    private val blockedParticipantsRepository: BlockedParticipantsRepository,
) : ConversationListActionsDelegate {

    override suspend fun setArchived(
        conversationIds: List<ConversationId>,
        isArchived: Boolean,
    ) {
        conversationIds.normalizedIds().forEach { conversationId ->
            when {
                isArchived -> conversationsRepository.archiveConversation(conversationId)
                else -> conversationsRepository.unarchiveConversation(conversationId)
            }
        }
    }

    override suspend fun setPinned(
        conversationIds: List<ConversationId>,
        isPinned: Boolean,
    ) {
        conversationIds.normalizedIds().forEach { conversationId ->
            when {
                isPinned -> conversationsRepository.pinConversation(conversationId)
                else -> conversationsRepository.unpinConversation(conversationId)
            }
        }
    }

    override suspend fun setRead(
        conversationIds: List<ConversationId>,
        isRead: Boolean,
    ) {
        conversationIds.normalizedIds().forEach { conversationId ->
            when {
                isRead -> conversationsRepository.markConversationRead(conversationId)
                else -> conversationsRepository.markConversationUnread(conversationId)
            }
        }
    }

    override suspend fun block(
        conversationId: ConversationId,
        destination: String,
    ): Boolean {
        val resolvedDestination = destination.takeIf(String::isNotBlank) ?: return false

        return blockedParticipantsRepository.setDestinationBlocked(
            destination = resolvedDestination,
            conversationId = conversationId.takeIf { it.isNotBlank() },
            isBlocked = true,
        )
    }

    override suspend fun unblock(
        conversationId: ConversationId,
        destination: String,
    ) {
        val resolvedDestination = destination.takeIf(String::isNotBlank) ?: return

        blockedParticipantsRepository.setDestinationBlocked(
            destination = resolvedDestination,
            conversationId = conversationId.takeIf { it.isNotBlank() },
            isBlocked = false,
        )
    }

    override fun delete(items: List<ConversationListItem>) {
        items.forEach { item ->
            conversationsRepository.deleteConversation(
                conversationId = item.conversationId,
                cutoffTimestamp = item.latestMessage.timestamp,
            )
        }
    }

    override fun snooze(
        conversationIds: List<ConversationId>,
        option: SnoozeOption,
    ) {
        conversationIds.normalizedIds().forEach { conversationId ->
            conversationListRepository.snooze(
                conversationId = conversationId,
                option = option,
            )
        }
    }

    override fun unsnooze(conversationIds: List<ConversationId>) {
        conversationIds.normalizedIds().forEach(conversationListRepository::clearSnooze)
    }

    private fun List<ConversationId>.normalizedIds(): List<ConversationId> {
        return filter { it.isNotBlank() }.distinct()
    }
}
