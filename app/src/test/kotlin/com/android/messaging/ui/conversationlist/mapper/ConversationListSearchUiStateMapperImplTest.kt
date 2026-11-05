package com.android.messaging.ui.conversationlist.mapper

import android.content.Context
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationSearchMatch
import com.android.messaging.data.conversationlist.model.MessageSearchMatch
import com.android.messaging.data.conversationlist.model.SearchableMessage
import com.android.messaging.data.phone.formatter.PhoneNumberFormatter
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.model.ConversationListSearchContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListSearchMessageUiModel
import com.android.messaging.util.OsUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.ImmutableList
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
            messageMatches = persistentListOf(messageMatch(text = "title")),
        )

        assertEquals(ConversationListSearchContentUiState.Idle, content)
    }

    @Test
    fun map_queryWithoutMatches_hasNoResults() {
        val content = mapper.map(
            query = QUERY,
            conversationMatches = persistentListOf(),
            messageMatches = persistentListOf(),
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
            messageMatches = persistentListOf(),
        )

        val conversations = (content as ConversationListSearchContentUiState.Results).conversations
        assertEquals(listOf(INBOX_ID, ARCHIVED_ID), conversations.map { it.conversationId })
        assertEquals(listOf(TITLE_MATCH), conversations.first().titleMatchRanges)
        assertEquals(listOf(false, true), conversations.map { it.isArchivedBadgeVisible })
        assertEquals(listOf(false, false), conversations.map { it.isSelected || it.isOpened })
    }

    @Test
    fun map_onlyMessagesMatch_showsThemInTheirConversationsRows() {
        val content = mapper.map(
            query = QUERY,
            conversationMatches = persistentListOf(),
            messageMatches = persistentListOf(
                messageMatch(
                    text = "The title is final",
                    matchRanges = persistentListOf(4..8),
                    isArchived = true,
                ),
            ),
        )

        val results = content as ConversationListSearchContentUiState.Results
        val message = results.messages.single()
        assertEquals(emptyList<Any>(), results.conversations)
        assertEquals(MESSAGE_ID, message.messageId)
        assertEquals(INBOX_ID, message.item.conversationId)
        assertEquals("The title is final", message.item.snippet.text)
        assertEquals(SENDER_NAME, message.item.snippet.senderName)
        assertEquals(listOf(4..8), message.item.snippetMatchRanges)
        assertEquals(MESSAGE_TIMESTAMP, message.item.timestampMillis)
        assertEquals(ConversationListMessageStatus.Normal, message.item.status)
        assertEquals(false, message.item.isUnread)
        assertEquals(false, message.item.isOutgoing)
        assertEquals(true, message.item.isArchivedBadgeVisible)
        assertEquals(emptyList<IntRange>(), message.item.titleMatchRanges)
    }

    @Test
    fun map_messageMatchFarIntoTheText_startsTheSnippetAtAWordShortlyBeforeIt() {
        val text = "Some long preamble that goes on and on\nuntil the title shows up"
        val matchStart = text.indexOf("title")

        val message = mapMessage(
            text = text,
            matchRanges = persistentListOf(matchStart..matchStart + 4),
        )

        assertEquals("…on until the title shows up", message.item.snippet.text)
        assertEquals(
            "title",
            message.item.snippetMatchRanges.single().let { range ->
                message.item.snippet.text.orEmpty().substring(range)
            },
        )
    }

    @Test
    fun map_messageMatchFarIntoTextWithoutSpaces_cutsTheSnippetMidWord() {
        val text = "a".repeat(40) + "title"

        val message = mapMessage(
            text = text,
            matchRanges = persistentListOf(40..44),
        )

        assertEquals("…" + "a".repeat(16) + "title", message.item.snippet.text)
        assertEquals(listOf(17..21), message.item.snippetMatchRanges)
    }

    @Test
    fun map_messageMatchNearTheStart_keepsTheWholeTextOnOneLine() {
        val message = mapMessage(
            text = "Title\r\nnext line",
            matchRanges = persistentListOf(0..4),
        )

        assertEquals("Title  next line", message.item.snippet.text)
        assertEquals(listOf(0..4), message.item.snippetMatchRanges)
    }

    @Test
    fun map_outgoingMessage_isShownAsOutgoing() {
        val message = mapMessage(
            text = "title",
            matchRanges = persistentListOf(),
            isIncoming = false,
        )

        assertEquals(true, message.item.isOutgoing)
        assertEquals(emptyList<IntRange>(), message.item.snippetMatchRanges)
    }

    private fun mapMessage(
        text: String,
        matchRanges: ImmutableList<IntRange>,
        isIncoming: Boolean = true,
    ): ConversationListSearchMessageUiModel {
        val content = mapper.map(
            query = QUERY,
            conversationMatches = persistentListOf(),
            messageMatches = persistentListOf(
                messageMatch(
                    text = text,
                    matchRanges = matchRanges,
                    isIncoming = isIncoming,
                ),
            ),
        )

        return (content as ConversationListSearchContentUiState.Results).messages.single()
    }

    private fun messageMatch(
        text: String,
        matchRanges: ImmutableList<IntRange> = persistentListOf(),
        isArchived: Boolean = false,
        isIncoming: Boolean = true,
    ): MessageSearchMatch {
        return MessageSearchMatch(
            item = conversationItem(
                conversationId = INBOX_ID,
                isArchived = isArchived,
            ),
            message = SearchableMessage(
                messageId = MESSAGE_ID,
                conversationId = INBOX_ID,
                text = text,
                receivedTimestamp = MESSAGE_TIMESTAMP,
                isIncoming = isIncoming,
                senderName = SENDER_NAME,
            ),
            textMatchRanges = matchRanges,
        )
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
        private const val MESSAGE_TIMESTAMP = 1_700_000_000_000L
        private const val SENDER_NAME = "Anna"
        private val TITLE_MATCH = 0..4
        private val INBOX_ID = ConversationId("1")
        private val ARCHIVED_ID = ConversationId("2")
        private val MESSAGE_ID = MessageId("10")
    }
}
