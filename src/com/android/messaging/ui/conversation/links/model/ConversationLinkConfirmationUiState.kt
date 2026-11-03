package com.android.messaging.ui.conversation.links.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationLinkConfirmationUiState(
    val uri: String,
    val destination: String,
    val showsFullUri: Boolean,
)
