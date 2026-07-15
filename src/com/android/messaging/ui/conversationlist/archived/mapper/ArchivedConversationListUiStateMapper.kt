package com.android.messaging.ui.conversationlist.archived.mapper

import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListUiState as State
import com.android.messaging.ui.conversationlist.mapper.ConversationListContentUiStateMapper
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList

internal interface ArchivedConversationListUiStateMapper {
    fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableList<String>,
        isDebugEnabled: Boolean,
    ): State
}

internal class ArchivedConversationListUiStateMapperImpl @Inject constructor(
    private val contentMapper: ConversationListContentUiStateMapper,
) : ArchivedConversationListUiStateMapper {

    override fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableList<String>,
        isDebugEnabled: Boolean,
    ): State {
        val content = contentMapper.map(
            snapshot = snapshot,
            selectedConversationIds = selectedConversationIds,
        )

        return State(
            content = content,
            selectedCount = selectedConversationIds.size,
            isDebugEnabled = isDebugEnabled,
        )
    }
}
