package com.android.messaging.ui.conversation.messages.ui.attachment

internal typealias OnConversationAttachmentClick = (
    contentType: String,
    contentUri: String,
    partId: String,
) -> Unit
