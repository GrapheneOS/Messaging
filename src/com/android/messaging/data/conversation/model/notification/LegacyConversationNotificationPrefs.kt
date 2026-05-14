package com.android.messaging.data.conversation.model.notification

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
