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
            isScrollUpVisible = false,
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
            isScrollUpVisible = false,
            isDebugEnabled = false,
        )

        assertFalse(singleItem(state).isPinned)
    }

    @Test
    fun map_noSelection_leavesToggleStatesNull() {
        val state = mapper.map(
            snapshot = snapshotOf(conversationItem(conversationId = "a")),
            selectedConversationIds = persistentListOf(),
            isScrollUpVisible = false,
            isDebugEnabled = false,
        )

        val actions = state.selection.actions
        assertFalse(state.selection.isActive)
        assertNull(actions.isFirstSelectedPinned)
        assertNull(actions.isFirstSelectedSnoozed)
        assertNull(actions.isFirstSelectedUnread)
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
            isScrollUpVisible = false,
            isDebugEnabled = false,
        )

        val actions = state.selection.actions
        assertEquals(true, actions.isFirstSelectedPinned)
        assertEquals(true, actions.isFirstSelectedSnoozed)
        assertEquals(true, actions.isFirstSelectedUnread)
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
            isScrollUpVisible = false,
            isDebugEnabled = false,
        )

        val actions = state.selection.actions
        assertEquals(false, actions.isFirstSelectedPinned)
        assertEquals(false, actions.isFirstSelectedSnoozed)
    }

    @Test
    fun map_selection_canArchiveBecauseInboxItemsAreNotArchived() {
        val state = mapper.map(
            snapshot = snapshotOf(conversationItem(conversationId = "a")),
            selectedConversationIds = persistentListOf("a"),
            isScrollUpVisible = false,
            isDebugEnabled = false,
        )

        assertTrue(state.selection.actions.canArchive)
        assertTrue(state.selection.actions.canDelete)
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
                senderName = null,
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
