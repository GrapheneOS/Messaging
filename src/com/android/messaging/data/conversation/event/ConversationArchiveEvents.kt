package com.android.messaging.data.conversation.event

import com.android.messaging.data.conversation.model.ArchivedConversation
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal interface ConversationArchiveEvents {
    val archivedConversations: Flow<ArchivedConversation>

    fun notifyArchived(archivedConversation: ArchivedConversation)
}

internal class ConversationArchiveEventsImpl @Inject constructor() : ConversationArchiveEvents {

    private val _archivedConversations = Channel<ArchivedConversation>(Channel.BUFFERED)
    override val archivedConversations = _archivedConversations.receiveAsFlow()

    override fun notifyArchived(archivedConversation: ArchivedConversation) {
        _archivedConversations.trySend(archivedConversation)
    }
}
