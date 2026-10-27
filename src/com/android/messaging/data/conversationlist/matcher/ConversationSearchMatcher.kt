package com.android.messaging.data.conversationlist.matcher

import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationSearchMatch
import com.android.messaging.data.conversationlist.model.SearchableConversation
import java.text.Normalizer
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationSearchMatcher {

    fun index(conversations: List<SearchableConversation>): ConversationSearchIndex
}

internal fun interface ConversationSearchIndex {

    fun match(query: String): ImmutableList<ConversationSearchMatch>
}

internal class ConversationSearchMatcherImpl @Inject constructor() : ConversationSearchMatcher {

    override fun index(conversations: List<SearchableConversation>): ConversationSearchIndex {
        // Folding is the costly part, so it runs once per change to the conversations rather
        // than once per keystroke
        val entries = conversations.map(::indexConversation)

        return ConversationSearchIndex { query ->
            matchEntries(
                entries = entries,
                query = query,
            )
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

    private fun phoneQueryDigitsOrNull(query: String): String? {
        val trimmedQuery = query.trim()
        val isPhoneQuery = trimmedQuery.all { char ->
            isAsciiDigit(char) || char in PHONE_NUMBER_SEPARATORS
        }

        return trimmedQuery
            .filter(::isAsciiDigit)
            .takeIf { digits -> isPhoneQuery && digits.length >= MIN_PHONE_QUERY_DIGITS }
    }

    private fun foldText(text: String): FoldedText {
        if (text.all { char -> char.code < ASCII_LIMIT }) {
            return FoldedText(
                text = text.lowercase(),
                sourceStarts = null,
                sourceLength = text.length,
            )
        }

        val folded = StringBuilder(text.length)
        val sourceStarts = ArrayList<Int>(text.length)
        var index = 0

        // Folding each code point on its own keeps track of where every folded character came
        // from, so matches can be highlighted in the original text
        while (index < text.length) {
            val charCount = Character.charCount(text.codePointAt(index))
            val foldedCodePoint = Normalizer
                .normalize(text.substring(index, index + charCount), Normalizer.Form.NFD)
                .replace(COMBINING_MARKS_REGEX, "")
                .lowercase()

            foldedCodePoint.forEach { char ->
                folded.append(char)
                sourceStarts.add(index)
            }

            index += charCount
        }

        return FoldedText(
            text = folded.toString(),
            sourceStarts = sourceStarts.toIntArray(),
            sourceLength = text.length,
        )
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

    private class FoldedText(
        val text: String,
        private val sourceStarts: IntArray?,
        private val sourceLength: Int,
    ) {

        fun findSourceRanges(terms: List<String>): ImmutableList<IntRange> {
            val ranges = terms
                .flatMap(::findOccurrences)
                .sortedBy(IntRange::first)

            return mergeRanges(ranges)
                .map(::toSourceRange)
                .toImmutableList()
        }

        private fun findOccurrences(term: String): List<IntRange> {
            return buildList {
                var start = text.indexOf(term)

                while (start >= 0) {
                    add(start until start + term.length)
                    start = text.indexOf(term, startIndex = start + term.length)
                }
            }
        }

        private fun mergeRanges(sortedRanges: List<IntRange>): List<IntRange> {
            val merged = ArrayList<IntRange>(sortedRanges.size)

            sortedRanges.forEach { range ->
                val last = merged.lastOrNull()

                when {
                    last != null && range.first <= last.last + 1 -> {
                        merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
                    }

                    else -> merged.add(range)
                }
            }

            return merged
        }

        private fun toSourceRange(range: IntRange): IntRange {
            val starts = sourceStarts ?: return range

            val start = starts[range.first]

            // Ending just before the next folded character's source keeps any combining marks
            // that trail the last matched character inside the range
            val end = when {
                range.last + 1 < starts.size -> starts[range.last + 1] - 1
                else -> sourceLength - 1
            }

            return start..maxOf(start, end)
        }
    }

    private companion object {
        private const val ASCII_LIMIT = 0x80
        private const val MIN_PHONE_QUERY_DIGITS = 3
        private const val PHONE_NUMBER_SEPARATORS = "+-(). "

        private val COMBINING_MARKS_REGEX = Regex("\\p{Mn}+")
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
