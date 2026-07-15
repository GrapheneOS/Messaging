package com.android.messaging.ui.conversationlist.chats.mapper

import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.domain.conversation.usecase.participant.CanAddContact
import com.android.messaging.ui.conversationlist.chats.model.ConversationListSelectionUiState
import com.android.messaging.ui.conversationlist.chats.model.ConversationListUiState
import com.android.messaging.ui.conversationlist.chats.model.SelectionActionsUiState
import com.android.messaging.ui.conversationlist.mapper.ConversationListContentUiStateMapper
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

internal interface ConversationListUiStateMapper {
    fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableList<String>,
        isScrollToTopVisible: Boolean,
        isDebugEnabled: Boolean,
    ): ConversationListUiState
}

internal class ConversationListUiStateMapperImpl @Inject constructor(
    private val canAddContact: CanAddContact,
    private val contentMapper: ConversationListContentUiStateMapper,
) : ConversationListUiStateMapper {

    override fun map(
        snapshot: ConversationListSnapshot,
        selectedConversationIds: ImmutableList<String>,
        isScrollToTopVisible: Boolean,
        isDebugEnabled: Boolean,
    ): ConversationListUiState {
        val content = contentMapper.map(
            snapshot = snapshot,
            selectedConversationIds = selectedConversationIds,
        )

        val selection = mapSelectionState(
            items = snapshot.items,
            selectedConversationIds = selectedConversationIds,
            blockedDestinations = snapshot.blockedDestinations,
        )

        return ConversationListUiState(
            content = content,
            selection = selection,
            isScrollToTopVisible = isScrollToTopVisible &&
                content is ConversationListContentUiState.Items,
            hasBlockedParticipants = snapshot.blockedDestinations.isNotEmpty(),
            isDebugEnabled = isDebugEnabled,
        )
    }

    private fun mapSelectionState(
        items: List<ConversationListItem>,
        selectedConversationIds: ImmutableList<String>,
        blockedDestinations: ImmutableSet<String>,
    ): ConversationListSelectionUiState {
        val itemsById = items.associateBy(ConversationListItem::conversationId)
        val selectedItems = selectedConversationIds
            .mapNotNull { conversationId ->
                itemsById[conversationId]
            }

        return ConversationListSelectionUiState(
            selectedCount = selectedItems.size,
            actions = mapSelectionActions(
                selectedItems = selectedItems,
                blockedDestinations = blockedDestinations,
            ),
        )
    }

    private fun mapSelectionActions(
        selectedItems: List<ConversationListItem>,
        blockedDestinations: ImmutableSet<String>,
    ): SelectionActionsUiState {
        val singleSelection = selectedItems.singleOrNull()
        val firstSelected = selectedItems.firstOrNull()
        val canAddSelectedContact = singleSelection?.participant?.let { participant ->
            canAddContact(
                isGroup = participant.isGroup,
                lookupKey = participant.lookupKey,
                destination = participant.otherNormalizedDestination,
            )
        }
        val canBlockSelected = singleSelection?.let { item ->
            canBlock(
                destination = item.participant.otherNormalizedDestination,
                blockedDestinations = blockedDestinations,
            )
        }

        return SelectionActionsUiState(
            canAddContact = canAddSelectedContact == true,
            canBlock = canBlockSelected == true,
            firstSelectedIsPinned = firstSelected?.isPinned,
            firstSelectedIsSnoozed = firstSelected?.notification?.isSnoozed,
            firstSelectedIsUnread = firstSelected?.latestMessage?.isRead?.not(),
        )
    }

    private fun canBlock(
        destination: String?,
        blockedDestinations: ImmutableSet<String>,
    ): Boolean {
        val resolvedDestination = destination?.takeIf(String::isNotBlank) ?: return false

        return resolvedDestination !in blockedDestinations
    }
}
