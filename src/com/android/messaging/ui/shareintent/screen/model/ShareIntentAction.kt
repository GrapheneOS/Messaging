package com.android.messaging.ui.shareintent.screen.model

import com.android.messaging.data.conversation.model.draft.ConversationDraft

internal sealed interface ShareIntentAction {

    sealed interface TargetsAction : ShareIntentAction

    sealed interface DraftAction : ShareIntentAction

    data class TargetClicked(
        val conversationId: String,
    ) : TargetsAction

    data class TargetLongPressed(
        val conversationId: String,
    ) : TargetsAction

    data class SelectionToggled(
        val conversationId: String,
    ) : TargetsAction

    data object SelectionCleared : TargetsAction

    data object SendToSelectedClicked : TargetsAction

    data object NewMessageClicked : TargetsAction

    data object SearchOpened : TargetsAction

    data object SearchClosed : TargetsAction

    data class SearchQueryChanged(
        val query: String,
    ) : TargetsAction

    data class DraftResolved(
        val draft: ConversationDraft?,
    ) : DraftAction

    data class DraftTextChanged(
        val text: String,
    ) : DraftAction

    data class DraftAttachmentRemoved(
        val id: String,
    ) : DraftAction

    data object DraftSubjectCleared : DraftAction

    data object ReviewDismissed : DraftAction

    data object ConfirmSendClicked : DraftAction
}
