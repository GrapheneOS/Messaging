package com.android.messaging.ui.conversationlist.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ConversationListSearchUiState(
    val isActive: Boolean = false,
    val content: ConversationListSearchContentUiState = ConversationListSearchContentUiState.Idle,
)

@Immutable
internal sealed interface ConversationListSearchContentUiState {

    @Immutable
    data object Idle : ConversationListSearchContentUiState

    @Immutable
    data object NoResults : ConversationListSearchContentUiState

    @Immutable
    data class Results(
        val conversations: ImmutableList<ConversationListItemUiModel>,
    ) : ConversationListSearchContentUiState
}
