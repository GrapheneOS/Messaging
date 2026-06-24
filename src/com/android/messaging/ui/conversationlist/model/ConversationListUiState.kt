package com.android.messaging.ui.conversationlist.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ConversationListUiState(
    val content: ConversationListContentUiState = ConversationListContentUiState.Loading,
    val selection: ConversationListSelectionUiState = ConversationListSelectionUiState(),
    val isScrollToTopVisible: Boolean = false,
    val hasBlockedParticipants: Boolean = false,
    val isDebugEnabled: Boolean = false,
)

@Immutable
internal sealed interface ConversationListContentUiState {

    @Immutable
    data object Loading : ConversationListContentUiState

    @Immutable
    data object WaitingForSync : ConversationListContentUiState

    @Immutable
    data object Empty : ConversationListContentUiState

    @Immutable
    data class Items(
        val items: ImmutableList<ConversationListItemUiModel>,
    ) : ConversationListContentUiState
}
