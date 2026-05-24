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
    val otherParticipant: ParticipantUiState? = null,
    val selfParticipantId: String = "",
    val availableSubscriptions: ImmutableList<Subscription> = persistentListOf(),
    val selectedSubscription: Subscription? = null,
    val isSimSwitchAvailable: Boolean = false,
    val canCall: Boolean = false,
    val canShowContact: Boolean = false,
    val isContactSaved: Boolean = false,
)

@Immutable
internal data class ParticipantUiState(
    val avatarUri: String?,
    val displayName: String,
    val details: String?,
    val contactId: Long,
    val lookupKey: String?,
    val normalizedDestination: String?,
    val isBlocked: Boolean,
    val displayDestination: String?,
    val canCall: Boolean,
    val isContactSaved: Boolean,
)
