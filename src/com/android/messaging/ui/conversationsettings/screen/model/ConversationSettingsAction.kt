package com.android.messaging.ui.conversationsettings.screen.model

import com.android.messaging.data.conversationsettings.model.SnoozeOption

internal sealed interface ConversationSettingsAction {

    data object NotificationsClicked : ConversationSettingsAction

    data class SnoozeOptionSelected(
        val option: SnoozeOption,
    ) : ConversationSettingsAction

    data object UnsnoozeClicked : ConversationSettingsAction

    data object UnarchiveClicked : ConversationSettingsAction

    data object ArchiveClicked : ConversationSettingsAction

    data object UnblockClicked : ConversationSettingsAction

    data object BlockConfirmed : ConversationSettingsAction

    data class SimSelected(
        val selfParticipantId: String,
    ) : ConversationSettingsAction
}

internal sealed interface ParticipantConversationSettingsAction : ConversationSettingsAction {

    data class ParticipantPressed(
        val destination: String,
    ) : ParticipantConversationSettingsAction

    data class ParticipantLongPressed(
        val details: String,
    ) : ParticipantConversationSettingsAction

    data class ParticipantActionPressed(
        val destination: String,
    ) : ParticipantConversationSettingsAction

    data class ParticipantCallClicked(
        val destination: String,
    ) : ParticipantConversationSettingsAction

    data class ParticipantContactInfoClicked(
        val participant: ParticipantUiState,
    ) : ParticipantConversationSettingsAction
}
