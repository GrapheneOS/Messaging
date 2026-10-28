package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent

private const val MESSAGE_HIGHLIGHT_ALPHA = 0.24f

/**
 * Flashes a tint over the bubble of a message the screen was opened at, fading in and back out.
 * Drawn over the content inside the bubble's clip, so it takes the bubble's shape whether that is
 * a text bubble or bare media, and leaves the timestamp and the rest of the row alone.
 */
@Composable
internal fun Modifier.conversationMessageHighlight(isHighlighted: Boolean): Modifier {
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = MESSAGE_HIGHLIGHT_ALPHA)
    val color by animateColorAsState(
        targetValue = when {
            isHighlighted -> highlightColor
            else -> highlightColor.copy(alpha = 0f)
        },
        animationSpec = tween(
            durationMillis = MessageHighlightFadeDuration.inWholeMilliseconds.toInt(),
        ),
        label = "conversationMessageHighlight",
    )

    // Read while drawing, so the fade redraws the bubble without recomposing the message.
    return drawWithContent {
        drawContent()

        if (color.alpha > 0f) {
            drawRect(color = color)
        }
    }
}
