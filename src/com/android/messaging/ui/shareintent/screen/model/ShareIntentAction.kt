package com.android.messaging.ui.shareintent.screen.model

import com.android.messaging.data.conversation.model.draft.ConversationDraft

internal sealed interface ShareIntentAction {

    data class DraftResolved(
        val draft: ConversationDraft?,
    ) : ShareIntentAction

    data class DraftTextChanged(
        val text: String,
    ) : ShareIntentAction

    data class DraftAttachmentRemoved(
        val id: String,
    ) : ShareIntentAction

    data object ReviewDismissed : ShareIntentAction

    data object ConfirmSendClicked : ShareIntentAction

    data class TargetClicked(
        val conversationId: String,
    ) : ShareIntentAction

    data class TargetLongPressed(
        val conversationId: String,
    ) : ShareIntentAction

    data class SelectionToggled(
        val conversationId: String,
    ) : ShareIntentAction

    data object SelectionCleared : ShareIntentAction

    data object SendToSelectedClicked : ShareIntentAction

    data object NewMessageClicked : ShareIntentAction

    data object SearchOpened : ShareIntentAction

    data object SearchClosed : ShareIntentAction

    data class SearchQueryChanged(
        val query: String,
    ) : ShareIntentAction
}
