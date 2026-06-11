package com.android.messaging.ui.conversationpicker.model

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerListItem

internal sealed interface ConversationPickerAction {

    sealed interface TargetsAction : ConversationPickerAction

    sealed interface DraftAction : ConversationPickerAction

    data class TargetClicked(
        val target: TargetUiState,
    ) : TargetsAction

    data class SelectionToggled(
        val target: TargetUiState,
    ) : TargetsAction

    data class ContactDestinationClicked(
        val item: RecipientPickerListItem,
        val destination: String,
    ) : TargetsAction

    data class ContactDestinationToggled(
        val item: RecipientPickerListItem,
        val destination: String,
    ) : TargetsAction

    data object SelectionCleared : TargetsAction

    data object ProceedToReviewClicked : TargetsAction

    data object SearchOpened : TargetsAction

    data object SearchClosed : TargetsAction

    data class SearchQueryChanged(
        val query: String,
    ) : TargetsAction

    data object LoadMoreContacts : TargetsAction

    data object LoadMoreRecent : TargetsAction

    data object CollapseRecent : TargetsAction

    data object ContactsPermissionGranted : TargetsAction

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

    data object SendClicked : DraftAction
}
