package com.android.messaging.data.conversation.model.recipient

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationRecipient(
    val id: String,
    val displayName: String,
    val destination: String,
    val photoUri: String? = null,
    val secondaryText: String? = null,
)
