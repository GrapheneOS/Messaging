package com.android.messaging.ui.conversation.recipientpicker

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
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
import com.android.messaging.ui.conversation.recipientpicker.model.RecipientPickerListItem
import kotlinx.collections.immutable.ImmutableSet

private val contactCornerRadius = 18.dp
private val contactMiddleCornerRadius = 2.dp
private val avatarSize = 40.dp
private val avatarToTextSpacing = 14.dp
private val rowHorizontalPadding = 16.dp
private val rowVerticalPadding = 14.dp
private val destinationIndentation = avatarSize + avatarToTextSpacing
private val destinationVerticalPadding = 10.dp

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
    selectedDestinations: ImmutableSet<String>,
    onDestinationClick: (destination: String) -> Unit,
    shape: RoundedCornerShape,
    rowDecorators: RecipientSelectionRowDecorators,
    modifier: Modifier = Modifier,
    onDestinationLongClick: ((destination: String) -> Unit)? = null,
) {
    when (item) {
        is RecipientPickerListItem.Contact -> {
            ContactRow(
                modifier = modifier,
                item = item,
                enabled = enabled,
                selectedDestinations = selectedDestinations,
                onDestinationClick = onDestinationClick,
                onDestinationLongClick = onDestinationLongClick,
                shape = shape,
                rowDecorators = rowDecorators,
            )
        }

        is RecipientPickerListItem.SyntheticPhone -> {
            SyntheticPhoneRow(
                modifier = modifier,
                item = item,
                enabled = enabled,
                isSelected = selectedDestinations.contains(item.normalizedDestination),
                onClick = { onDestinationClick(item.normalizedDestination) },
                onLongClick = onDestinationLongClick?.let { callback ->
                    { callback(item.normalizedDestination) }
                },
                shape = shape,
                rowDecorators = rowDecorators,
            )
        }
    }
}

@Composable
private fun ContactRow(
    item: RecipientPickerListItem.Contact,
    enabled: Boolean,
    selectedDestinations: ImmutableSet<String>,
    onDestinationClick: (destination: String) -> Unit,
    onDestinationLongClick: ((destination: String) -> Unit)?,
    shape: RoundedCornerShape,
    rowDecorators: RecipientSelectionRowDecorators,
    modifier: Modifier = Modifier,
) {
    val destinations = item.destinations
    val isSingleDestination = destinations.size <= 1
    val singleDestination = destinations.firstOrNull()
    val isSingleSelected = singleDestination != null &&
        selectedDestinations.contains(singleDestination.normalizedValue)

    when {
        isSingleDestination && singleDestination != null -> {
            SingleDestinationContactRow(
                modifier = modifier,
                item = item,
                destination = singleDestination,
                enabled = enabled,
                isSelected = isSingleSelected,
                onClick = { onDestinationClick(singleDestination.normalizedValue) },
                onLongClick = onDestinationLongClick?.let { callback ->
                    { callback(singleDestination.normalizedValue) }
                },
                shape = shape,
                rowDecorators = rowDecorators,
            )
        }

        else -> {
            MultiDestinationContactRow(
                modifier = modifier,
                item = item,
                enabled = enabled,
                selectedDestinations = selectedDestinations,
                onDestinationClick = onDestinationClick,
                onDestinationLongClick = onDestinationLongClick,
                shape = shape,
                rowDecorators = rowDecorators,
            )
        }
    }
}

@Composable
private fun SingleDestinationContactRow(
    item: RecipientPickerListItem.Contact,
    destination: ContactDestination,
    enabled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    shape: RoundedCornerShape,
    rowDecorators: RecipientSelectionRowDecorators,
    modifier: Modifier = Modifier,
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
            .testTag(rowDecorators.recipientRowTestTag(item))
            .semantics { selected = isSelected }
            .background(color = containerColor, shape = shape)
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
            .padding(horizontal = rowHorizontalPadding, vertical = rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipientSelectionContactAvatar(
            item = item,
            isSelected = isSelected,
        )

        ContactPrimaryAndSecondaryText(
            primaryText = item.contact.displayName,
            secondaryText = destination.displayValue,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
        )

        RecipientSelectionTrailingIndicator(
            visible = rowDecorators.showRecipientTrailingIndicator(
                item,
                destination.normalizedValue,
            ),
            testTag = rowDecorators.trailingIndicatorTestTag,
        )
    }
}

@Composable
private fun SyntheticPhoneRow(
    item: RecipientPickerListItem.SyntheticPhone,
    enabled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    shape: RoundedCornerShape,
    rowDecorators: RecipientSelectionRowDecorators,
    modifier: Modifier = Modifier,
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
            .testTag(rowDecorators.recipientRowTestTag(item))
            .semantics { selected = isSelected }
            .background(color = containerColor, shape = shape)
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
            .padding(horizontal = rowHorizontalPadding, vertical = rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipientSelectionContactAvatar(
            item = item,
            isSelected = isSelected,
        )

        ContactPrimaryAndSecondaryText(
            primaryText = recipientSelectionItemDisplayName(item = item),
            secondaryText = item.secondaryText,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
        )

        RecipientSelectionTrailingIndicator(
            visible = rowDecorators.showRecipientTrailingIndicator(
                item,
                item.normalizedDestination,
            ),
            testTag = rowDecorators.trailingIndicatorTestTag,
        )
    }
}

@Composable
private fun MultiDestinationContactRow(
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
            item.destinations.forEach { destination ->
                MultiDestinationMiniRow(
                    item = item,
                    destination = destination,
                    enabled = enabled,
                    isSelected = selectedDestinations.contains(destination.normalizedValue),
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

    val label = rememberDestinationLabel(destination = destination)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(rowDecorators.destinationRowTestTag(item, destination.value))
            .semantics { selected = isSelected }
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
                start = destinationIndentation,
                end = 12.dp,
                top = destinationVerticalPadding,
                bottom = destinationVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        modifier = Modifier.weight(weight = 1f),
        text = destination.displayValue,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium,
        color = primaryTextColor,
    )

    Text(
        modifier = Modifier.padding(start = 12.dp),
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
private fun RowScope.ContactPrimaryAndSecondaryText(
    primaryText: String,
    secondaryText: String?,
    primaryTextColor: Color,
    secondaryTextColor: Color,
) {
    Column(
        modifier = Modifier
            .padding(start = avatarToTextSpacing)
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
