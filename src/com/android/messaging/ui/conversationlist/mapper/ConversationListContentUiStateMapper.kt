package com.android.messaging.ui.conversationlist.mapper

import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationListContentUiStateMapper {
    fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableList<String>,
    ): ConversationListContentUiState
}

internal class ConversationListContentUiStateMapperImpl @Inject constructor(
    private val itemUiMapper: ConversationListItemUiMapper,
) : ConversationListContentUiStateMapper {

    override fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableList<String>,
    ): ConversationListContentUiState {
        val items = snapshot.items
            .map { item ->
                itemUiMapper.map(
                    item = item,
                    isSelected = item.conversationId in selectedConversationIds,
                )
            }
            .toImmutableList()

        return when {
            items.isNotEmpty() -> {
                ConversationListContentUiState.Items(
                    items = items,
                    restoredConversationIds = snapshot.restoredConversationIds,
                )
            }

            !snapshot.hasFirstSyncCompleted -> {
                ConversationListContentUiState.WaitingForSync
            }

            else -> {
                ConversationListContentUiState.Empty
            }
        }
    }
}
