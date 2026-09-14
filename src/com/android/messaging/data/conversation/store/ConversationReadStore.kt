package com.android.messaging.data.conversation.store

import android.content.ContentValues
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.sms.MmsUtils
import com.android.messaging.util.PendingIntentConstants
import javax.inject.Inject

internal interface ConversationReadStore {
    fun markConversationRead(conversationId: ConversationId)
    fun markConversationUnread(conversationId: ConversationId)
}

internal class ConversationReadStoreImpl @Inject constructor() : ConversationReadStore {

    override fun markConversationRead(conversationId: ConversationId) {
        val database = DataModel.get().database

        val threadId = BugleDatabaseOperations.getThreadId(database, conversationId.value)
        if (threadId != -1L) {
            MmsUtils.updateSmsReadStatus(threadId, Long.MAX_VALUE)
        }

        database.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(MessageColumns.READ, 1)
                put(MessageColumns.SEEN, 1)
            }

            val selection = "(${MessageColumns.READ}!=1 OR ${MessageColumns.SEEN}!=1) AND " +
                "${MessageColumns.CONVERSATION_ID}=?"

            val updatedRows = database.update(
                DatabaseHelper.MESSAGES_TABLE,
                values,
                selection,
                arrayOf(conversationId.value),
            )

            if (updatedRows > 0) {
                MessagingContentProvider.notifyMessagesChanged(conversationId.value)
            }

            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        BugleNotifications.cancel(PendingIntentConstants.SMS_NOTIFICATION_ID, conversationId.value)
    }

    override fun markConversationUnread(conversationId: ConversationId) {
        val database = DataModel.get().database

        database.beginTransaction()
        try {
            val latestMessageId = BugleDatabaseOperations
                .getQueryConversationsLatestMessageStatement(database, conversationId.value)
                .simpleQueryForString()

            if (latestMessageId != null) {
                val values = ContentValues().apply {
                    put(MessageColumns.READ, 0)
                }

                val updatedRows = database.update(
                    DatabaseHelper.MESSAGES_TABLE,
                    values,
                    "${MessageColumns.READ}=1 AND ${MessageColumns._ID}=?",
                    arrayOf(latestMessageId),
                )

                if (updatedRows > 0) {
                    MessagingContentProvider.notifyMessagesChanged(conversationId.value)
                }
            }

            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}
