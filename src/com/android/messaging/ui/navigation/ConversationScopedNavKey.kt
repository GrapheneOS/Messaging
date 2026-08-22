package com.android.messaging.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId

internal interface ConversationScopedNavKey : NavKey {
    val conversationId: ConversationId
}
