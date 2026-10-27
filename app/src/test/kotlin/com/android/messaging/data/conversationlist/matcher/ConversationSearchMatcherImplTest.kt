package com.android.messaging.data.conversationlist.matcher

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.SearchableConversation
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
    fun match_nationalFormatDigits_findsConversationByDisplayedNumberAndHighlightsThem() {
        val conversation = searchableConversation(
            id = "1",
            title = UK_DISPLAY_NUMBER,
            participants = listOf(UK_PARTICIPANT),
        )

        val matches = matcher.index(listOf(conversation)).match("0791112")

        assertEquals(listOf(0..7), matches.single().titleMatchRanges)
    }

    @Test
    fun match_internationalFormatDigits_findsConversationByNormalizedNumber() {
        val conversation = searchableConversation(
            id = "1",
            title = UK_DISPLAY_NUMBER,
            participants = listOf(UK_PARTICIPANT),
        )

        val matches = matcher.index(listOf(conversation)).match("+44 7911 123")

        assertEquals(1, matches.size)
    }

    @Test
    fun match_tooFewDigitsOfTheDisplayedNumber_findsNothing() {
        val conversation = searchableConversation(
            id = "1",
            title = UK_DISPLAY_NUMBER,
            participants = listOf(UK_PARTICIPANT),
        )
        val index = matcher.index(listOf(conversation))

        assertTrue(index.match("07").isEmpty())
        assertTrue(index.match("1 2").isEmpty())
    }

    @Test
    fun match_nationalFormatDigitsOfAnInternationallyDisplayedNumber_findsConversation() {
        val conversation = searchableConversation(
            id = "1",
            title = UK_INTERNATIONAL_DISPLAY_NUMBER,
            participants = listOf(
                participant(
                    displayDestination = UK_INTERNATIONAL_DISPLAY_NUMBER,
                    normalizedDestination = UK_NORMALIZED_NUMBER,
                ),
            ),
        )
        val index = matcher.index(listOf(conversation))

        assertEquals(1, index.match("07911 123456").size)
        assertEquals(1, index.match("0791112").size)
    }

    @Test
    fun match_nationalFormatDigitsWithNonZeroTrunkPrefix_findsConversation() {
        val conversation = searchableConversation(
            id = "1",
            title = "Ivan",
            participants = listOf(participant(normalizedDestination = "+79123456789")),
        )

        val matches = matcher.index(listOf(conversation)).match("8 912 345-67-89")

        assertEquals(1, matches.size)
    }

    @Test
    fun match_destinationWithoutNationalForm_findsConversationByItsOwnDigits() {
        val conversation = searchableConversation(
            id = "1",
            title = "Codes",
            participants = listOf(
                participant(normalizedDestination = "+000123"),
                participant(normalizedDestination = "72345"),
            ),
        )
        val index = matcher.index(listOf(conversation))

        assertEquals(1, index.match("0001").size)
        assertEquals(1, index.match("723").size)
    }

    @Test
    fun match_nameAndEnoughDigits_findsConversation() {
        val conversation = searchableConversation(
            id = "1",
            title = "Mum",
            participants = listOf(participant(normalizedDestination = "+15551234567")),
        )

        val matches = matcher.index(listOf(conversation)).match("mum 555")

        assertEquals(listOf(0..2), matches.single().titleMatchRanges)
    }

    @Test
    fun match_nameAndTooFewDigits_findsNothing() {
        val conversation = searchableConversation(
            id = "1",
            title = "Mum 5",
            participants = listOf(participant(normalizedDestination = "+15551234567")),
        )

        val matches = matcher.index(listOf(conversation)).match("mum 5")

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

    private companion object {
        private const val UK_DISPLAY_NUMBER = "07911 123456"
        private const val UK_INTERNATIONAL_DISPLAY_NUMBER = "+44 7911 123456"
        private const val UK_NORMALIZED_NUMBER = "+447911123456"

        private val UK_PARTICIPANT = SearchableParticipant(
            fullName = null,
            firstName = null,
            displayDestination = UK_DISPLAY_NUMBER,
            normalizedDestination = UK_NORMALIZED_NUMBER,
        )
    }
}
