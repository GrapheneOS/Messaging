package com.android.messaging.testutil

import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftEditorDelegate

internal fun ConversationDraftEditorDelegate.typeMessageText(messageText: String) {
    onMessageTextChanged(
        messageText = messageText,
        messageTextRevision = state.value.messageTextRevision,
    )
}

/** Edits the message text the way the message field does, typed over the text the draft has now. */
internal fun ConversationDraftDelegate.typeMessageText(messageText: String) {
    onMessageTextChanged(
        messageText = messageText,
        messageTextRevision = state.value.messageTextRevision,
    )
}
