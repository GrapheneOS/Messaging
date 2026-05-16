package com.android.messaging.ui.conversation.recipientpicker.component

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.messaging.data.contact.model.ContactDestination
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerListItem
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionRowDecorators
import kotlinx.collections.immutable.ImmutableSet

private val avatarSize = 40.dp
private val destinationVerticalPadding = 10.dp

@Composable
internal fun MultiDestinationContactRow(
    item: RecipientPickerListItem.Contact,
    enabled: Boolean,
    selectedDestinations: ImmutableSet<String>,
    onDestinationClick: (destination: String) -> Unit,
    onDestinationLongClick: ((destination: String) -> Unit)?,
    shape: RoundedCornerShape,
    rowDecorators: RecipientSelectionRowDecorators,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .then(other = modifier)
            .fillMaxWidth()
            .testTag(rowDecorators.recipientRowTestTag(item))
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = shape,
            )
            .padding(
                horizontal = rowHorizontalPadding,
                vertical = rowVerticalPadding,
            ),
    ) {
        MultiDestinationContactHeader(item = item)

        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            item.destinations.forEachIndexed { index, destination ->
                val isPrevSelected = index > 0 &&
                    selectedDestinations.contains(
                        item.destinations[index - 1].normalizedValue,
                    )

                val isNextSelected = index < item.destinations.lastIndex &&
                    selectedDestinations.contains(
                        item.destinations[index + 1].normalizedValue,
                    )

                MultiDestinationMiniRow(
                    item = item,
                    destination = destination,
                    enabled = enabled,
                    isSelected = selectedDestinations.contains(destination.normalizedValue),
                    isPrevSelected = isPrevSelected,
                    isNextSelected = isNextSelected,
                    onClick = { onDestinationClick(destination.normalizedValue) },
                    onLongClick = onDestinationLongClick?.let { callback ->
                        { callback(destination.normalizedValue) }
                    },
                    rowDecorators = rowDecorators,
                )
            }
        }
    }
}

@Composable
private fun MultiDestinationContactHeader(
    item: RecipientPickerListItem.Contact,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipientSelectionContactAvatar(
            item = item,
            isSelected = false,
        )

        Text(
            modifier = Modifier
                .padding(start = avatarToTextSpacing)
                .weight(weight = 1f),
            text = item.contact.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MultiDestinationMiniRow(
    item: RecipientPickerListItem.Contact,
    destination: ContactDestination,
    enabled: Boolean,
    isSelected: Boolean,
    isPrevSelected: Boolean,
    isNextSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    rowDecorators: RecipientSelectionRowDecorators,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val selectionTransition = updateTransition(
        targetState = isSelected,
        label = "recipientSelectionContactDestinationSelection",
    )

    val containerColor by selectionTransition.animateContainerColor()
    val primaryTextColor by selectionTransition.animatePrimaryTextColor()
    val secondaryTextColor by selectionTransition.animateSecondaryTextColor()
    val highlightShape = rememberDestinationHighlightShape(
        isPrevSelected = isPrevSelected,
        isNextSelected = isNextSelected,
    )

    val label = rememberDestinationLabel(destination = destination)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Min)
            .testTag(rowDecorators.destinationRowTestTag(item, destination.value))
            .semantics { selected = isSelected }
            .background(color = containerColor, shape = highlightShape)
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
                end = 12.dp,
                top = destinationVerticalPadding,
                bottom = destinationVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DestinationCheckSlot(isSelected = isSelected)

        Spacer(modifier = Modifier.width(avatarToTextSpacing))

        MultiDestinationMiniRowContent(
            item = item,
            destination = destination,
            label = label,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            rowDecorators = rowDecorators,
        )
    }
}

@Composable
private fun RowScope.MultiDestinationMiniRowContent(
    item: RecipientPickerListItem.Contact,
    destination: ContactDestination,
    label: String,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    rowDecorators: RecipientSelectionRowDecorators,
) {
    Text(
        modifier = Modifier
            .weight(weight = 1f),
        text = destination.displayValue,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium,
        color = primaryTextColor,
    )

    Text(
        modifier = Modifier
            .padding(start = 12.dp),
        text = label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        color = secondaryTextColor,
    )

    RecipientSelectionTrailingIndicator(
        visible = rowDecorators.showRecipientTrailingIndicator(
            item,
            destination.normalizedValue,
        ),
        testTag = rowDecorators.trailingIndicatorTestTag,
    )
}

@Composable
private fun DestinationCheckSlot(
    isSelected: Boolean,
) {
    Box(
        modifier = Modifier
            .width(width = avatarSize)
            .fillMaxHeight(),
    ) {
        AnimatedVisibility(
            modifier = Modifier.matchParentSize(),
            visible = isSelected,
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                DestinationCheckBadge()
            }
        }
    }
}

@Composable
private fun DestinationCheckBadge() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(ratio = 1f)
            .sizeIn(maxWidth = avatarSize, maxHeight = avatarSize)
            .padding(all = 2.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(fraction = 0.6f),
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun rememberDestinationLabel(
    destination: ContactDestination,
): String {
    val resources = LocalResources.current

    return remember(destination.kind, destination.type, destination.customLabel) {
        val label = when (destination.kind) {
            ContactDestination.Kind.PHONE -> {
                Phone.getTypeLabel(resources, destination.type, destination.customLabel)
            }

            ContactDestination.Kind.EMAIL -> {
                Email.getTypeLabel(resources, destination.type, destination.customLabel)
            }
        }

        label?.toString().orEmpty()
    }
}

@Composable
private fun rememberDestinationHighlightShape(
    isPrevSelected: Boolean,
    isNextSelected: Boolean,
): RoundedCornerShape {
    val topCornerRadius by animateDpAsState(
        targetValue = when {
            isPrevSelected -> contactMiddleCornerRadius
            else -> contactCornerRadius
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "destinationHighlightTopCornerRadius",
    )
    val bottomCornerRadius by animateDpAsState(
        targetValue = when {
            isNextSelected -> contactMiddleCornerRadius
            else -> contactCornerRadius
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "destinationHighlightBottomCornerRadius",
    )
    return RoundedCornerShape(
        topStart = topCornerRadius,
        topEnd = topCornerRadius,
        bottomStart = bottomCornerRadius,
        bottomEnd = bottomCornerRadius,
    )
}
