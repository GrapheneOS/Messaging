package com.android.messaging.ui.conversation.metadata.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationComposerDisabledReason

@Immutable
internal sealed interface ConversationMetadataUiState {
    val composerAvailability: ConversationComposerAvailability

    @Immutable
    sealed interface Avatar {
        @Immutable
        data object Group : Avatar

        @Immutable
        data class Single(
            val photoUri: String?,
            val normalizedDestination: String?,
            val displayName: String?,
        ) : Avatar
    }

    @Immutable
    data object Loading : ConversationMetadataUiState {
        override val composerAvailability = ConversationComposerAvailability.Unavailable(
            reason = ConversationComposerDisabledReason.CONVERSATION_UNAVAILABLE,
        )
    }

    @Immutable
    data class Present(
        val title: String,
        val selfParticipantId: String,
        val avatar: Avatar,
        val participantCount: Int,
        val otherParticipantDisplayDestination: String?,
        val otherParticipantPhoneNumber: String?,
        val otherParticipantContactLookupKey: String?,
        val isArchived: Boolean,
        val isBlocked: Boolean,
        override val composerAvailability: ConversationComposerAvailability,
    ) : ConversationMetadataUiState

    @Immutable
    data object Unavailable : ConversationMetadataUiState {
        override val composerAvailability = ConversationComposerAvailability.Unavailable(
            reason = ConversationComposerDisabledReason.CONVERSATION_UNAVAILABLE,
        )
    }
}
