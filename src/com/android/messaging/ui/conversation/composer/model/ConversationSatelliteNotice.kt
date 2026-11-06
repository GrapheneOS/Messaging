package com.android.messaging.ui.conversation.composer.model

/**
 * Tells the user that the subscription messages will be sent from is connected to a satellite
 * network, and which kinds of message the carrier supports over it.
 */
internal enum class ConversationSatelliteNotice {
    TextAndMultimedia,
    TextOnly,
    MessagingUnavailable,
}
