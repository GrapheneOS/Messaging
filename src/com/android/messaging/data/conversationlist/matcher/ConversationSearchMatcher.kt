package com.android.messaging.data.conversationlist.matcher

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationSearchMatch
import com.android.messaging.data.conversationlist.model.MessageSearchMatch
import com.android.messaging.data.conversationlist.model.SearchableConversation
import com.android.messaging.data.conversationlist.model.SearchableMessage
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
            .mapNotNull { participant ->
                participant.normalizedDestination
                    ?.filter(::isAsciiDigit)
                    ?.takeIf(String::isNotEmpty)
            }

        return IndexedConversation(
            item = conversation.item,
            title = foldText(conversation.item.title.orEmpty()),
            names = names,
            destinationDigits = destinationDigits,
        )
    }

    private fun matchEntries(
        entries: List<IndexedConversation>,
        query: String,
    ): ImmutableList<ConversationSearchMatch> {
        val terms = foldText(query).text
            .split(WHITESPACE_REGEX)
            .filter(String::isNotEmpty)

        if (terms.isEmpty()) {
            return persistentListOf()
        }

        val queryDigits = phoneQueryDigitsOrNull(query)

        return entries
            .mapNotNull { entry ->
                matchEntry(
                    entry = entry,
                    terms = terms,
                    queryDigits = queryDigits,
                )
            }
            .toImmutableList()
    }

    private fun matchEntry(
        entry: IndexedConversation,
        terms: List<String>,
        queryDigits: String?,
    ): ConversationSearchMatch? {
        // Each word may match a different field, so "anna bob" finds the group they are both in
        val matchesNames = terms.all { term ->
            term in entry.title.text || entry.names.any { name -> term in name }
        }
        val matchesDestination = queryDigits != null &&
            entry.destinationDigits.any { digits -> queryDigits in digits }

        if (!matchesNames && !matchesDestination) {
            return null
        }

        return ConversationSearchMatch(
            item = entry.item,
            titleMatchRanges = entry.title.findSourceRanges(terms),
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

    private fun phoneQueryDigitsOrNull(query: String): String? {
        val trimmedQuery = query.trim()
        val isPhoneQuery = trimmedQuery.all { char ->
            isAsciiDigit(char) || char in PHONE_NUMBER_SEPARATORS
        }

        return trimmedQuery
            .filter(::isAsciiDigit)
            .takeIf { digits -> isPhoneQuery && digits.length >= MIN_PHONE_QUERY_DIGITS }
    }

    private fun isAsciiDigit(char: Char): Boolean {
        return char in '0'..'9'
    }

    private class IndexedConversation(
        val item: ConversationListItem,
        val title: FoldedText,
        val names: List<String>,
        val destinationDigits: List<String>,
    )

    private companion object {
        private const val MIN_PHONE_QUERY_DIGITS = 3
        private const val PHONE_NUMBER_SEPARATORS = "+-(). "

        private val WHITESPACE_REGEX = Regex("\\s+")
        private val NON_WORD_REGEX = Regex("[^\\p{L}\\p{N}]+")
    }
}
