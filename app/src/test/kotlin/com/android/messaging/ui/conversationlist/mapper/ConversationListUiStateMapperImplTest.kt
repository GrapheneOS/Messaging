package com.android.messaging.ui.conversationlist.mapper

import android.content.Context
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.domain.conversation.usecase.participant.CanAddContact
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListUiState
import com.android.messaging.ui.conversationlist.snapshotOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationListUiStateMapperImplTest {

    private val canAddContact = mockk<CanAddContact>(relaxed = true)
    private val canPlacePhoneCall = mockk<CanPlacePhoneCall>(relaxed = true)
    private val canShowOrAddContact = mockk<CanShowOrAddContact>(relaxed = true)
    private val isContactSaved = mockk<IsContactSaved>(relaxed = true)
    private val resolveAvatarUri = mockk<ResolveAvatarUri>(relaxed = true)

    private val itemUiMapper = ConversationListItemUiMapperImpl(
        context = mockk<Context>(relaxed = true),
        canPlacePhoneCall = canPlacePhoneCall,
        canShowOrAddContact = canShowOrAddContact,
        isContactSaved = isContactSaved,
        resolveAvatarUri = resolveAvatarUri,
    )

    private val mapper = ConversationListUiStateMapperImpl(
        canAddContact = canAddContact,
        itemUiMapper = itemUiMapper,
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
        assertTrue(requireNotNull(actions.firstSelectedIsPinned))
        assertTrue(requireNotNull(actions.firstSelectedIsSnoozed))
        assertTrue(requireNotNull(actions.firstSelectedIsUnread))
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
        assertFalse(requireNotNull(actions.firstSelectedIsPinned))
        assertFalse(requireNotNull(actions.firstSelectedIsSnoozed))
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

    @Test
    fun map_itemsPresent_producesContentItems() {
        val state = mapper.map(
            snapshot = snapshotOf(conversationItem(conversationId = "a")),
            selectedConversationIds = persistentListOf(),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        assertTrue(state.content is ConversationListContentUiState.Items)
    }

    @Test
    fun map_emptyAfterFirstSync_producesEmptyContent() {
        val state = mapper.map(
            snapshot = snapshotOf(),
            selectedConversationIds = persistentListOf(),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        assertEquals(ConversationListContentUiState.Empty, state.content)
    }

    @Test
    fun map_emptyBeforeFirstSync_producesWaitingForSync() {
        val state = mapper.map(
            snapshot = ConversationListSnapshot(
                items = persistentListOf(),
                blockedDestinations = persistentSetOf(),
                hasFirstSyncCompleted = false,
            ),
            selectedConversationIds = persistentListOf(),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        assertEquals(ConversationListContentUiState.WaitingForSync, state.content)
    }

    @Test
    fun map_visibleDraft_takesPrecedenceOverLatestMessage() {
        val state = mapper.map(
            snapshot = snapshotOf(
                conversationItem(
                    conversationId = "a",
                    isDraftVisible = true,
                    draftSnippet = "Draft body",
                    draftSubject = "Draft subject",
                ),
            ),
            selectedConversationIds = persistentListOf(),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        val item = singleItem(state)
        assertTrue(item.snippet.isDraft)
        assertEquals("Draft body", item.snippet.text)
        assertEquals("Draft subject", item.subject)
        assertEquals(ConversationListMessageStatus.Draft, item.status)
        assertTrue(item.isOutgoing)
    }

    @Test
    fun map_incomingMmsDownloadStatus_preservesDownloadStatus() {
        val state = mapper.map(
            snapshot = snapshotOf(
                conversationItem(
                    conversationId = "mms",
                    status = ConversationListMessageStatus.IncomingAwaitingManualDownload,
                ),
            ),
            selectedConversationIds = persistentListOf(),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        val item = singleItem(state)
        assertEquals(ConversationListMessageStatus.IncomingAwaitingManualDownload, item.status)
        assertFalse(item.isOutgoing)
    }

    @Test
    fun map_callableSavedContact_populatesAvatarCapabilities() {
        every { canPlacePhoneCall(any()) } returns true
        every { canShowOrAddContact(any(), any(), any(), any()) } returns true
        every { isContactSaved(any(), any()) } returns true

        val state = mapper.map(
            snapshot = snapshotOf(
                conversationItem(
                    conversationId = "a",
                    contactId = 42L,
                    lookupKey = "lookup",
                ),
            ),
            selectedConversationIds = persistentListOf(),
            isScrollToTopVisible = false,
            isDebugEnabled = false,
        )

        val avatar = singleItem(state).avatar
        assertTrue(avatar.canCall)
        assertTrue(avatar.canShowContact)
        assertTrue(avatar.isContactSaved)
        assertEquals("+1555000a", avatar.normalizedDestination)
    }

    @Test
    fun map_selectedBlockedConversation_propagatesScreenState() {
        val state = mapper.map(
            snapshot = ConversationListSnapshot(
                items = persistentListOf(conversationItem(conversationId = "a")),
                blockedDestinations = persistentSetOf("+1555000a"),
                hasFirstSyncCompleted = true,
            ),
            selectedConversationIds = persistentListOf("a"),
            isScrollToTopVisible = true,
            isDebugEnabled = true,
        )

        assertTrue(singleItem(state).isSelected)
        assertTrue(state.isScrollToTopVisible)
        assertTrue(state.hasBlockedParticipants)
        assertTrue(state.isDebugEnabled)
        assertFalse(state.selection.actions.canBlock)
    }

    private fun singleItem(
        state: ConversationListUiState,
    ): ConversationListItemUiModel {
        return (state.content as ConversationListContentUiState.Items).items.single()
    }
}
