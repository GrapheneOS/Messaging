package com.android.messaging.ui.conversation.navigation

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.data.MessageData

internal interface ConversationDraftLauncher {
    fun launch(
        conversationId: ConversationId,
        draft: MessageData?,
    )
}
