package com.android.messaging.data.conversation.store

import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.MessagingContentProvider
import javax.inject.Inject

internal interface ConversationPinStore {
    fun pinConversation(conversationId: String)
    fun unpinConversation(conversationId: String)
}

internal class ConversationPinStoreImpl @Inject constructor() : ConversationPinStore {

    override fun pinConversation(conversationId: String) {
        setPinned(
            conversationId = conversationId,
            isPinned = true,
        )
    }

    override fun unpinConversation(conversationId: String) {
        setPinned(
            conversationId = conversationId,
            isPinned = false,
        )
    }

    private fun setPinned(
        conversationId: String,
        isPinned: Boolean,
    ) {
        val database = DataModel.get().database

        database.beginTransaction()
        try {
            BugleDatabaseOperations.updateConversationPinStatusInTransaction(
                database,
                conversationId,
                isPinned,
            )
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        MessagingContentProvider.notifyConversationListChanged()
        MessagingContentProvider.notifyConversationMetadataChanged(conversationId)
    }
}
