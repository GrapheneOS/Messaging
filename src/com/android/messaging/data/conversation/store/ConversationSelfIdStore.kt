package com.android.messaging.data.conversation.store

import android.content.ContentValues
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import javax.inject.Inject

internal interface ConversationSelfIdStore {
    fun updateSelfId(conversationId: ConversationId, selfId: ParticipantId)
}

internal class ConversationSelfIdStoreImpl @Inject constructor() : ConversationSelfIdStore {

    override fun updateSelfId(
        conversationId: ConversationId,
        selfId: ParticipantId,
    ) {
        val values = ContentValues().apply {
            put(ConversationColumns.CURRENT_SELF_ID, selfId.value)
        }

        BugleDatabaseOperations.updateConversationRowIfExists(
            DataModel.get().database,
            conversationId.value,
            values,
        )
    }
}
