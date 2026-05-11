package com.android.messaging.ui.conversationsettings.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.datamodel.data.ParticipantData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ConversationSettingsUiState(
    val conversationId: String = "",
    val conversationTitle: String = "",
    val legacyNotificationEnabled: Boolean = false,
    val legacyRingtoneString: String? = null,
    val legacyVibrationEnabled: Boolean = false,
    val otherParticipant: ParticipantData? = null,
    val participants: ImmutableList<ParticipantUiState> = persistentListOf(),
)

@Immutable
internal data class ParticipantUiState(
    val participantId: String,
    val avatarUri: String?,
    val displayName: String,
    val details: String?,
    val contactId: Long,
    val lookupKey: String?,
    val normalizedDestination: String?,
)
