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

    data class ParticipantPressed(
        val destination: String,
    ) : ConversationSettingsAction

    data class ParticipantLongPressed(
        val details: String,
    ) : ConversationSettingsAction

    data class ParticipantActionPressed(
        val destination: String,
    ) : ConversationSettingsAction

    data class SimSelected(
        val selfParticipantId: String,
    ) : ConversationSettingsAction

    data object CallClicked : ConversationSettingsAction

    data object ContactInfoClicked : ConversationSettingsAction
}
