package com.android.messaging.ui.common.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import org.junit.Assert.assertEquals
import org.junit.Test

internal class HighlightedTextTest {

    @Test
    fun buildHighlightedText_stylesEachMatchedRange() {
        val text = buildHighlightedText(
            text = TEXT,
            matchRanges = listOf(0..1, 5..6),
            highlightStyle = HIGHLIGHT,
        )

        assertEquals(TEXT, text.text)
        assertEquals(
            listOf(
                AnnotatedString.Range(HIGHLIGHT, 0, 2),
                AnnotatedString.Range(HIGHLIGHT, 5, 7),
            ),
            text.spanStyles,
        )
    }

    @Test
    fun buildHighlightedText_rangePastTheEnd_isClampedToTheText() {
        val text = buildHighlightedText(
            text = TEXT,
            matchRanges = listOf(8..20),
            highlightStyle = HIGHLIGHT,
        )

        assertEquals(listOf(AnnotatedString.Range(HIGHLIGHT, 8, TEXT.length)), text.spanStyles)
    }

    @Test
    fun buildHighlightedText_emptyOrOutsideRanges_addNoStyle() {
        val text = buildHighlightedText(
            text = TEXT,
            matchRanges = listOf(3..2, 40..45),
            highlightStyle = HIGHLIGHT,
        )

        assertEquals(emptyList<AnnotatedString.Range<SpanStyle>>(), text.spanStyles)
    }

    private companion object {
        private const val TEXT = "Anna Smith"
        private val HIGHLIGHT = SpanStyle(color = Color.Red)
    }
}
