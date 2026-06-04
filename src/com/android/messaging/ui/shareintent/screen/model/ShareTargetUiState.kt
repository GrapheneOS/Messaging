package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ShareTargetUiState {
    val key: String
    val selectionId: String
    val displayName: String
    val details: String?
    val avatarUri: String?

    @Immutable
    data class Conversation(
        val conversationId: String,
        val normalizedDestination: String?,
        override val displayName: String,
        override val details: String?,
        override val avatarUri: String?,
        val isGroup: Boolean,
    ) : ShareTargetUiState {
        override val key: String = "$CONVERSATION_KEY_PREFIX$conversationId"

        override val selectionId: String = when {
            normalizedDestination.isNullOrEmpty() -> key
            else -> "$DESTINATION_SELECTION_PREFIX$normalizedDestination"
        }
    }

    @Immutable
    data class Contact(
        val contactId: Long,
        val destination: String,
        val normalizedDestination: String,
        override val displayName: String,
        override val details: String?,
        override val avatarUri: String?,
    ) : ShareTargetUiState {
        override val key: String = "$CONTACT_KEY_PREFIX$contactId"
        override val selectionId: String = "$DESTINATION_SELECTION_PREFIX$normalizedDestination"
    }

    private companion object {
        private const val CONVERSATION_KEY_PREFIX = "conversation:"
        private const val CONTACT_KEY_PREFIX = "contact:"
        private const val DESTINATION_SELECTION_PREFIX = "dest:"
    }
}
