package com.android.messaging.ui.conversationpicker.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface TargetUiState {
    val key: String
    val selectionId: String
    val displayName: String
    val details: String?
    val avatarUri: String?
    val normalizedDestination: String?

    @Immutable
    data class Conversation(
        val conversationId: String,
        override val normalizedDestination: String?,
        override val displayName: String,
        override val details: String?,
        override val avatarUri: String?,
        val isGroup: Boolean,
    ) : TargetUiState {
        override val key: String = "$CONVERSATION_KEY_PREFIX$conversationId"

        override val selectionId: String = when {
            normalizedDestination.isNullOrEmpty() -> key
            else -> "$DESTINATION_SELECTION_PREFIX$normalizedDestination"
        }
    }

    @Immutable
    data class Contact(
        val destination: String,
        override val normalizedDestination: String,
        override val displayName: String,
        override val details: String?,
        override val avatarUri: String?,
    ) : TargetUiState {
        override val key: String = "$DESTINATION_SELECTION_PREFIX$normalizedDestination"
        override val selectionId: String = key
    }

    private companion object {
        private const val CONVERSATION_KEY_PREFIX = "conversation:"
        private const val DESTINATION_SELECTION_PREFIX = "dest:"
    }
}
