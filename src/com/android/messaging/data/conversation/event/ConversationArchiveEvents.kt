package com.android.messaging.data.conversation.event

import com.android.messaging.data.conversation.model.ArchivedConversation
import com.android.messaging.data.conversation.model.ConversationId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal interface ConversationArchiveEvents {
    val archivedConversations: Flow<ArchivedConversation>
    val unarchivedConversationIds: Flow<ConversationId>

    fun notifyArchived(archivedConversation: ArchivedConversation)
    fun notifyUnarchived(conversationId: ConversationId)
}

internal class ConversationArchiveEventsImpl @Inject constructor() : ConversationArchiveEvents {

    private val _archivedConversations = Channel<ArchivedConversation>(Channel.BUFFERED)
    override val archivedConversations = _archivedConversations.receiveAsFlow()

    private val _unarchivedConversationIds = Channel<ConversationId>(Channel.BUFFERED)
    override val unarchivedConversationIds = _unarchivedConversationIds.receiveAsFlow()

    override fun notifyArchived(archivedConversation: ArchivedConversation) {
        _archivedConversations.trySend(archivedConversation)
    }

    override fun notifyUnarchived(conversationId: ConversationId) {
        _unarchivedConversationIds.trySend(conversationId)
    }
}
