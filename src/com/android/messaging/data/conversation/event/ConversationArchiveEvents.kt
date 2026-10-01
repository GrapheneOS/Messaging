package com.android.messaging.data.conversation.event

import com.android.messaging.data.conversation.model.ConversationId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal interface ConversationArchiveEvents {
    val archivedConversationIds: Flow<ConversationId>

    fun notifyArchived(conversationId: ConversationId)
}

internal class ConversationArchiveEventsImpl @Inject constructor() : ConversationArchiveEvents {

    private val _archivedConversationIds = Channel<ConversationId>(Channel.BUFFERED)
    override val archivedConversationIds = _archivedConversationIds.receiveAsFlow()

    override fun notifyArchived(conversationId: ConversationId) {
        _archivedConversationIds.trySend(conversationId)
    }
}
