package com.android.messaging.data.conversation.store

import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.MessageData
import javax.inject.Inject

internal interface ConversationDraftStore {
    fun getSelfParticipantId(conversationId: String): String?

    fun readDraftMessage(
        conversationId: String,
        selfParticipantId: String,
    ): MessageData?

    fun updateDraftMessage(
        conversationId: String,
        message: MessageData,
    )
}

internal class ConversationDraftStoreImpl @Inject constructor() : ConversationDraftStore {

    override fun getSelfParticipantId(conversationId: String): String? {
        val conversation = ConversationListItemData.getExistingConversation(
            DataModel.get().database,
            conversationId,
        ) ?: return null

        return conversation.selfId?.takeIf { it.isNotBlank() }
    }

    override fun readDraftMessage(
        conversationId: String,
        selfParticipantId: String,
    ): MessageData? {
        return BugleDatabaseOperations.readDraftMessageData(
            DataModel.get().database,
            conversationId,
            selfParticipantId,
        )
    }

    override fun updateDraftMessage(
        conversationId: String,
        message: MessageData,
    ) {
        BugleDatabaseOperations.updateDraftMessageData(
            DataModel.get().database,
            conversationId,
            message,
            BugleDatabaseOperations.UPDATE_MODE_ADD_DRAFT,
        )
    }
}
