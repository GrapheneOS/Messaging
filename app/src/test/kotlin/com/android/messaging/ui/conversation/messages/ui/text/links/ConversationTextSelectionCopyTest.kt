package com.android.messaging.ui.conversation.messages.ui.text.links

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLinkStyles
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLink
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLinkKind
import com.android.messaging.ui.conversation.messages.ui.text.buildConversationLinkedAnnotatedString
import com.android.messaging.ui.conversation.messages.ui.text.clipboardText
import com.android.messaging.ui.conversation.messages.ui.text.selectedConversationTextLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class ConversationTextSelectionCopyTest {
    @Test
    fun partialLinkWithSurroundingProseCopiesItsFullResolvedTarget() {
        val text = "Visit example.com/path now"
        val link = link(text = text, value = "example.com/path", url = "http://example.com/path")
        val selection = annotated(text = text, links = listOf(link)).subSequence(0, link.start + 3)
        val selected = selectedConversationTextLink(
            selectedTexts = listOf(selection),
            links = listOf(link),
        )
        assertEquals(link, selected)
        assertEquals("http://example.com/path", selected?.clipboardText())
    }

    @Test
    fun twoOccurrencesOfTheSameDestinationAreAmbiguous() {
        val text = "example.com and example.com"
        val first = link(text = text, value = "example.com", url = "http://example.com")
        val second = first.copy(start = 16, end = 27)
        val links = listOf(first, second)
        assertNull(
            selectedConversationTextLink(
                selectedTexts = listOf(annotated(text = text, links = links)),
                links = links,
            ),
        )
    }

    @Test
    fun touchingRangeBoundariesAndEmptySelectionDoNotSelectALink() {
        val text = "Go example.com now"
        val link = link(text = text, value = "example.com")
        val annotated = annotated(text = text, links = listOf(link))
        listOf(
            annotated.subSequence(0, link.start),
            annotated.subSequence(link.end, text.length),
            annotated.subSequence(link.start, link.start),
        ).forEach { selected ->
            assertNull(
                selectedConversationTextLink(
                    selectedTexts = listOf(selected),
                    links = listOf(link),
                ),
            )
        }
    }

    @Test
    fun rangeUpdatesNeverReuseAPreviouslySelectedLink() {
        val text = "one.com plain two.com"
        val first = link(text = text, value = "one.com")
        val second = link(text = text, value = "two.com")
        val links = listOf(first, second)
        val annotated = annotated(text = text, links = links)
        val selections = listOf(
            annotated.subSequence(first.start, first.end),
            annotated,
            annotated.subSequence(second.start, second.end),
            annotated.subSequence(first.end, second.start),
        )
        assertEquals(
            listOf(first, null, second, null),
            selections.map { selected ->
                selectedConversationTextLink(selectedTexts = listOf(selected), links = links)
            },
        )
    }

    @Test
    fun nonWebCopyPreservesOriginalCharactersInsteadOfDecodingUris() {
        val values = mapOf(
            ConversationTextLinkKind.Phone to "+31 (6) 2222 3333",
            ConversationTextLinkKind.Email to "name+tag@example.com",
            ConversationTextLinkKind.Address to "1 Main St, Tallinn",
        )
        values.forEach { (kind, value) ->
            val link = link(
                text = value,
                value = value,
                url = "scheme:encoded%2Bvalue",
            ).copy(kind = kind)
            assertEquals(value, link.clipboardText())
        }
    }

    @Test
    fun subjectAndBodySelectionCountsOnlyBodyLinkOccurrences() {
        val text = "Open example.com"
        val link = link(text = text, value = "example.com")
        assertEquals(
            link,
            selectedConversationTextLink(
                selectedTexts = listOf(
                    AnnotatedString("Subject"),
                    annotated(text = text, links = listOf(link)),
                ),
                links = listOf(link),
            ),
        )
    }

    private fun annotated(text: String, links: List<ConversationTextLink>): AnnotatedString {
        return buildConversationLinkedAnnotatedString(
            text = text,
            links = links,
            linkStyle = TextLinkStyles(),
            onExternalUriClick = {},
        )
    }

    private fun link(text: String, value: String, url: String = value): ConversationTextLink {
        val start = text.indexOf(string = value)
        return ConversationTextLink(
            start = start,
            end = start + value.length,
            url = url,
            kind = ConversationTextLinkKind.Web,
            originalText = value,
        )
    }
}
