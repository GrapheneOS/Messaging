package com.android.messaging.ui.conversation.messages.model.text

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationTextLink(
    val start: Int,
    val end: Int,
    val url: String,
)
