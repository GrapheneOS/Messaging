package com.android.messaging.ui.conversation.messages.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

private const val SIM_ANNOTATION_PLACEHOLDER = $$"%1$s"
private const val SIM_LINK_TAG = "sim_selector"

internal fun buildConversationSimLinkAnnotatedString(
    annotationTemplate: String,
    simDisplayName: String,
    linkColor: Color,
    onSimSelectorClick: () -> Unit,
    isLinkEnabled: Boolean = true,
    leadingText: String? = null,
    leadingSeparator: String = "",
): AnnotatedString {
    val placeholderIndex = annotationTemplate.indexOf(SIM_ANNOTATION_PLACEHOLDER)

    val annotationPrefix = when {
        placeholderIndex >= 0 -> annotationTemplate.substring(0, placeholderIndex)
        else -> annotationTemplate
    }

    val annotationSuffix = when {
        placeholderIndex >= 0 -> {
            annotationTemplate.substring(
                placeholderIndex + SIM_ANNOTATION_PLACEHOLDER.length,
            )
        }

        else -> ""
    }

    return buildAnnotatedString {
        if (!leadingText.isNullOrEmpty()) {
            append(leadingText)
            append(leadingSeparator)
        }

        append(annotationPrefix)

        when {
            isLinkEnabled -> appendClickableSimDisplayName(
                simDisplayName = simDisplayName,
                linkColor = linkColor,
                onSimSelectorClick = onSimSelectorClick,
            )

            else -> {
                append(simDisplayName)
            }
        }

        if (annotationSuffix.isNotEmpty()) {
            append(annotationSuffix)
        }
    }
}

private fun AnnotatedString.Builder.appendClickableSimDisplayName(
    simDisplayName: String,
    linkColor: Color,
    onSimSelectorClick: () -> Unit,
) {
    val link = LinkAnnotation.Clickable(
        tag = SIM_LINK_TAG,
        styles = TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
            ),
        ),
    ) {
        onSimSelectorClick()
    }

    withLink(link = link) {
        append(simDisplayName)
    }
}
