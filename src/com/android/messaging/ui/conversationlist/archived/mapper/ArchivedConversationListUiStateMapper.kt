package com.android.messaging.ui.conversationlist.archived.mapper

import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListUiState as State
import com.android.messaging.ui.conversationlist.mapper.ConversationListItemUiMapper
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ArchivedConversationListUiStateMapper {
    fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableList<String>,
        isDebugEnabled: Boolean,
    ): State
}

internal class ArchivedConversationListUiStateMapperImpl @Inject constructor(
    private val itemUiMapper: ConversationListItemUiMapper,
) : ArchivedConversationListUiStateMapper {

    override fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableList<String>,
        isDebugEnabled: Boolean,
    ): State {
        val items = snapshot.items
            .map { item ->
                itemUiMapper.map(
                    item = item,
                    isSelected = item.conversationId in selectedConversationIds,
                )
            }
            .toImmutableList()

        val content = when {
            items.isNotEmpty() -> ConversationListContentUiState.Items(
                items = items,
                restoredConversationIds = snapshot.restoredConversationIds,
            )

            else -> ConversationListContentUiState.Empty
        }

        return State(
            content = content,
            selectedCount = selectedConversationIds.size,
            isDebugEnabled = isDebugEnabled,
        )
    }
}
