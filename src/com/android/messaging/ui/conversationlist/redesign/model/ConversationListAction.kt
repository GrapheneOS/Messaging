package com.android.messaging.ui.conversationlist.redesign.model

import kotlinx.collections.immutable.ImmutableList

internal sealed interface ConversationListAction {

    sealed interface DialogAction : ConversationListAction

    sealed interface LifecycleAction : ConversationListAction

    sealed interface ListAction : ConversationListAction

    sealed interface NavigationAction : ConversationListAction

    sealed interface SelectionAction : ConversationListAction

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

    data class AddContactConfirmed(
        val destination: String,
    ) : DialogAction

    data class ArchiveUndoClicked(
        val conversationIds: ImmutableList<String>,
        val isArchived: Boolean,
    ) : DialogAction

    data class BlockUndoClicked(
        val conversationId: String,
        val destination: String,
    ) : DialogAction

    data object AddContactClicked : SelectionAction
    data object ArchiveClicked : SelectionAction
    data object BlockClicked : SelectionAction
    data object SelectionCleared : SelectionAction
    data object UnarchiveClicked : SelectionAction

    data object ArchivedConversationsClicked : NavigationAction
    data object BlockedParticipantsClicked : NavigationAction
    data object DebugOptionsClicked : NavigationAction
    data object ScrollUpClicked : NavigationAction
    data object SettingsClicked : NavigationAction
    data object StartChatClicked : NavigationAction

    data object BlockConfirmed : DialogAction
    data object DeleteConfirmed : DialogAction

    data object ScreenResumed : LifecycleAction
}
