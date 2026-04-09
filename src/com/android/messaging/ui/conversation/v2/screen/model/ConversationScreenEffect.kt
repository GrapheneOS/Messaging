package com.android.messaging.ui.conversation.v2.screen.model

internal sealed interface ConversationScreenEffect {

    data class OpenAttachmentPreview(
        val contentType: String,
        val contentUri: String,
        val imageCollectionUri: String?,
    ) : ConversationScreenEffect

    data class OpenExternalUri(
        val uri: String,
    ) : ConversationScreenEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : ConversationScreenEffect
}
