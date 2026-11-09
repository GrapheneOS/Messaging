package com.android.messaging.data.secondaryuser.model

internal sealed interface SecondaryUserNotificationTarget {

    data object Silenced : SecondaryUserNotificationTarget

    data object NoConversation : SecondaryUserNotificationTarget

    data class Conversation(
        val conversationId: String,
        val conversationTitle: String,
    ) : SecondaryUserNotificationTarget
}
