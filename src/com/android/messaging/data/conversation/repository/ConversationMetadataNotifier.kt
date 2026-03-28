package com.android.messaging.data.conversation.repository

import com.android.messaging.datamodel.MessagingContentProvider
import javax.inject.Inject

internal interface ConversationMetadataNotifier {
    fun notifyConversationMetadataChanged(conversationId: String)
}

internal class ConversationMetadataNotifierImpl @Inject constructor() :
    ConversationMetadataNotifier {

    override fun notifyConversationMetadataChanged(conversationId: String) {
        MessagingContentProvider.notifyConversationMetadataChanged(conversationId)
    }
}
