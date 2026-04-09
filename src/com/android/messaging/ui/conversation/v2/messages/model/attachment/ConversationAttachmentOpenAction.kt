package com.android.messaging.ui.conversation.v2.messages.model.attachment

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ConversationAttachmentOpenAction {

    @Immutable
    data class OpenContent(
        val contentType: String,
        val contentUri: String,
    ) : ConversationAttachmentOpenAction

    @Immutable
    data class OpenExternal(
        val uri: String,
    ) : ConversationAttachmentOpenAction
}
