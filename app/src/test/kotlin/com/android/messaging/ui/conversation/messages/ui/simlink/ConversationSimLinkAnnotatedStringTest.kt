package com.android.messaging.ui.conversation.messages.ui.simlink

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.style.TextDecoration
import com.android.messaging.ui.conversation.messages.ui.buildConversationSimLinkAnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val LINK_COLOR = Color(0xFF336699)

internal class ConversationSimLinkAnnotatedStringTest {

    @Test
    fun buildConversationSimLinkAnnotatedString_placesClickableSimNameInTemplate() {
        var clickCount = 0

        val result = buildConversationSimLinkAnnotatedString(
            annotationTemplate = $$"via %1$s now",
            simDisplayName = "Work SIM",
            linkColor = LINK_COLOR,
            onSimSelectorClick = { clickCount++ },
            isLinkEnabled = true,
            leadingText = "Queued",
            leadingSeparator = " · ",
        )

        val linkStart = result.text.indexOf("Work SIM")
        val linkEnd = linkStart + "Work SIM".length
        val linkAnnotationRange = result.getLinkAnnotations(
            start = linkStart,
            end = linkEnd,
        ).single()
        val linkAnnotation = linkAnnotationRange.item as LinkAnnotation.Clickable

        assertEquals("Queued · via Work SIM now", result.text)
        assertEquals(linkStart, linkAnnotationRange.start)
        assertEquals(linkEnd, linkAnnotationRange.end)
        assertEquals("sim_selector", linkAnnotation.tag)
        assertTrue(LINK_COLOR == linkAnnotation.styles?.style?.color)
        assertEquals(TextDecoration.Underline, linkAnnotation.styles?.style?.textDecoration)

        linkAnnotation.linkInteractionListener?.onClick(linkAnnotation)

        assertEquals(1, clickCount)
    }

    @Test
    fun buildConversationSimLinkAnnotatedString_omitsLinkAnnotationWhenDisabled() {
        val result = buildConversationSimLinkAnnotatedString(
            annotationTemplate = $$"via %1$s",
            simDisplayName = "Personal SIM",
            linkColor = LINK_COLOR,
            onSimSelectorClick = {},
            isLinkEnabled = false,
        )

        assertEquals("via Personal SIM", result.text)
        assertFalse(
            result.hasLinkAnnotations(
                start = 0,
                end = result.text.length,
            ),
        )
    }

    @Test
    fun buildConversationSimLinkAnnotatedString_appendsSimNameWhenTemplateHasNoPlaceholder() {
        val result = buildConversationSimLinkAnnotatedString(
            annotationTemplate = "Sending from ",
            simDisplayName = "Travel SIM",
            linkColor = LINK_COLOR,
            onSimSelectorClick = {},
        )

        assertEquals("Sending from Travel SIM", result.text)
        assertTrue(
            result.hasLinkAnnotations(
                start = "Sending from ".length,
                end = result.text.length,
            ),
        )
    }

    @Test
    fun buildConversationSimLinkAnnotatedString_skipsLeadingSeparatorWhenLeadingTextIsEmpty() {
        val result = buildConversationSimLinkAnnotatedString(
            annotationTemplate = $$"%1$s",
            simDisplayName = "Only SIM",
            linkColor = LINK_COLOR,
            onSimSelectorClick = {},
            leadingText = "",
            leadingSeparator = " · ",
        )

        assertEquals("Only SIM", result.text)
    }
}
