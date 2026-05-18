package com.android.messaging.data.conversationsettings.model

internal data class LegacyConversationNotificationPrefs(
    val notificationsEnabled: Boolean,
    val ringtoneString: String?,
    val vibrationEnabled: Boolean,
) {
    companion object {
        val Default = LegacyConversationNotificationPrefs(
            notificationsEnabled = true,
            ringtoneString = null,
            vibrationEnabled = false,
        )
    }
}
