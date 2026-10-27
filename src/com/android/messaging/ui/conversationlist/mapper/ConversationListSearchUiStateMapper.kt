package com.android.messaging.ui.conversationlist.mapper

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
    ): ConversationListSearchContentUiState
}

internal class ConversationListSearchUiStateMapperImpl @Inject constructor(
    private val itemUiMapper: ConversationListItemUiMapper,
) : ConversationListSearchUiStateMapper {

    override fun map(
        query: String,
        conversationMatches: ImmutableList<ConversationSearchMatch>,
    ): ConversationListSearchContentUiState {
        return when {
            query.isBlank() -> ConversationListSearchContentUiState.Idle

            conversationMatches.isEmpty() -> ConversationListSearchContentUiState.NoResults

            else -> {
                ConversationListSearchContentUiState.Results(
                    conversations = conversationMatches
                        .map(::mapConversation)
                        .toImmutableList(),
                )
            }
        }
    }

    private fun mapConversation(match: ConversationSearchMatch): ConversationListItemUiModel {
        return itemUiMapper
            .map(
                item = match.item,
                isSelected = false,
                isOpened = false,
            )
            .copy(
                titleMatchRanges = match.titleMatchRanges,
                isArchivedBadgeVisible = match.item.isArchived,
            )
    }
}
