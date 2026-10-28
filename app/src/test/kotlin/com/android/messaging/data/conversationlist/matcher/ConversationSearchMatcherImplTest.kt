package com.android.messaging.data.conversationlist.matcher

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversationlist.model.SearchableConversation
import com.android.messaging.data.conversationlist.model.SearchableMessage
import com.android.messaging.data.conversationlist.model.SearchableParticipant
import com.android.messaging.ui.conversationlist.conversationItem
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationSearchMatcherImplTest {

    private val matcher = ConversationSearchMatcherImpl()

    @Test
    fun match_titleContainsQuery_findsConversationAndHighlightsTitle() {
        val conversation = searchableConversation(id = "1", title = "Anna Smith")

        val matches = matcher.index(listOf(conversation)).match("smi")

        assertEquals(listOf(ConversationId("1")), matches.map { it.item.conversationId })
        assertEquals(listOf(5..7), matches.single().titleMatchRanges)
    }

    @Test
    fun match_differentCaseAndAccents_findsConversation() {
        val conversation = searchableConversation(id = "1", title = "Zoë Ángel")

        val matches = matcher.index(listOf(conversation)).match("ZOE angel")

        assertEquals(1, matches.size)
        assertEquals(listOf(0..2, 4..8), matches.single().titleMatchRanges)
    }

    @Test
    fun match_cyrillicInDifferentCase_findsConversation() {
        val conversation = searchableConversation(id = "1", title = "Олена Петренко")

        val matches = matcher.index(listOf(conversation)).match("ПЕТР")

        assertEquals(listOf(6..9), matches.single().titleMatchRanges)
    }

    @Test
    fun match_titleWithDecomposedAccent_highlightsTheCombiningMarkToo() {
        val conversation = searchableConversation(id = "1", title = "José Luis")

        val matches = matcher.index(listOf(conversation)).match("jose")

        assertEquals(listOf(0..4), matches.single().titleMatchRanges)
    }

    @Test
    fun match_participantFullNameOutsideGroupTitle_findsGroupWithoutHighlight() {
        val conversation = searchableConversation(
            id = "1",
            title = "Anna, Bob",
            participants = listOf(
                participant(fullName = "Anna Smith", firstName = "Anna"),
                participant(fullName = "Bob Jones", firstName = "Bob"),
            ),
        )

        val matches = matcher.index(listOf(conversation)).match("jones")

        assertEquals(1, matches.size)
        assertTrue(matches.single().titleMatchRanges.isEmpty())
    }

    @Test
    fun match_wordsMatchingDifferentParticipants_findsTheirGroup() {
        val group = searchableConversation(
            id = "group",
            title = "Anna, Bob",
            participants = listOf(
                participant(fullName = "Anna Smith"),
                participant(fullName = "Bob Jones"),
            ),
        )
        val other = searchableConversation(
            id = "other",
            title = "Anna Smith",
            participants = listOf(participant(fullName = "Anna Smith")),
        )

        val matches = matcher.index(listOf(group, other)).match("smith jones")

        assertEquals(listOf(ConversationId("group")), matches.map { it.item.conversationId })
    }

    @Test
    fun match_phoneDigitsInAnyFormat_findsConversationByDestination() {
        val conversation = searchableConversation(
            id = "1",
            title = "Mum",
            participants = listOf(participant(normalizedDestination = "+15551234567")),
        )

        val matches = matcher.index(listOf(conversation)).match("(555) 123-45")

        assertEquals(1, matches.size)
    }

    @Test
    fun match_tooFewDigits_doesNotMatchDestination() {
        val conversation = searchableConversation(
            id = "1",
            title = "Mum",
            participants = listOf(participant(normalizedDestination = "+15551234567")),
        )

        val matches = matcher.index(listOf(conversation)).match("55")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun match_digitsMixedWithLetters_doesNotMatchDestination() {
        val conversation = searchableConversation(
            id = "1",
            title = "Mum",
            participants = listOf(participant(normalizedDestination = "+15551234567")),
        )

        val matches = matcher.index(listOf(conversation)).match("555 mum2")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun match_repeatedAndOverlappingTerms_mergesHighlightRanges() {
        val conversation = searchableConversation(id = "1", title = "ana banana")

        val matches = matcher.index(listOf(conversation)).match("an nan")

        assertEquals(listOf(0..1, 5..8), matches.single().titleMatchRanges)
    }

    @Test
    fun match_blankQuery_findsNothing() {
        val conversation = searchableConversation(id = "1", title = "Anna")

        val matches = matcher.index(listOf(conversation)).match("   ")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun match_noConversationMatches_findsNothing() {
        val conversation = searchableConversation(id = "1", title = "Anna")

        val matches = matcher.index(listOf(conversation)).match("bob")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun match_severalMatches_keepsTheConversationsInTheirOrder() {
        val conversations = listOf(
            searchableConversation(id = "b", title = "Anna B"),
            searchableConversation(id = "a", title = "Anna A"),
        )

        val matches = matcher.index(conversations).match("anna")

        assertEquals(
            listOf(ConversationId("b"), ConversationId("a")),
            matches.map { it.item.conversationId },
        )
    }

    @Test
    fun matchMessages_pairsEachMessageWithItsConversationInTheMessagesOrder() {
        val first = searchableConversation(id = "1", title = "Anna")
        val second = searchableConversation(id = "2", title = "Bob")

        val matches = matcher.index(listOf(first, second)).matchMessages(
            query = "lunch",
            messages = listOf(
                message(id = "20", conversationId = "2", text = "Lunch?"),
                message(id = "10", conversationId = "1", text = "Lunch at noon"),
            ),
        )

        assertEquals(
            listOf("2" to "20", "1" to "10"),
            matches.map { match ->
                match.item.conversationId.value to match.message.messageId.value
            },
        )
    }

    @Test
    fun matchMessages_messageOfAConversationSearchLeavesOut_isDropped() {
        val conversation = searchableConversation(id = "1", title = "Anna")

        val matches = matcher.index(listOf(conversation)).matchMessages(
            query = "lunch",
            messages = listOf(message(id = "30", conversationId = "blocked", text = "Lunch")),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun matchMessages_highlightsWordStartsIgnoringCaseAndAccents() {
        val conversation = searchableConversation(id = "1", title = "Anna")

        val matches = matcher.index(listOf(conversation)).matchMessages(
            query = "CAFE, pre",
            messages = listOf(
                message(id = "10", conversationId = "1", text = "Café? Prepare the espresso"),
            ),
        )

        // "pre" inside "espresso" doesn't start a word, so the text index didn't match it there
        assertEquals(listOf(0..3, 6..8), matches.single().textMatchRanges)
    }

    private fun message(
        id: String,
        conversationId: String,
        text: String,
    ): SearchableMessage {
        return SearchableMessage(
            messageId = MessageId(id),
            conversationId = ConversationId(conversationId),
            text = text,
            receivedTimestamp = 0L,
            isIncoming = true,
            senderName = null,
        )
    }

    private fun searchableConversation(
        id: String,
        title: String,
        participants: List<SearchableParticipant> = emptyList(),
    ): SearchableConversation {
        return SearchableConversation(
            item = conversationItem(conversationId = ConversationId(id)).copy(title = title),
            participants = participants.toImmutableList(),
        )
    }

    private fun participant(
        fullName: String? = null,
        firstName: String? = null,
        displayDestination: String? = null,
        normalizedDestination: String? = null,
    ): SearchableParticipant {
        return SearchableParticipant(
            fullName = fullName,
            firstName = firstName,
            displayDestination = displayDestination,
            normalizedDestination = normalizedDestination,
        )
    }
}
