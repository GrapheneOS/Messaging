package com.android.messaging.domain.shareintent.model

internal sealed interface ShareSendTarget {

    data class Conversation(
        val conversationId: String,
    ) : ShareSendTarget

    data class Contact(
        val destination: String,
    ) : ShareSendTarget
}
