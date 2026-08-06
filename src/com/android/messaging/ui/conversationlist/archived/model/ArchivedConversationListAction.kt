package com.android.messaging.ui.conversationlist.archived.model

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversationlist.model.ConversationListAvatarUiModel

internal sealed interface ArchivedConversationListAction {

    sealed interface ConfirmationAction : ArchivedConversationListAction

    sealed interface ListAction : ArchivedConversationListAction

    sealed interface NavigationAction : ArchivedConversationListAction

    sealed interface SelectionAction : ArchivedConversationListAction

    sealed interface SnackbarAction : ArchivedConversationListAction

    // region ConfirmationAction
    data object DeleteSelectedConfirmed : ConfirmationAction
    // endregion

    // region ListAction
    data class ConversationClicked(
        val conversationId: ConversationId,
    ) : ListAction

    data class ConversationLongClicked(
        val conversationId: ConversationId,
    ) : ListAction

    data class ConversationSwipedToUnarchive(
        val conversationId: ConversationId,
    ) : ListAction

    data class AvatarMessageClicked(
        val conversationId: ConversationId,
    ) : ListAction

    data class AvatarCallClicked(
        val destination: String,
    ) : ListAction

    data class AvatarContactClicked(
        val avatar: ConversationListAvatarUiModel,
    ) : ListAction

    data class AvatarInfoClicked(
        val conversationId: ConversationId,
    ) : ListAction
    // endregion

    // region NavigationAction
    data object DebugOptionsClicked : NavigationAction
    // endregion

    // region SelectionAction
    data object UnarchiveSelectedClicked : SelectionAction
    data object SelectionCleared : SelectionAction
    // endregion

    // region SnackbarAction
    data class UnarchiveUndoClicked(
        val conversationIds: List<ConversationId>,
    ) : SnackbarAction

    data class UnarchiveSnackbarDismissed(
        val conversationIds: List<ConversationId>,
    ) : SnackbarAction
    // endregion
}
