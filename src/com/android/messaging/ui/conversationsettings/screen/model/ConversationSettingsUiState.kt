package com.android.messaging.ui.conversationsettings.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ConversationSettingsUiState(
    val conversationId: String = "",
    val conversationTitle: String = "",
    val isArchived: Boolean = false,
    val participants: ImmutableList<ParticipantUiState> = persistentListOf(),
) {
    val otherParticipant: ParticipantUiState?
        get() = participants.singleOrNull()
}

@Immutable
internal data class ParticipantUiState(
    val participantId: String,
    val avatarUri: String?,
    val displayName: String,
    val details: String?,
    val contactId: Long,
    val lookupKey: String?,
    val normalizedDestination: String?,
    val isBlocked: Boolean,
    val displayDestination: String?,
)
