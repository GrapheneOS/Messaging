package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import javax.inject.Inject

internal interface ConversationListActionsDelegate {
    suspend fun setArchived(conversationIds: List<String>, isArchived: Boolean)
    suspend fun setPinned(conversationIds: List<String>, isPinned: Boolean)
    suspend fun setRead(conversationIds: List<String>, isRead: Boolean)
    suspend fun block(conversationId: String, destination: String): Boolean
    suspend fun unblock(conversationId: String, destination: String)
    fun delete(items: List<ConversationListItem>)
    fun snooze(conversationIds: List<String>, option: SnoozeOption)
    fun unsnooze(conversationIds: List<String>)
}

internal class ConversationListActionsDelegateImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val conversationListRepository: ConversationListRepository,
    private val blockedParticipantsRepository: BlockedParticipantsRepository,
) : ConversationListActionsDelegate {

    override suspend fun setArchived(
        conversationIds: List<String>,
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
        conversationIds: List<String>,
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
        conversationIds: List<String>,
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
        conversationId: String,
        destination: String,
    ): Boolean {
        val resolvedDestination = destination.takeIf(String::isNotBlank) ?: return false

        return blockedParticipantsRepository.setDestinationBlocked(
            destination = resolvedDestination,
            conversationId = conversationId.takeIf(String::isNotBlank),
            isBlocked = true,
        )
    }

    override suspend fun unblock(
        conversationId: String,
        destination: String,
    ) {
        val resolvedDestination = destination.takeIf(String::isNotBlank) ?: return

        blockedParticipantsRepository.setDestinationBlocked(
            destination = resolvedDestination,
            conversationId = conversationId.takeIf(String::isNotBlank),
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
        conversationIds: List<String>,
        option: SnoozeOption,
    ) {
        conversationIds.normalizedIds().forEach { conversationId ->
            conversationListRepository.snooze(
                conversationId = conversationId,
                option = option,
            )
        }
    }

    override fun unsnooze(conversationIds: List<String>) {
        conversationIds.normalizedIds().forEach(conversationListRepository::clearSnooze)
    }

    private fun List<String>.normalizedIds(): List<String> {
        return filter(String::isNotBlank).distinct()
    }
}
