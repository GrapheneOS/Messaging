package com.android.messaging.ui.conversationlist.mapper

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationSearchMatch
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListSearchContentUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationListSearchUiStateMapper {
    fun map(
        query: String,
        conversationMatches: ImmutableList<ConversationSearchMatch>,
        openedConversationId: ConversationId?,
    ): ConversationListSearchContentUiState
}

internal class ConversationListSearchUiStateMapperImpl @Inject constructor(
    private val itemUiMapper: ConversationListItemUiMapper,
) : ConversationListSearchUiStateMapper {

    override fun map(
        query: String,
        conversationMatches: ImmutableList<ConversationSearchMatch>,
        openedConversationId: ConversationId?,
    ): ConversationListSearchContentUiState {
        return when {
            query.isBlank() -> ConversationListSearchContentUiState.Idle

            conversationMatches.isEmpty() -> ConversationListSearchContentUiState.NoResults

            else -> {
                ConversationListSearchContentUiState.Results(
                    conversations = conversationMatches
                        .map { match ->
                            mapConversation(
                                match = match,
                                isOpened = match.item.conversationId == openedConversationId,
                            )
                        }
                        .toImmutableList(),
                )
            }
        }
    }

    private fun mapConversation(
        match: ConversationSearchMatch,
        isOpened: Boolean,
    ): ConversationListItemUiModel {
        return itemUiMapper
            .map(
                item = match.item,
                isSelected = false,
                isOpened = isOpened,
            )
            .copy(
                titleMatchRanges = match.titleMatchRanges,
                isArchivedBadgeVisible = match.item.isArchived,
            )
    }
}
