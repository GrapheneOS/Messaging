package com.android.messaging.data.conversation.store

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.MessageData
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
}

internal class ConversationDraftStoreImpl @Inject constructor() : ConversationDraftStore {

    override fun getSelfParticipantId(conversationId: ConversationId): ParticipantId? {
        val conversation = ConversationListItemData.getExistingConversation(
            DataModel.get().database,
            conversationId.value,
        ) ?: return null

        return ParticipantId.fromOrNull(conversation.selfId)?.takeIf { it.isNotBlank() }
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
}
