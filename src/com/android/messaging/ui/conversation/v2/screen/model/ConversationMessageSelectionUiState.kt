package com.android.messaging.ui.conversation.v2.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal data class ConversationMessageSelectionUiState(
    val selectedMessageIds: ImmutableSet<String> = persistentSetOf(),
    val availableActions: ImmutableSet<ConversationMessageSelectionAction> = persistentSetOf(),
    val deleteConfirmation: ConversationMessageDeleteConfirmationUiState? = null,
) {
    val isSelectionMode: Boolean
        get() = selectedMessageIds.isNotEmpty()

    val isMultiSelect: Boolean
        get() = selectedMessageIds.size > 1

    val selectedMessageCount: Int
        get() = selectedMessageIds.size
}

@Immutable
internal data class ConversationMessageDeleteConfirmationUiState(
    val messageIds: ImmutableSet<String> = persistentSetOf(),
)

internal enum class ConversationMessageSelectionAction {
    Copy,
    Delete,
    Details,
    Download,
    Forward,
    Resend,
    Share,
}
