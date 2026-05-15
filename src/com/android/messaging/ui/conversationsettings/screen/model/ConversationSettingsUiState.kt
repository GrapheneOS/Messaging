package com.android.messaging.ui.conversationsettings.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.subscription.model.Subscription
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ConversationSettingsUiState(
    val conversationId: String = "",
    val conversationTitle: String = "",
    val isArchived: Boolean = false,
    val isSnoozed: Boolean = false,
    val participants: ImmutableList<ParticipantUiState> = persistentListOf(),
    val selfParticipantId: String = "",
    val availableSubscriptions: ImmutableList<Subscription> = persistentListOf(),
    val canCall: Boolean = false,
) {
    val otherParticipant: ParticipantUiState?
        get() = participants.singleOrNull()

    val canShowContact: Boolean
        get() = !otherParticipant?.normalizedDestination.isNullOrBlank()

    val isContactSaved: Boolean
        get() {
            val participant = otherParticipant ?: return false
            return participant.contactId > 0 && !participant.lookupKey.isNullOrBlank()
        }

    val selectedSubscription: Subscription?
        get() {
            return availableSubscriptions.firstOrNull { it.selfParticipantId == selfParticipantId }
                ?: availableSubscriptions.firstOrNull()
        }

    val isSimSwitchAvailable: Boolean
        get() = availableSubscriptions.size > 1
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
