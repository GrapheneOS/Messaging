package com.android.messaging.ui.conversationlist.mapper

import com.android.messaging.data.conversationlist.model.ConversationListDraft
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListLatestMessage
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationListNotification
import com.android.messaging.data.conversationlist.model.ConversationListParticipant
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationsettings.model.SNOOZE_NEVER_EXPIRES
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.domain.conversation.usecase.participant.CanAddContact
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListUiState
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationListUiStateMapperImplTest {

    private val mapper = ConversationListUiStateMapperImpl(
        canAddContact = mockk<CanAddContact>(relaxed = true),
        canPlacePhoneCall = mockk<CanPlacePhoneCall>(relaxed = true),
        canShowOrAddContact = mockk<CanShowOrAddContact>(relaxed = true),
        isContactSavedUseCase = mockk<IsContactSaved>(relaxed = true),
        resolveAvatarUri = mockk<ResolveAvatarUri>(relaxed = true),
    )

    @Test
    fun map_pinnedConversation_marksItemPinned() {
        val state = mapper.map(
            snapshot = snapshotOf(
                conversationItem(
                    conversationId = "pinned",
                    isPinned = true,
                ),
            ),
            selectedConversationIds = persistentListOf(),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        assertTrue(singleItem(state).isPinned)
    }

    @Test
    fun map_unpinnedConversation_marksItemNotPinned() {
        val state = mapper.map(
            snapshot = snapshotOf(
                conversationItem(
                    conversationId = "plain",
                    isPinned = false,
                ),
            ),
            selectedConversationIds = persistentListOf(),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        assertFalse(singleItem(state).isPinned)
    }

    @Test
    fun map_noSelection_leavesToggleStatesNull() {
        val state = mapper.map(
            snapshot = snapshotOf(conversationItem(conversationId = "a")),
            selectedConversationIds = persistentListOf(),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        val actions = state.selection.actions
        assertEquals(0, state.selection.selectedCount)
        assertNull(actions.firstSelectedIsPinned)
        assertNull(actions.firstSelectedIsSnoozed)
        assertNull(actions.firstSelectedIsUnread)
    }

    @Test
    fun map_singleSelectedPinnedSnoozedUnread_derivesToggleStatesFromSelection() {
        val state = mapper.map(
            snapshot = snapshotOf(
                conversationItem(
                    conversationId = "selected",
                    isPinned = true,
                    isSnoozed = true,
                    isRead = false,
                ),
            ),
            selectedConversationIds = persistentListOf("selected"),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        val actions = state.selection.actions
        assertEquals(true, actions.firstSelectedIsPinned)
        assertEquals(true, actions.firstSelectedIsSnoozed)
        assertEquals(true, actions.firstSelectedIsUnread)
    }

    @Test
    fun map_mixedSelection_togglesFollowFirstSelectedConversation() {
        val state = mapper.map(
            snapshot = snapshotOf(
                conversationItem(
                    conversationId = "first",
                    isPinned = false,
                    isSnoozed = false,
                ),
                conversationItem(
                    conversationId = "second",
                    isPinned = true,
                    isSnoozed = true,
                ),
            ),
            selectedConversationIds = persistentListOf("first", "second"),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        val actions = state.selection.actions
        assertEquals(false, actions.firstSelectedIsPinned)
        assertEquals(false, actions.firstSelectedIsSnoozed)
    }

    @Test
    fun map_selection_exposesSelectedCount() {
        val state = mapper.map(
            snapshot = snapshotOf(
                conversationItem(conversationId = "a"),
                conversationItem(conversationId = "b"),
            ),
            selectedConversationIds = persistentListOf("a", "b"),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        assertEquals(2, state.selection.selectedCount)
    }

    @Test
    fun map_senderName_preservesItForSnippetAccessibility() {
        val state = mapper.map(
            snapshot = snapshotOf(
                conversationItem(
                    conversationId = "group",
                    senderName = "Jane",
                ),
            ),
            selectedConversationIds = persistentListOf(),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        assertEquals("Jane", singleItem(state).snippet.senderName)
    }

    private fun singleItem(
        state: ConversationListUiState,
    ): ConversationListItemUiModel {
        return (state.content as ConversationListContentUiState.Items).items.single()
    }

    private fun snapshotOf(vararg items: ConversationListItem): ConversationListSnapshot {
        return ConversationListSnapshot(
            items = items.toList().toImmutableList(),
            blockedDestinations = persistentSetOf(),
            hasFirstSyncCompleted = true,
        )
    }

    private fun conversationItem(
        conversationId: String,
        isPinned: Boolean = false,
        isSnoozed: Boolean = false,
        isRead: Boolean = true,
        senderName: String? = null,
    ): ConversationListItem {
        return ConversationListItem(
            conversationId = conversationId,
            title = "Title $conversationId",
            icon = null,
            subject = null,
            isArchived = false,
            isPinned = isPinned,
            participant = ConversationListParticipant(
                contactId = -1L,
                lookupKey = null,
                otherNormalizedDestination = "+1555000$conversationId",
                isGroup = false,
                isEnterprise = false,
            ),
            latestMessage = ConversationListLatestMessage(
                isRead = isRead,
                timestamp = 1_000L,
                snippetText = "Snippet $conversationId",
                previewUri = null,
                previewContentType = null,
                status = ConversationListMessageStatus.Normal,
                isIncoming = true,
                senderName = senderName,
            ),
            draft = ConversationListDraft(
                isVisible = false,
                snippetText = null,
                previewUri = null,
                previewContentType = null,
                subject = null,
            ),
            notification = ConversationListNotification(
                isEnabled = true,
                snoozedUntilMillis = when {
                    isSnoozed -> SNOOZE_NEVER_EXPIRES
                    else -> ConversationListNotification.SNOOZE_NOT_SET
                },
            ),
        )
    }
}
