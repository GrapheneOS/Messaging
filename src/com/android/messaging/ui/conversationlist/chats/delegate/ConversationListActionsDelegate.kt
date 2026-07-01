package com.android.messaging.ui.conversationlist.chats.delegate

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.ui.conversationlist.chats.model.ConversationListEffect
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal interface ConversationListActionsDelegate {
    val effects: Flow<ConversationListEffect>

    fun bind(scope: CoroutineScope)

    fun setArchived(
        conversationIds: List<String>,
        isArchived: Boolean,
        shouldShowSnackbar: Boolean,
    )

    fun setPinned(conversationIds: List<String>, isPinned: Boolean)
    fun setRead(conversationIds: List<String>, isRead: Boolean)
    fun snooze(conversationIds: List<String>, option: SnoozeOption)
    fun unsnooze(conversationIds: List<String>)
    fun delete(items: List<ConversationListItem>)
    fun block(conversationId: String, destination: String)
    fun unblock(conversationId: String, destination: String)
}

internal class ConversationListActionsDelegateImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val conversationListRepository: ConversationListRepository,
    private val blockedParticipantsRepository: BlockedParticipantsRepository,
) : ConversationListActionsDelegate {

    private val _effects = Channel<ConversationListEffect>(Channel.BUFFERED)
    override val effects: Flow<ConversationListEffect> = _effects.receiveAsFlow()

    private var boundScope: CoroutineScope? = null

    override fun bind(scope: CoroutineScope) {
        if (boundScope != null) {
            return
        }

        boundScope = scope
    }

    override fun setArchived(
        conversationIds: List<String>,
        isArchived: Boolean,
        shouldShowSnackbar: Boolean,
    ) {
        val resolvedConversationIds = conversationIds.normalizedIds()

        if (resolvedConversationIds.isEmpty()) {
            return
        }

        boundScope?.launch {
            resolvedConversationIds.forEach { conversationId ->
                when {
                    isArchived -> conversationsRepository.archiveConversation(conversationId)
                    else -> conversationsRepository.unarchiveConversation(conversationId)
                }
            }
        }

        if (!shouldShowSnackbar) {
            return
        }

        _effects.trySend(
            ConversationListEffect.ArchiveStatusChanged(
                conversationIds = resolvedConversationIds.toImmutableList(),
                isArchived = isArchived,
            ),
        )
    }

    override fun setPinned(
        conversationIds: List<String>,
        isPinned: Boolean,
    ) {
        val resolvedConversationIds = conversationIds.normalizedIds()

        if (resolvedConversationIds.isEmpty()) {
            return
        }

        boundScope?.launch {
            resolvedConversationIds.forEach { conversationId ->
                when {
                    isPinned -> conversationsRepository.pinConversation(conversationId)
                    else -> conversationsRepository.unpinConversation(conversationId)
                }
            }
        }
    }

    override fun setRead(
        conversationIds: List<String>,
        isRead: Boolean,
    ) {
        val resolvedConversationIds = conversationIds.normalizedIds()

        if (resolvedConversationIds.isEmpty()) {
            return
        }

        boundScope?.launch {
            resolvedConversationIds.forEach { conversationId ->
                when {
                    isRead -> conversationsRepository.markConversationRead(conversationId)
                    else -> conversationsRepository.markConversationUnread(conversationId)
                }
            }
        }
    }

    override fun snooze(
        conversationIds: List<String>,
        option: SnoozeOption,
    ) {
        val resolvedConversationIds = conversationIds.normalizedIds()

        if (resolvedConversationIds.isEmpty()) {
            return
        }

        resolvedConversationIds.forEach { conversationId ->
            conversationListRepository.snooze(
                conversationId = conversationId,
                option = option,
            )
        }
    }

    override fun unsnooze(conversationIds: List<String>) {
        val resolvedConversationIds = conversationIds.normalizedIds()

        if (resolvedConversationIds.isEmpty()) {
            return
        }

        resolvedConversationIds.forEach(conversationListRepository::clearSnooze)
    }

    override fun delete(items: List<ConversationListItem>) {
        items.forEach { item ->
            conversationsRepository.deleteConversation(
                conversationId = item.conversationId,
                cutoffTimestamp = item.latestMessage.timestamp,
            )
        }
    }

    override fun block(
        conversationId: String,
        destination: String,
    ) {
        val resolvedDestination = destination.takeIf(String::isNotBlank) ?: return

        boundScope?.launch {
            val success = blockedParticipantsRepository.setDestinationBlocked(
                destination = resolvedDestination,
                conversationId = conversationId.takeIf(String::isNotBlank),
                isBlocked = true,
            )

            _effects.trySend(
                ConversationListEffect.ConversationBlocked(
                    conversationId = conversationId,
                    destination = resolvedDestination,
                    success = success,
                ),
            )
        }
    }

    override fun unblock(
        conversationId: String,
        destination: String,
    ) {
        val resolvedDestination = destination.takeIf(String::isNotBlank) ?: return

        boundScope?.launch {
            blockedParticipantsRepository.setDestinationBlocked(
                destination = resolvedDestination,
                conversationId = conversationId.takeIf(String::isNotBlank),
                isBlocked = false,
            )
        }
    }

    private fun List<String>.normalizedIds(): List<String> {
        return filter(String::isNotBlank).distinct()
    }
}
