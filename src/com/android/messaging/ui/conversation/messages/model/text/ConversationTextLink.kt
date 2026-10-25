package com.android.messaging.ui.conversation.messages.model.text

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationTextLink(
    val start: Int,
    val end: Int,
    val url: String,
    val kind: ConversationTextLinkKind,
    val originalText: String,
)

internal enum class ConversationTextLinkKind {
    Web,
    Phone,
    Email,
    Address,
}
