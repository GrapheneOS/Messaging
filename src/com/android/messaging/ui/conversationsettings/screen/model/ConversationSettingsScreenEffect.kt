package com.android.messaging.ui.conversationsettings.screen.model

internal sealed interface ConversationSettingsScreenEffect {

    data class OpenNotificationChannelSettings(
        val conversationId: String,
        val conversationTitle: String,
        val legacyNotificationEnabled: Boolean,
        val legacyRingtoneString: String?,
        val legacyVibrationEnabled: Boolean,
    ) : ConversationSettingsScreenEffect

    data object FinishAfterBlock : ConversationSettingsScreenEffect

    data class CopyToClipboard(
        val text: String,
    ) : ConversationSettingsScreenEffect
}
