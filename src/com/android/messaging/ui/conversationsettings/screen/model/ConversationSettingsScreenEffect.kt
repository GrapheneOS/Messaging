package com.android.messaging.ui.conversationsettings.screen.model

import com.android.messaging.data.conversationsettings.model.LegacyConversationNotificationPrefs

internal sealed interface ConversationSettingsScreenEffect {

    data class OpenNotificationChannelSettings(
        val conversationId: String,
        val conversationTitle: String,
        val legacyPrefs: LegacyConversationNotificationPrefs,
    ) : ConversationSettingsScreenEffect

    data class OpenParticipantChat(
        val conversationId: String,
    ) : ConversationSettingsScreenEffect

    data class CopyToClipboard(
        val text: String,
    ) : ConversationSettingsScreenEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : ConversationSettingsScreenEffect

    data class PlacePhoneCall(
        val destination: String,
    ) : ConversationSettingsScreenEffect

    data class ShowOrAddContact(
        val contactId: Long,
        val contactLookupKey: String?,
        val avatarUri: String?,
        val normalizedDestination: String?,
    ) : ConversationSettingsScreenEffect
}
