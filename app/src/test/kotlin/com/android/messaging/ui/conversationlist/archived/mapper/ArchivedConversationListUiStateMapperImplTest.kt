package com.android.messaging.ui.conversationlist.archived.mapper

import android.content.Context
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.phone.formatter.PhoneNumberFormatter
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListUiState
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.mapper.ConversationListContentUiStateMapperImpl
import com.android.messaging.ui.conversationlist.mapper.ConversationListItemUiMapperImpl
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.snapshotOf
import com.android.messaging.util.OsUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class ArchivedConversationListUiStateMapperImplTest {

    private val itemUiMapper = ConversationListItemUiMapperImpl(
        context = mockk<Context>(relaxed = true),
        canPlacePhoneCall = mockk<CanPlacePhoneCall>(relaxed = true),
        canShowOrAddContact = mockk<CanShowOrAddContact>(relaxed = true),
        isContactSaved = mockk<IsContactSaved>(relaxed = true),
        phoneNumberFormatter = mockk<PhoneNumberFormatter>(relaxed = true),
        resolveAvatarUri = mockk<ResolveAvatarUri>(relaxed = true),
    )

    private val mapper = ArchivedConversationListUiStateMapperImpl(
        contentMapper = ConversationListContentUiStateMapperImpl(
            itemUiMapper = itemUiMapper,
        ),
    )

    @Before
    fun setUp() {
        mockkStatic(OsUtil::class)
        every { OsUtil.isSecondaryUser() } returns false
    }

    @After
    fun tearDown() {
        unmockkStatic(OsUtil::class)
    }

    @Test
    fun map_openedConversation_marksOnlyThatItemOpened() {
        val state = mapper.map(
            snapshot = archivedSnapshot(),
            selectedConversationIds = persistentListOf(),
            openedConversationId = SECOND_ID,
            isDebugEnabled = false,
        )

        assertEquals(listOf(SECOND_ID), openedIds(state))
    }

    @Test
    fun map_noOpenedConversation_marksNoItemOpened() {
        val state = mapper.map(
            snapshot = archivedSnapshot(),
            selectedConversationIds = persistentListOf(),
            openedConversationId = null,
            isDebugEnabled = false,
        )

        assertEquals(emptyList<ConversationId>(), openedIds(state))
    }

    @Test
    fun map_openedConversationNotArchived_marksNoItemOpened() {
        val state = mapper.map(
            snapshot = archivedSnapshot(),
            selectedConversationIds = persistentListOf(),
            openedConversationId = ConversationId("inbox"),
            isDebugEnabled = false,
        )

        assertEquals(emptyList<ConversationId>(), openedIds(state))
    }

    @Test
    fun map_openedAndSelectedConversations_keepsBothFlagsAndCountsSelection() {
        val state = mapper.map(
            snapshot = archivedSnapshot(),
            selectedConversationIds = persistentListOf(FIRST_ID),
            openedConversationId = SECOND_ID,
            isDebugEnabled = false,
        )

        val items = items(state)
        assertEquals(listOf(FIRST_ID), items.filter { it.isSelected }.map { it.conversationId })
        assertEquals(listOf(SECOND_ID), items.filter { it.isOpened }.map { it.conversationId })
        assertEquals(1, state.selectedCount)
    }

    private fun archivedSnapshot(): ConversationListSnapshot {
        return snapshotOf(
            conversationItem(conversationId = FIRST_ID, isArchived = true),
            conversationItem(conversationId = SECOND_ID, isArchived = true),
        )
    }

    private fun openedIds(state: ArchivedConversationListUiState): List<ConversationId> {
        return items(state)
            .filter(ConversationListItemUiModel::isOpened)
            .map(ConversationListItemUiModel::conversationId)
    }

    private fun items(state: ArchivedConversationListUiState): List<ConversationListItemUiModel> {
        return (state.content as ConversationListContentUiState.Items).items
    }

    private companion object {
        private val FIRST_ID = ConversationId("first")
        private val SECOND_ID = ConversationId("second")
    }
}
