package com.android.messaging.ui.conversationlist.mapper

import android.content.Context
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.snapshotOf
import com.android.messaging.util.OsUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class ConversationListContentUiStateMapperImplTest {

    @Before
    fun setUp() {
        mockkStatic(OsUtil::class)
        every { OsUtil.isSecondaryUser() } returns false
    }

    @After
    fun tearDown() {
        unmockkStatic(OsUtil::class)
    }

    private val itemUiMapper = ConversationListItemUiMapperImpl(
        context = mockk<Context>(relaxed = true),
        canPlacePhoneCall = mockk<CanPlacePhoneCall>(relaxed = true),
        canShowOrAddContact = mockk<CanShowOrAddContact>(relaxed = true),
        isContactSaved = mockk<IsContactSaved>(relaxed = true),
        resolveAvatarUri = mockk<ResolveAvatarUri>(relaxed = true),
    )

    private val mapper = ConversationListContentUiStateMapperImpl(
        itemUiMapper = itemUiMapper,
    )

    @Test
    fun map_itemsPresent_producesContentItems() {
        val content = mapper.map(
            snapshot = snapshotOf(conversationItem(conversationId = "a")),
            selectedConversationIds = persistentListOf(),
        )

        assertTrue(content is ConversationListContentUiState.Items)
    }

    @Test
    fun map_emptyAfterFirstSync_producesEmptyContent() {
        val content = mapper.map(
            snapshot = snapshotOf(),
            selectedConversationIds = persistentListOf(),
        )

        assertEquals(ConversationListContentUiState.Empty, content)
    }

    @Test
    fun map_emptyBeforeFirstSync_producesWaitingForSync() {
        val content = mapper.map(
            snapshot = ConversationListSnapshot(
                items = persistentListOf(),
                blockedDestinations = persistentSetOf(),
                hasFirstSyncCompleted = false,
            ),
            selectedConversationIds = persistentListOf(),
        )

        assertEquals(ConversationListContentUiState.WaitingForSync, content)
    }

    @Test
    fun map_itemsPresentBeforeFirstSync_stillProducesItems() {
        val content = mapper.map(
            snapshot = ConversationListSnapshot(
                items = persistentListOf(conversationItem(conversationId = "a")),
                blockedDestinations = persistentSetOf(),
                hasFirstSyncCompleted = false,
            ),
            selectedConversationIds = persistentListOf(),
        )

        assertTrue(content is ConversationListContentUiState.Items)
    }

    @Test
    fun map_selectedConversation_marksItemSelected() {
        val content = mapper.map(
            snapshot = snapshotOf(conversationItem(conversationId = "a")),
            selectedConversationIds = persistentListOf("a"),
        )

        val items = (content as ConversationListContentUiState.Items).items
        assertTrue(items.single().isSelected)
    }
}
