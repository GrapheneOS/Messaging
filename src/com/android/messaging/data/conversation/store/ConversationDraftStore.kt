package com.android.messaging.data.conversation.store

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.util.db.ext.withTransaction
import javax.inject.Inject

internal interface ConversationDraftStore {
    fun getSelfParticipantId(conversationId: ConversationId): ParticipantId?

    fun readDraftMessage(
        conversationId: ConversationId,
        selfParticipantId: ParticipantId,
    ): MessageData?

    fun updateDraftMessage(
        conversationId: ConversationId,
        message: MessageData,
    )

    fun moveDraftMessage(
        fromConversationId: ConversationId,
        toConversationId: ConversationId,
        message: MessageData,
    )
}

internal class ConversationDraftStoreImpl @Inject constructor() : ConversationDraftStore {

    override fun getSelfParticipantId(conversationId: ConversationId): ParticipantId? {
        val conversation = ConversationListItemData.getExistingConversation(
            DataModel.get().database,
            conversationId.value,
        ) ?: return null

        return ParticipantId.fromOrNull(conversation.selfId)
    }

    override fun readDraftMessage(
        conversationId: ConversationId,
        selfParticipantId: ParticipantId,
    ): MessageData? {
        return BugleDatabaseOperations.readDraftMessageData(
            DataModel.get().database,
            conversationId.value,
            selfParticipantId.value,
        )
    }

    override fun updateDraftMessage(
        conversationId: ConversationId,
        message: MessageData,
    ) {
        BugleDatabaseOperations.updateDraftMessageData(
            DataModel.get().database,
            conversationId.value,
            message,
            BugleDatabaseOperations.UPDATE_MODE_ADD_DRAFT,
        )
    }

    override fun moveDraftMessage(
        fromConversationId: ConversationId,
        toConversationId: ConversationId,
        message: MessageData,
    ) {
        val database = DataModel.get().database

        database.withTransaction {
            checkNotNull(
                BugleDatabaseOperations.updateDraftMessageData(
                    database,
                    toConversationId.value,
                    message,
                    BugleDatabaseOperations.UPDATE_MODE_ADD_DRAFT,
                ),
            ) {
                "Draft was not written to conversation ${toConversationId.value}"
            }

            // Clearing through updateDraftMessageData would delete the scratch files of the
            // moved attachments, so drop the source draft row first and only reset its snippet
            database.delete(
                DatabaseHelper.MESSAGES_TABLE,
                "${MessageColumns.STATUS}=? AND ${MessageColumns.CONVERSATION_ID}=?",
                arrayOf(
                    MessageData.BUGLE_STATUS_OUTGOING_DRAFT.toString(),
                    fromConversationId.value,
                ),
            )
            BugleDatabaseOperations.updateDraftMessageData(
                database,
                fromConversationId.value,
                null,
                BugleDatabaseOperations.UPDATE_MODE_CLEAR_DRAFT,
            )
        }
    }
}
