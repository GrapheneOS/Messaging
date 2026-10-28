package com.android.messaging.data.conversationlist.matcher

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationSearchMatch
import com.android.messaging.data.conversationlist.model.MessageSearchMatch
import com.android.messaging.data.conversationlist.model.SearchableConversation
import com.android.messaging.data.conversationlist.model.SearchableMessage
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationSearchMatcher {

    fun index(conversations: List<SearchableConversation>): ConversationSearchIndex
}

internal interface ConversationSearchIndex {

    fun match(query: String): ImmutableList<ConversationSearchMatch>

    /**
     * Pairs the messages found by their text with their conversations, leaving out the ones in
     * conversations search can't find, and finds the words to highlight in them.
     */
    fun matchMessages(
        query: String,
        messages: List<SearchableMessage>,
    ): ImmutableList<MessageSearchMatch>
}

internal class ConversationSearchMatcherImpl @Inject constructor() : ConversationSearchMatcher {

    override fun index(conversations: List<SearchableConversation>): ConversationSearchIndex {
        // Folding is the costly part, so it runs once per change to the conversations rather
        // than once per keystroke
        val entries = conversations.map(::indexConversation)
        val entriesById = entries.associateBy { entry -> entry.item.conversationId }

        return object : ConversationSearchIndex {
            override fun match(query: String): ImmutableList<ConversationSearchMatch> {
                return matchEntries(
                    entries = entries,
                    query = query,
                )
            }

            override fun matchMessages(
                query: String,
                messages: List<SearchableMessage>,
            ): ImmutableList<MessageSearchMatch> {
                return resolveMessages(
                    entriesById = entriesById,
                    query = query,
                    messages = messages,
                )
            }
        }
    }

    private fun indexConversation(conversation: SearchableConversation): IndexedConversation {
        val names = conversation.participants
            .flatMap { participant ->
                listOfNotNull(
                    participant.fullName,
                    participant.firstName,
                    participant.displayDestination,
                )
            }
            .map { name -> foldText(name).text }

        val destinationDigits = conversation.participants
            .flatMap { participant ->
                listOfNotNull(
                    participant.normalizedDestination,
                    participant.normalizedDestination?.let(::formatNationally),
                    participant.displayDestination,
                )
            }
            .map { destination -> destination.digits() }
            .filter(String::isNotEmpty)
            .distinct()

        return IndexedConversation(
            item = conversation.item,
            title = foldText(conversation.item.title.orEmpty()),
            names = names,
            destinationDigits = destinationDigits,
        )
    }

    private fun formatNationally(destination: String): String? {
        if (!destination.startsWith(INTERNATIONAL_PREFIX)) {
            return null
        }

        val phoneNumberUtil = PhoneNumberUtil.getInstance()

        return try {
            phoneNumberUtil.format(
                phoneNumberUtil.parse(destination, null),
                PhoneNumberFormat.NATIONAL,
            )
        } catch (_: NumberParseException) {
            null
        }
    }

    private fun matchEntries(
        entries: List<IndexedConversation>,
        query: String,
    ): ImmutableList<ConversationSearchMatch> {
        val terms = parseTerms(query)

        if (terms.isEmpty()) {
            return persistentListOf()
        }

        return entries
            .mapNotNull { entry ->
                matchEntry(
                    entry = entry,
                    terms = terms,
                )
            }
            .toImmutableList()
    }

    private fun parseTerms(query: String): List<QueryTerm> {
        val words = foldText(query).text
            .split(WHITESPACE_REGEX)
            .filter(String::isNotEmpty)

        return when {
            words.isEmpty() -> emptyList()

            words.all(::isPhoneNumberWord) -> {
                listOf(QueryTerm.Digits(digits = words.joinToString(separator = "").digits()))
            }

            else -> {
                words.map { word ->
                    when {
                        isPhoneNumberWord(word) -> QueryTerm.Digits(digits = word.digits())
                        else -> QueryTerm.Text(text = word)
                    }
                }
            }
        }
    }

    private fun isPhoneNumberWord(word: String): Boolean {
        return word.any(::isAsciiDigit) &&
            word.all { char -> isAsciiDigit(char) || char in PHONE_NUMBER_SEPARATORS }
    }

    private fun matchEntry(
        entry: IndexedConversation,
        terms: List<QueryTerm>,
    ): ConversationSearchMatch? {
        val isMatch = terms.all { term ->
            when (term) {
                is QueryTerm.Text -> {
                    term.text in entry.title.text || entry.names.any { name -> term.text in name }
                }

                is QueryTerm.Digits -> {
                    term.digits.length >= MIN_PHONE_QUERY_DIGITS &&
                        entry.destinationDigits.any { digits -> term.digits in digits }
                }
            }
        }

        if (!isMatch) {
            return null
        }

        return ConversationSearchMatch(
            item = entry.item,
            titleMatchRanges = entry.title.findSourceRanges(
                terms = terms.filterIsInstance<QueryTerm.Text>().map(QueryTerm.Text::text),
                digitTerms = terms
                    .filterIsInstance<QueryTerm.Digits>()
                    .map(QueryTerm.Digits::digits),
            ),
        )
    }

    private fun resolveMessages(
        entriesById: Map<ConversationId, IndexedConversation>,
        query: String,
        messages: List<SearchableMessage>,
    ): ImmutableList<MessageSearchMatch> {
        // The text index matches the starts of words, so highlights only start words too
        val terms = foldText(query).text
            .split(NON_WORD_REGEX)
            .filter(String::isNotEmpty)

        return messages
            .mapNotNull { message ->
                val entry = entriesById[message.conversationId] ?: return@mapNotNull null

                MessageSearchMatch(
                    item = entry.item,
                    message = message,
                    textMatchRanges = foldText(message.text).findSourceRanges(
                        terms = terms,
                        isWordStartOnly = true,
                    ),
                )
            }
            .toImmutableList()
    }

    private class IndexedConversation(
        val item: ConversationListItem,
        val title: FoldedText,
        val names: List<String>,
        val destinationDigits: List<String>,
    )

    private sealed interface QueryTerm {
        data class Text(
            val text: String,
        ) : QueryTerm

        data class Digits(
            val digits: String,
        ) : QueryTerm
    }

    private companion object {
        private const val INTERNATIONAL_PREFIX = "+"
        private const val MIN_PHONE_QUERY_DIGITS = 3
        private const val PHONE_NUMBER_SEPARATORS = "+-(). "

        private val WHITESPACE_REGEX = Regex("\\s+")
        private val NON_WORD_REGEX = Regex("[^\\p{L}\\p{N}]+")
    }
}
