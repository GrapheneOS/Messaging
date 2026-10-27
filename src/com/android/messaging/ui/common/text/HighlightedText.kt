package com.android.messaging.ui.common.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun highlightedText(
    text: String,
    matchRanges: List<IntRange>,
): AnnotatedString {
    val highlightColor = MaterialTheme.colorScheme.primary

    return remember(text, matchRanges, highlightColor) {
        buildHighlightedText(
            text = text,
            matchRanges = matchRanges,
            highlightStyle = SpanStyle(
                color = highlightColor,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

internal fun buildHighlightedText(
    text: String,
    matchRanges: List<IntRange>,
    highlightStyle: SpanStyle,
): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        matchRanges.forEach { range ->
            val start = range.first.coerceIn(0, text.length)
            val end = (range.last + 1).coerceIn(start, text.length)

            if (start < end) {
                addStyle(
                    style = highlightStyle,
                    start = start,
                    end = end,
                )
            }
        }
    }
}
