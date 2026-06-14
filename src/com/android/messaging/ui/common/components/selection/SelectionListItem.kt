package com.android.messaging.ui.common.components.selection

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.common.components.optionalTestTag

@Composable
internal fun SelectionListItem(
    primaryText: String,
    secondaryText: String?,
    isSelected: Boolean,
    enabled: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    leadingContent: @Composable RowScope.() -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val hapticFeedback = LocalHapticFeedback.current
    val selectionTransition = updateTransition(
        targetState = isSelected,
        label = "selectionListItemSelection",
    )

    val colors = currentSelectionListItemColors()
    val containerColor by selectionTransition.animateSelectionContainerColor(colors)
    val primaryTextColor by selectionTransition.animateSelectionPrimaryTextColor(colors)
    val secondaryTextColor by selectionTransition.animateSelectionSecondaryTextColor(colors)

    Row(
        modifier = Modifier
            .then(other = modifier)
            .fillMaxWidth()
            .optionalTestTag(testTag)
            .semantics { selected = isSelected }
            .clip(shape = shape)
            .background(color = containerColor)
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
            .padding(
                horizontal = SelectionListItemTokens.rowHorizontalPadding,
                vertical = SelectionListItemTokens.rowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent()

        SelectionListItemText(
            primaryText = primaryText,
            secondaryText = secondaryText,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
        )

        trailingContent()
    }
}

@Composable
internal fun SelectionListTrailingIndicator(
    visible: Boolean,
    testTag: String?,
) {
    SelectionAnimatedVisibility(
        visible = visible,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(size = 20.dp)
                .optionalTestTag(testTag),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun RowScope.SelectionListItemText(
    primaryText: String,
    secondaryText: String?,
    primaryTextColor: Color,
    secondaryTextColor: Color,
) {
    Column(
        modifier = Modifier
            .padding(start = SelectionListItemTokens.avatarToTextSpacing)
            .weight(weight = 1f),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        Text(
            text = primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = primaryTextColor,
        )

        if (secondaryText != null) {
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
internal fun Transition<Boolean>.animateSelectionContainerColor(
    colors: SelectionListItemColors,
): State<Color> {
    return animateColor(
        transitionSpec = {
            tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            )
        },
        label = "selectionListItemContainerColor",
        targetValueByState = { isItemSelected ->
            when {
                isItemSelected -> colors.selectedContainerColor
                else -> colors.containerColor
            }
        },
    )
}

@Composable
internal fun Transition<Boolean>.animateSelectionPrimaryTextColor(
    colors: SelectionListItemColors,
): State<Color> {
    return animateColor(
        transitionSpec = {
            tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            )
        },
        label = "selectionListItemPrimaryTextColor",
        targetValueByState = { isItemSelected ->
            when {
                isItemSelected -> colors.selectedPrimaryTextColor
                else -> colors.primaryTextColor
            }
        },
    )
}

@Composable
internal fun Transition<Boolean>.animateSelectionSecondaryTextColor(
    colors: SelectionListItemColors,
): State<Color> {
    return animateColor(
        transitionSpec = {
            tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            )
        },
        label = "selectionListItemSecondaryTextColor",
        targetValueByState = { isItemSelected ->
            when {
                isItemSelected -> colors.selectedSecondaryTextColor
                else -> colors.secondaryTextColor
            }
        },
    )
}
