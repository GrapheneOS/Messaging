package com.android.messaging.ui.blockedparticipants.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal data class BlockedParticipantsUiState(
    val isLoading: Boolean = true,
    val participants: ImmutableList<BlockedParticipantUiState> = persistentListOf(),
    val selectedParticipantIds: PersistentSet<String> = persistentSetOf(),
)

@Immutable
internal data class BlockedParticipantUiState(
    val participantId: String,
    val conversationId: String,
    val avatarUri: String?,
    val displayName: String,
    val details: String?,
    val contactId: Long,
    val lookupKey: String?,
    val normalizedDestination: String?,
    val canCall: Boolean,
    val isContactSaved: Boolean,
)
