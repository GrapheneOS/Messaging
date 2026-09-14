package com.android.messaging.ui.blockedparticipants.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal data class BlockedParticipantsUiState(
    val isLoading: Boolean = true,
    val participants: ImmutableList<BlockedParticipantUiState> = persistentListOf(),
    val selectedParticipantIds: PersistentSet<ParticipantId> = persistentSetOf(),
)

@Immutable
internal data class BlockedParticipantUiState(
    val participantId: ParticipantId,
    val conversationId: ConversationId,
    val avatarUri: String?,
    val displayName: String,
    val details: String?,
    val contactId: Long,
    val lookupKey: String?,
    val normalizedDestination: String?,
    val canCall: Boolean,
    val canShowContact: Boolean,
    val isContactSaved: Boolean,
)
