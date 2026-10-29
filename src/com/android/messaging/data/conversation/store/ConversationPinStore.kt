package com.android.messaging.data.conversation.store

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.MessagingContentProvider
import javax.inject.Inject

internal interface ConversationPinStore {
    fun isConversationPinned(conversationId: ConversationId): Boolean
    fun pinConversation(conversationId: ConversationId)
    fun unpinConversation(conversationId: ConversationId)
}

internal class ConversationPinStoreImpl @Inject constructor() : ConversationPinStore {

    override fun isConversationPinned(conversationId: ConversationId): Boolean {
        return DataModel.get().database.query(
            DatabaseHelper.CONVERSATIONS_TABLE,
            PINNED_PROJECTION,
            CONVERSATION_SELECTION,
            arrayOf(conversationId.value),
            null,
            null,
            null,
        ).use { cursor ->
            cursor.moveToFirst() && cursor.getInt(0) == 1
        }
    }

    override fun pinConversation(conversationId: ConversationId) {
        setPinned(
            conversationId = conversationId,
            isPinned = true,
        )
    }

    override fun unpinConversation(conversationId: ConversationId) {
        setPinned(
            conversationId = conversationId,
            isPinned = false,
        )
    }

    private fun setPinned(
        conversationId: ConversationId,
        isPinned: Boolean,
    ) {
        val database = DataModel.get().database

        database.beginTransaction()
        try {
            BugleDatabaseOperations.updateConversationPinStatusInTransaction(
                database,
                conversationId.value,
                isPinned,
            )
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        MessagingContentProvider.notifyConversationListChanged()
        MessagingContentProvider.notifyConversationMetadataChanged(conversationId.value)
    }

    private companion object {
        private val PINNED_PROJECTION = arrayOf(ConversationColumns.PINNED)
        private const val CONVERSATION_SELECTION = "${ConversationColumns._ID}=?"
    }
}
