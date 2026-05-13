package com.android.messaging.ui.conversationsettings.screen.model

internal sealed interface ConversationSettingsAction {

    data object NotificationsClicked : ConversationSettingsAction

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
}
