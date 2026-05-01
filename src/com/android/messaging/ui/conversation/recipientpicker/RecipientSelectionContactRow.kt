package com.android.messaging.ui.conversation.recipientpicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.recipientpicker.model.RecipientPickerListItem

private val contactCornerRadius = 18.dp
private val contactMiddleCornerRadius = 2.dp
private val topContactShape = RoundedCornerShape(
    topStart = contactCornerRadius,
    topEnd = contactCornerRadius,
    bottomStart = contactMiddleCornerRadius,
    bottomEnd = contactMiddleCornerRadius,
)
private val bottomContactShape = RoundedCornerShape(
    topStart = contactMiddleCornerRadius,
    topEnd = contactMiddleCornerRadius,
    bottomStart = contactCornerRadius,
    bottomEnd = contactCornerRadius,
)
private val middleContactShape = RoundedCornerShape(size = contactMiddleCornerRadius)
private val singleContactShape = RoundedCornerShape(size = contactCornerRadius)

@Composable
internal fun RecipientSelectionContactRow(
    item: RecipientPickerListItem,
    enabled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    rowTestTag: String,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    showTrailingIndicator: Boolean = false,
    trailingIndicatorTestTag: String? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val selectionTransition = updateTransition(
        targetState = isSelected,
        label = "recipientSelectionContactSelection",
    )

    val containerColor by selectionTransition.animateContainerColor()
    val primaryTextColor by selectionTransition.animatePrimaryTextColor()
    val secondaryTextColor by selectionTransition.animateSecondaryTextColor()

    Row(
        modifier = Modifier
            .then(other = modifier)
            .fillMaxWidth()
            .testTag(rowTestTag)
            .semantics {
                selected = isSelected
            }
            .background(
                color = containerColor,
                shape = shape,
            )
            .combinedClickable(
                enabled = enabled,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                },
                onLongClick = onLongClick?.let { callback ->
                    {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        callback()
                    }
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipientSelectionContactAvatar(
            item = item,
            isSelected = isSelected,
        )

        RecipientSelectionContactText(
            item = item,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
        )

        RecipientSelectionTrailingIndicator(
            visible = showTrailingIndicator,
            testTag = trailingIndicatorTestTag,
        )
    }
}

@Composable
private fun RowScope.RecipientSelectionContactText(
    item: RecipientPickerListItem,
    primaryTextColor: Color,
    secondaryTextColor: Color,
) {
    Column(
        modifier = Modifier
            .padding(start = 14.dp)
            .weight(weight = 1f),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        Text(
            text = recipientSelectionItemDisplayName(item = item),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = primaryTextColor,
        )

        item.secondaryText?.let { secondaryText ->
            Text(
                text = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor,
            )
        }
    }
}

@Composable
private fun RecipientSelectionTrailingIndicator(
    visible: Boolean,
    testTag: String?,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 200),
        ) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialScale = 0.8f,
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 150),
        ) + scaleOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            targetScale = 0.8f,
        ),
    ) {
        CircularProgressIndicator(
            modifier = when {
                testTag != null -> {
                    Modifier
                        .size(size = 20.dp)
                        .testTag(testTag)
                }

                else -> {
                    Modifier.size(size = 20.dp)
                }
            },
            strokeWidth = 2.dp,
        )
    }
}

internal fun recipientSelectionContactRowShape(
    index: Int,
    totalCount: Int,
): RoundedCornerShape {
    return when {
        totalCount <= 1 -> singleContactShape
        index == 0 -> topContactShape
        index == totalCount - 1 -> bottomContactShape
        else -> middleContactShape
    }
}

@Composable
private fun Transition<Boolean>.animateContainerColor(): State<Color> {
    return animateColor(
        transitionSpec = {
            tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            )
        },
        label = "recipientSelectionContactContainerColor",
        targetValueByState = { isContactSelected ->
            when {
                isContactSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.background
            }
        },
    )
}

@Composable
private fun Transition<Boolean>.animatePrimaryTextColor(): State<Color> {
    return animateColor(
        transitionSpec = {
            tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            )
        },
        label = "recipientSelectionContactPrimaryTextColor",
        targetValueByState = { isContactSelected ->
            when {
                isContactSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
        },
    )
}

@Composable
private fun Transition<Boolean>.animateSecondaryTextColor(): State<Color> {
    return animateColor(
        transitionSpec = {
            tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            )
        },
        label = "recipientSelectionContactSecondaryTextColor",
        targetValueByState = { isContactSelected ->
            when {
                isContactSelected -> {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                }

                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        },
    )
}
