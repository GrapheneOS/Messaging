package com.android.messaging.ui.conversation.messages.model.message

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.MessageId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed interface ConversationMessagesUiState {

    @Immutable
    data object Loading : ConversationMessagesUiState

    /**
     * [missingMessageId] is a message the screen asked to scroll to that is no longer in the
     * conversation, so the screen stops waiting for the window to bring it in.
     *
     * [windowId] changes when [messages] move to another part of the conversation instead of
     * growing, so the list starts over rather than animating every message out and in.
     *
     * [hasNewerMessages] is set while [messages] sit around an older message and the conversation
     * goes on past them. [latestUnloadedMessage] is then its newest message, which changes when a
     * message arrives.
     */
    @Immutable
    data class Present(
        val messages: ImmutableList<ConversationMessageUiModel> = persistentListOf(),
        val youTubeLinkPreviewsEnabled: Boolean = false,
        val highlightedMessageId: MessageId? = null,
        val missingMessageId: MessageId? = null,
        val windowId: Int = 0,
        val hasNewerMessages: Boolean = false,
        val latestUnloadedMessage: ConversationMessageUiModel? = null,
    ) : ConversationMessagesUiState
}
