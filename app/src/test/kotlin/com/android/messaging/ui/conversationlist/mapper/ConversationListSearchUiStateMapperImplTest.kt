package com.android.messaging.ui.conversationlist.mapper

import android.content.Context
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationSearchMatch
import com.android.messaging.data.phone.formatter.PhoneNumberFormatter
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.model.ConversationListSearchContentUiState
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

internal class ConversationListSearchUiStateMapperImplTest {

    private val mapper = ConversationListSearchUiStateMapperImpl(
        itemUiMapper = ConversationListItemUiMapperImpl(
            context = mockk<Context>(relaxed = true),
            canPlacePhoneCall = mockk<CanPlacePhoneCall>(relaxed = true),
            canShowOrAddContact = mockk<CanShowOrAddContact>(relaxed = true),
            isContactSaved = mockk<IsContactSaved>(relaxed = true),
            phoneNumberFormatter = mockk<PhoneNumberFormatter>(relaxed = true),
            resolveAvatarUri = mockk<ResolveAvatarUri>(relaxed = true),
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
    fun map_blankQuery_isIdle() {
        val content = mapper.map(
            query = "  ",
            conversationMatches = persistentListOf(match(conversationId = INBOX_ID)),
        )

        assertEquals(ConversationListSearchContentUiState.Idle, content)
    }

    @Test
    fun map_queryWithoutMatches_hasNoResults() {
        val content = mapper.map(
            query = QUERY,
            conversationMatches = persistentListOf(),
        )

        assertEquals(ConversationListSearchContentUiState.NoResults, content)
    }

    @Test
    fun map_matches_keepTheirOrderHighlightsAndArchivedBadge() {
        val content = mapper.map(
            query = QUERY,
            conversationMatches = persistentListOf(
                match(conversationId = INBOX_ID),
                match(conversationId = ARCHIVED_ID, isArchived = true),
            ),
        )

        val conversations = (content as ConversationListSearchContentUiState.Results).conversations
        assertEquals(listOf(INBOX_ID, ARCHIVED_ID), conversations.map { it.conversationId })
        assertEquals(listOf(TITLE_MATCH), conversations.first().titleMatchRanges)
        assertEquals(listOf(false, true), conversations.map { it.isArchivedBadgeVisible })
        assertEquals(listOf(false, false), conversations.map { it.isSelected || it.isOpened })
    }

    private fun match(
        conversationId: ConversationId,
        isArchived: Boolean = false,
    ): ConversationSearchMatch {
        return ConversationSearchMatch(
            item = conversationItem(
                conversationId = conversationId,
                isArchived = isArchived,
            ),
            titleMatchRanges = persistentListOf(TITLE_MATCH),
        )
    }

    private companion object {
        private const val QUERY = "title"
        private val TITLE_MATCH = 0..4
        private val INBOX_ID = ConversationId("1")
        private val ARCHIVED_ID = ConversationId("2")
    }
}
