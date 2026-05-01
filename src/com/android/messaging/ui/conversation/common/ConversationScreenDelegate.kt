package com.android.messaging.ui.conversation.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal interface ConversationScreenDelegate<T> {
    val state: StateFlow<T>

    fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    )
}
