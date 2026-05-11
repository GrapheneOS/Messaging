package com.android.messaging.ui.conversationsettings.screen.model

internal sealed interface ConversationSettingsAction {

    data object NotificationsClicked : ConversationSettingsAction

    data object UnblockClicked : ConversationSettingsAction

    data object BlockConfirmed : ConversationSettingsAction

    data class ParticipantLongPressed(
        val details: String,
    ) : ConversationSettingsAction
}
