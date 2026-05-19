package com.android.messaging.ui.conversationsettings.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal interface ConversationSettingsScreenDelegate<T> {
    val state: StateFlow<T>

    fun bind(conversationIdFlow: StateFlow<String?>, scope: CoroutineScope)
    fun refresh()
}
