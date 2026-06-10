package com.android.messaging.ui.conversationlist.redesign.delegate

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListEffect
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal interface ConversationListActionsDelegate {
    val effects: Flow<ConversationListEffect>

    fun bind(scope: CoroutineScope)
    fun setArchived(items: List<ConversationListItem>, isArchived: Boolean)
    fun delete(items: List<ConversationListItem>)
    fun block(item: ConversationListItem)
}

internal class ConversationListActionsDelegateImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val blockedParticipantsRepository: BlockedParticipantsRepository,
) : ConversationListActionsDelegate {

    private val _effects = MutableSharedFlow<ConversationListEffect>(extraBufferCapacity = 1)
    override val effects: Flow<ConversationListEffect> = _effects.asSharedFlow()

    private var boundScope: CoroutineScope? = null

    override fun bind(scope: CoroutineScope) {
        if (boundScope != null) {
            return
        }

        boundScope = scope
    }

    override fun setArchived(
        items: List<ConversationListItem>,
        isArchived: Boolean,
    ) {
        if (items.isEmpty()) {
            return
        }

        items.forEach { item ->
            when {
                isArchived -> conversationsRepository.archiveConversation(item.conversationId)
                else -> conversationsRepository.unarchiveConversation(item.conversationId)
            }
        }

        _effects.tryEmit(
            ConversationListEffect.ConversationsArchived(
                count = items.size,
                isArchived = isArchived,
            ),
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

    override fun block(item: ConversationListItem) {
        val destination = item
            .participant
            .otherNormalizedDestination
            ?.takeIf(String::isNotBlank)
            ?: return

        boundScope?.launch {
            val success = blockedParticipantsRepository.setDestinationBlocked(
                destination = destination,
                conversationId = item.conversationId,
                isBlocked = true,
            )

            _effects.emit(
                ConversationListEffect.ConversationBlocked(
                    destination = destination,
                    success = success,
                ),
            )
        }
    }
}
