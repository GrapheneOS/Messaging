package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.MessagingPreviewColumn

private val MESSAGE_SELECTION_INDICATOR_SIZE = 22.dp
private val MESSAGE_SELECTION_INDICATOR_HORIZONTAL_GAP = 16.dp
private val MESSAGE_SELECTION_INDICATOR_GUTTER_WIDTH = MESSAGE_SELECTION_INDICATOR_SIZE +
    MESSAGE_SELECTION_INDICATOR_HORIZONTAL_GAP
private val MESSAGE_SELECTION_INDICATOR_CHECK_SIZE = 16.dp
private val MESSAGE_SELECTION_INDICATOR_BORDER_WIDTH = 2.dp

@Composable
internal fun ConversationMessageSelectionIndicator(
    visible: Boolean,
    isSelected: Boolean,
    expandFrom: Alignment.Horizontal,
    shrinkTowards: Alignment.Horizontal,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            ),
        ) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialScale = 0.8f,
        ) + expandHorizontally(
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            ),
            expandFrom = expandFrom,
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 150,
                easing = FastOutSlowInEasing,
            ),
        ) + scaleOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            targetScale = 0.8f,
        ) + shrinkHorizontally(
            animationSpec = tween(
                durationMillis = 150,
                easing = FastOutSlowInEasing,
            ),
            shrinkTowards = shrinkTowards,
        ),
        label = "conversationMessageSelectionIndicatorVisibility",
    ) {
        ConversationMessageSelectionIndicatorContent(
            isSelected = isSelected,
        )
    }
}

@Composable
private fun ConversationMessageSelectionIndicatorContent(
    isSelected: Boolean,
) {
    val selectionTransition = updateTransition(
        targetState = isSelected,
        label = "conversationMessageSelectionIndicator",
    )

    val containerColor by selectionTransition.animateSelectionIndicatorContainerColor()
    val borderColor by selectionTransition.animateSelectionIndicatorBorderColor()
    val checkmarkAlpha by selectionTransition.animateSelectionIndicatorCheckmarkAlpha()
    val checkmarkScale by selectionTransition.animateSelectionIndicatorCheckmarkScale()

    Box(
        modifier = Modifier
            .width(width = MESSAGE_SELECTION_INDICATOR_GUTTER_WIDTH)
            .height(height = MESSAGE_SELECTION_INDICATOR_SIZE),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(size = MESSAGE_SELECTION_INDICATOR_SIZE)
                .background(
                    color = containerColor,
                    shape = CircleShape,
                )
                .border(
                    width = MESSAGE_SELECTION_INDICATOR_BORDER_WIDTH,
                    color = borderColor,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier
                    .size(size = MESSAGE_SELECTION_INDICATOR_CHECK_SIZE)
                    .graphicsLayer {
                        alpha = checkmarkAlpha
                        scaleX = checkmarkScale
                        scaleY = checkmarkScale
                    },
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
internal fun ConversationMessageSelectionIndicatorOffset(
    visible: Boolean,
    expandFrom: Alignment.Horizontal,
    shrinkTowards: Alignment.Horizontal,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            ),
            expandFrom = expandFrom,
        ),
        exit = shrinkHorizontally(
            animationSpec = tween(
                durationMillis = 150,
                easing = FastOutSlowInEasing,
            ),
            shrinkTowards = shrinkTowards,
        ),
        label = "conversationMessageSelectionIndicatorOffset",
    ) {
        Spacer(
            modifier = Modifier
                .width(width = MESSAGE_SELECTION_INDICATOR_GUTTER_WIDTH),
        )
    }
}

@Composable
private fun Transition<Boolean>.animateSelectionIndicatorContainerColor(): State<Color> {
    return animateColor(
        transitionSpec = {
            tween(
                durationMillis = 180,
                easing = FastOutSlowInEasing,
            )
        },
        label = "conversationMessageSelectionIndicatorContainerColor",
        targetValueByState = { indicatorSelected ->
            when {
                indicatorSelected -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            }
        },
    )
}

@Composable
private fun Transition<Boolean>.animateSelectionIndicatorBorderColor(): State<Color> {
    return animateColor(
        transitionSpec = {
            tween(
                durationMillis = 180,
                easing = FastOutSlowInEasing,
            )
        },
        label = "conversationMessageSelectionIndicatorBorderColor",
        targetValueByState = { indicatorSelected ->
            when {
                indicatorSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
        },
    )
}

@Composable
private fun Transition<Boolean>.animateSelectionIndicatorCheckmarkAlpha(): State<Float> {
    return animateFloat(
        transitionSpec = {
            tween(
                durationMillis = 180,
                easing = FastOutSlowInEasing,
            )
        },
        label = "conversationMessageSelectionIndicatorCheckmarkAlpha",
        targetValueByState = { indicatorSelected ->
            when {
                indicatorSelected -> 1f
                else -> 0f
            }
        },
    )
}

@Composable
private fun Transition<Boolean>.animateSelectionIndicatorCheckmarkScale(): State<Float> {
    return animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "conversationMessageSelectionIndicatorCheckmarkScale",
        targetValueByState = { indicatorSelected ->
            when {
                indicatorSelected -> 1f
                else -> 0.7f
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun ConversationMessageSelectionIndicatorPreview() {
    MessagingPreviewColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            ConversationMessageSelectionIndicator(
                visible = true,
                isSelected = false,
                expandFrom = Alignment.Start,
                shrinkTowards = Alignment.Start,
            )
            ConversationMessageSelectionIndicator(
                visible = true,
                isSelected = true,
                expandFrom = Alignment.Start,
                shrinkTowards = Alignment.Start,
            )
        }
    }
}
