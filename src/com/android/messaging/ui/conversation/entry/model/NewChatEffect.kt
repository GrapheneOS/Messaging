package com.android.messaging.ui.conversation.entry.model

internal sealed interface NewChatEffect {

    data class ShowMessage(
        val messageResId: Int,
    ) : NewChatEffect
}
