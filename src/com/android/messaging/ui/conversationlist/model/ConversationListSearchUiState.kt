package com.android.messaging.ui.conversationlist.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.MessageId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
        val messages: ImmutableList<ConversationListSearchMessageUiModel> = persistentListOf(),
    ) : ConversationListSearchContentUiState
}

/**
 * A message found by its text, shown as its conversation's row with the message in place of the
 * latest one.
 */
@Immutable
internal data class ConversationListSearchMessageUiModel(
    val messageId: MessageId,
    val item: ConversationListItemUiModel,
)
