package com.android.messaging.data.conversation.store

import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.MessagingContentProvider
import javax.inject.Inject

internal interface ConversationArchiveStore {
    fun archiveConversation(conversationId: String)
    fun unarchiveConversation(conversationId: String)
}

internal class ConversationArchiveStoreImpl @Inject constructor() : ConversationArchiveStore {

    override fun archiveConversation(conversationId: String) {
        setArchived(
            conversationId = conversationId,
            isArchived = true,
        )
    }

    override fun unarchiveConversation(conversationId: String) {
        setArchived(
            conversationId = conversationId,
            isArchived = false,
        )
    }

    private fun setArchived(
        conversationId: String,
        isArchived: Boolean,
    ) {
        val database = DataModel.get().database

        database.beginTransaction()
        try {
            BugleDatabaseOperations.updateConversationArchiveStatusInTransaction(
                database,
                conversationId,
                isArchived,
            )
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        MessagingContentProvider.notifyConversationListChanged()
        MessagingContentProvider.notifyConversationMetadataChanged(conversationId)
    }
}
