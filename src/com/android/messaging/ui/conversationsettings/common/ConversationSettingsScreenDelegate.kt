package com.android.messaging.ui.conversationsettings.common

import com.android.messaging.data.conversation.model.ConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal interface ConversationSettingsScreenDelegate<T> {
    val state: StateFlow<T>
    val rootConversationId: ConversationId

    fun bind(scope: CoroutineScope)
    fun setConversationId(conversationId: ConversationId)
    fun refresh()
}
