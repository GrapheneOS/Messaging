package com.android.messaging.data.conversationlist.matcher

import java.text.Normalizer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val ASCII_LIMIT = 0x80

private val COMBINING_MARKS_REGEX = Regex("\\p{Mn}+")

/**
 * Text lowercased and stripped of accents for matching, which remembers where each of its
 * characters came from so matches can be highlighted in the original text.
 */
internal class FoldedText(
    val text: String,
    private val sourceStarts: IntArray?,
    private val sourceLength: Int,
) {

    fun findSourceRanges(
        terms: List<String>,
        isWordStartOnly: Boolean = false,
    ): ImmutableList<IntRange> {
        val ranges = terms
            .flatMap { term ->
                findOccurrences(
                    term = term,
                    isWordStartOnly = isWordStartOnly,
                )
            }
            .sortedBy(IntRange::first)

        return mergeRanges(ranges)
            .map(::toSourceRange)
            .toImmutableList()
    }

    private fun findOccurrences(
        term: String,
        isWordStartOnly: Boolean,
    ): List<IntRange> {
        return buildList {
            var start = text.indexOf(term)

            while (start >= 0) {
                if (!isWordStartOnly || isWordStart(index = start)) {
                    add(start until start + term.length)
                }

                start = text.indexOf(term, startIndex = start + term.length)
            }
        }
    }

    private fun isWordStart(index: Int): Boolean {
        return index == 0 || !text[index - 1].isLetterOrDigit()
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

internal fun foldText(text: String): FoldedText {
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
