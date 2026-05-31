package com.android.messaging.ui.conversation.messages.ui.text.links

import android.content.Context
import android.net.Uri
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLinks
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLink
import com.android.messaging.ui.conversation.messages.ui.text.extractConversationTextLinks
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageTextLinkExtractorTest {

    @Test
    fun extractConversationTextLinks_returnsEmptyListForBlankTextWithoutClassifierLookup() {
        val context = mockk<Context>(relaxed = true)

        val result = extractConversationTextLinks(
            context = context,
            text = "  ",
        )

        assertTrue(result.isEmpty())
        verify(exactly = 0) {
            context.getSystemService(TextClassificationManager::class.java)
        }
    }

    @Test
    fun extractConversationTextLinks_mapsSupportedEntitiesAndSortsByStartOffset() {
        val text = "Email a@b.com, call +15551212, go to https://example.com/a, meet at 1 Main St"
        val emailRange = text.rangeOf("a@b.com")
        val phoneRange = text.rangeOf("+15551212")
        val urlRange = text.rangeOf("https://example.com/a")
        val addressRange = text.rangeOf("1 Main St")
        val textLinks = buildTextLinks(
            text = text,
            LinkSpec(range = urlRange, entityType = TextClassifier.TYPE_URL),
            LinkSpec(range = phoneRange, entityType = TextClassifier.TYPE_PHONE),
            LinkSpec(range = emailRange, entityType = TextClassifier.TYPE_EMAIL),
            LinkSpec(range = addressRange, entityType = TextClassifier.TYPE_ADDRESS),
        )
        val requestSlot = slot<TextLinks.Request>()
        val context = createContextWithClassifier(
            textLinks = textLinks,
            requestSlot = requestSlot,
        )

        val result = extractConversationTextLinks(
            context = context,
            text = text,
        )

        assertEquals(text, requestSlot.captured.text.toString())
        assertEquals(
            listOf(
                ConversationTextLink(
                    start = emailRange.first,
                    end = emailRange.last + 1,
                    url = "mailto:${Uri.encode("a@b.com")}",
                ),
                ConversationTextLink(
                    start = phoneRange.first,
                    end = phoneRange.last + 1,
                    url = "tel:${Uri.encode("+15551212")}",
                ),
                ConversationTextLink(
                    start = urlRange.first,
                    end = urlRange.last + 1,
                    url = "https://example.com/a",
                ),
                ConversationTextLink(
                    start = addressRange.first,
                    end = addressRange.last + 1,
                    url = "geo:0,0?q=${Uri.encode("1 Main St")}",
                ),
            ),
            result,
        )
    }

    @Test
    fun extractConversationTextLinks_skipsBlankAndUnsupportedLinks() {
        val text = "bad   ok unsupported"
        val blankRange = text.rangeOf("   ")
        val validRange = text.rangeOf("ok")
        val unsupportedRange = text.rangeOf("unsupported")
        val textLinks = buildTextLinks(
            text = text,
            LinkSpec(range = blankRange, entityType = TextClassifier.TYPE_EMAIL),
            LinkSpec(range = unsupportedRange, entityType = "unsupported"),
            LinkSpec(range = validRange, entityType = TextClassifier.TYPE_PHONE),
        )
        val context = createContextWithClassifier(textLinks = textLinks)

        val result = extractConversationTextLinks(
            context = context,
            text = text,
        )

        assertEquals(
            listOf(
                ConversationTextLink(
                    start = validRange.first,
                    end = validRange.last + 1,
                    url = "tel:${Uri.encode("ok")}",
                ),
            ),
            result,
        )
    }

    @Test
    fun extractConversationTextLinks_usesNoOpClassifierWhenSystemServiceIsMissing() {
        val context = mockk<Context>()
        every {
            context.getSystemService(TextClassificationManager::class.java)
        } returns null

        val result = extractConversationTextLinks(
            context = context,
            text = "Call +15551212",
        )

        assertTrue(result.isEmpty())
    }

    private fun createContextWithClassifier(
        textLinks: TextLinks,
        requestSlot: CapturingSlot<TextLinks.Request>? = null,
    ): Context {
        val context = mockk<Context>()
        val manager = mockk<TextClassificationManager>()
        val classifier = mockk<TextClassifier>()
        every { context.getSystemService(TextClassificationManager::class.java) } returns manager
        every { manager.textClassifier } returns classifier
        if (requestSlot != null) {
            every { classifier.generateLinks(capture(requestSlot)) } returns textLinks
        } else {
            every { classifier.generateLinks(any()) } returns textLinks
        }
        return context
    }

    private fun buildTextLinks(text: String, vararg specs: LinkSpec): TextLinks {
        val builder = TextLinks.Builder(text)
        specs.forEach { spec ->
            builder.addLink(
                spec.range.first,
                spec.range.last + 1,
                mapOf(spec.entityType to 1.0f),
            )
        }
        return builder.build()
    }

    private fun String.rangeOf(value: String): IntRange {
        val start = indexOf(value)
        check(start >= 0)
        return start until (start + value.length)
    }

    private data class LinkSpec(
        val range: IntRange,
        val entityType: String,
    )
}
