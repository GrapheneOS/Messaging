package com.android.messaging.ui.conversationlist.chats.model

import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.ui.conversationlist.model.ConversationListAvatarUiModel
import kotlinx.collections.immutable.ImmutableList

internal sealed interface ConversationListAction {

    sealed interface ConfirmationAction : ConversationListAction

    sealed interface LifecycleAction : ConversationListAction

    sealed interface ListAction : ConversationListAction

    sealed interface NavigationAction : ConversationListAction

    sealed interface SelectionAction : ConversationListAction

    sealed interface SnackbarAction : ConversationListAction

    // region ConfirmationAction
    data class BlockConfirmed(
        val conversationId: String,
        val destination: String,
    ) : ConfirmationAction

    data object DeleteConfirmed : ConfirmationAction
    // endregion

    // region SnackbarAction
    data class ArchiveUndoClicked(
        val conversationIds: ImmutableList<String>,
        val isArchived: Boolean,
    ) : SnackbarAction

    data class ArchiveSnackbarDismissed(
        val conversationIds: ImmutableList<String>,
    ) : SnackbarAction

    data class BlockUndoClicked(
        val conversationId: String,
        val destination: String,
    ) : SnackbarAction
    // endregion

    // region LifecycleAction
    data object ScreenResumed : LifecycleAction
    // endregion

    // region ListAction
    data class ConversationClicked(
        val conversationId: String,
    ) : ListAction

    data class ConversationLongClicked(
        val conversationId: String,
    ) : ListAction

    data class NewestConversationVisibilityChanged(
        val isVisible: Boolean,
    ) : ListAction

    data class AvatarMessageClicked(
        val conversationId: String,
    ) : ListAction

    data class AvatarCallClicked(
        val destination: String,
    ) : ListAction

    data class AvatarContactClicked(
        val avatar: ConversationListAvatarUiModel,
    ) : ListAction

    data class AvatarInfoClicked(
        val conversationId: String,
    ) : ListAction

    data class ConversationSwipedToArchive(
        val conversationId: String,
    ) : ListAction

    data class ConversationSwipedToToggleRead(
        val conversationId: String,
    ) : ListAction
    // endregion

    // region NavigationAction
    data object ArchivedConversationsClicked : NavigationAction
    data object BlockedParticipantsClicked : NavigationAction
    data object DebugOptionsClicked : NavigationAction
    data object ScrollToTopClicked : NavigationAction
    data object SettingsClicked : NavigationAction
    data object StartChatClicked : NavigationAction
    // endregion

    // region SelectionAction
    data object AddContactClicked : SelectionAction
    data object ArchiveClicked : SelectionAction
    data object BlockClicked : SelectionAction
    data object MarkReadClicked : SelectionAction
    data object MarkUnreadClicked : SelectionAction
    data object PinClicked : SelectionAction
    data object SelectionCleared : SelectionAction
    data object UnpinClicked : SelectionAction
    data object UnsnoozeClicked : SelectionAction

    data class SnoozeOptionSelected(
        val option: SnoozeOption,
    ) : SelectionAction

    data class PinAnimationPrepared(
        val conversationIds: ImmutableList<String>,
        val isPinned: Boolean,
    ) : SelectionAction
    // endregion
}
