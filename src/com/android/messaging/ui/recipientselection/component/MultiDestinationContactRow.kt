package com.android.messaging.ui.recipientselection.component

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.common.components.selection.SelectionAnimatedVisibility
import com.android.messaging.ui.common.components.selection.SelectionListTrailingIndicator
import com.android.messaging.ui.common.components.selection.animateSelectionContainerColor
import com.android.messaging.ui.common.components.selection.animateSelectionPrimaryTextColor
import com.android.messaging.ui.common.components.selection.animateSelectionSecondaryTextColor
import com.android.messaging.ui.contact.model.ContactDestinationUiModel
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionRowDecorators
import com.android.messaging.ui.recipientselection.preview.previewRecipientPickerUiState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

private const val PREVIEW_MOBILE_DESTINATION = "+31622223333"
private const val PREVIEW_WORK_DESTINATION = "+31644445555"
private const val PREVIEW_HOME_DESTINATION = "+31677778888"
private const val PREVIEW_EMAIL_DESTINATION = "ada@example.com"
private const val PREVIEW_LONG_CONTACT_NAME =
    "Alexandria Cassandra Montgomery-Washington from International Partnerships"
private const val PREVIEW_LONG_PHONE_DESTINATION = "+1 415 555 0198 ext. 4827"
private const val PREVIEW_LONG_EMAIL_DESTINATION =
    "alexandria.montgomery-washington@international-partnerships.example"
private const val PREVIEW_LONG_LOCATION_DESTINATION =
    "northwest-visitor-center-routing-desk@example.com"

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
    destination: ContactDestinationUiModel,
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

    val containerColor by selectionTransition.animateSelectionContainerColor()
    val primaryTextColor by selectionTransition.animateSelectionPrimaryTextColor()
    val secondaryTextColor by selectionTransition.animateSelectionSecondaryTextColor()
    val highlightShape = rememberDestinationHighlightShape(
        isSelected = isSelected,
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
            .clip(shape = highlightShape)
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
    destination: ContactDestinationUiModel,
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

    SelectionListTrailingIndicator(
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
        SelectionAnimatedVisibility(
            modifier = Modifier.matchParentSize(),
            visible = isSelected,
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
    destination: ContactDestinationUiModel,
): String {
    val resources = LocalResources.current

    return remember(destination.kind, destination.type, destination.customLabel) {
        val label = when (destination.kind) {
            ContactDestinationUiModel.Kind.PHONE -> {
                Phone.getTypeLabel(resources, destination.type, destination.customLabel)
            }

            ContactDestinationUiModel.Kind.EMAIL -> {
                Email.getTypeLabel(resources, destination.type, destination.customLabel)
            }
        }

        label?.toString().orEmpty()
    }
}

@Composable
private fun rememberDestinationHighlightShape(
    isSelected: Boolean,
    isPrevSelected: Boolean,
    isNextSelected: Boolean,
): RoundedCornerShape {
    val topCornerRadius by animateDpAsState(
        targetValue = when {
            isSelected && isPrevSelected -> contactMiddleCornerRadius
            else -> contactCornerRadius
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "destinationHighlightTopCornerRadius",
    )
    val bottomCornerRadius by animateDpAsState(
        targetValue = when {
            isSelected && isNextSelected -> contactMiddleCornerRadius
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

@PreviewLightDark
@Composable
private fun MultiDestinationContactRowSelectionStatesPreview() {
    val contactItem = previewMultiDestinationContactItem()
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            PreviewMultiDestinationContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf(),
            )

            PreviewMultiDestinationContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf(PREVIEW_WORK_DESTINATION),
            )

            PreviewMultiDestinationContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf<String>()
                    .add(PREVIEW_MOBILE_DESTINATION)
                    .add(PREVIEW_WORK_DESTINATION)
                    .add(PREVIEW_HOME_DESTINATION),
            )

            PreviewMultiDestinationContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf<String>()
                    .add(PREVIEW_MOBILE_DESTINATION)
                    .add(PREVIEW_WORK_DESTINATION)
                    .add(PREVIEW_HOME_DESTINATION)
                    .add(PREVIEW_EMAIL_DESTINATION),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MultiDestinationContactRowContentStatesPreview() {
    val contactItem = previewLongMultiDestinationContactItem()
    MessagingPreviewColumn {
        Column(
            modifier = Modifier.width(width = 320.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            PreviewMultiDestinationContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf(PREVIEW_LONG_PHONE_DESTINATION),
            )

            PreviewMultiDestinationContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf<String>()
                    .add(PREVIEW_LONG_EMAIL_DESTINATION)
                    .add(PREVIEW_LONG_LOCATION_DESTINATION),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MultiDestinationContactRowInteractionStatesPreview() {
    val contactItem = previewMultiDestinationContactItem()
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            PreviewMultiDestinationContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf(),
                enabled = false,
                onDestinationLongClick = null,
            )

            PreviewMultiDestinationContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf(PREVIEW_MOBILE_DESTINATION),
                enabled = false,
                loadingDestination = PREVIEW_EMAIL_DESTINATION,
                onDestinationLongClick = null,
            )

            PreviewMultiDestinationContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf(PREVIEW_EMAIL_DESTINATION),
                loadingDestination = PREVIEW_EMAIL_DESTINATION,
            )
        }
    }
}

@Composable
private fun PreviewMultiDestinationContactRow(
    item: RecipientPickerListItem.Contact,
    selectedDestinations: ImmutableSet<String>,
    enabled: Boolean = true,
    loadingDestination: String? = null,
    onDestinationLongClick: ((destination: String) -> Unit)? = { _ -> },
) {
    MultiDestinationContactRow(
        item = item,
        enabled = enabled,
        selectedDestinations = selectedDestinations,
        onDestinationClick = { _ -> },
        onDestinationLongClick = onDestinationLongClick,
        shape = RoundedCornerShape(size = 20.dp),
        rowDecorators = previewRecipientSelectionRowDecorators(
            loadingDestination = loadingDestination,
        ),
    )
}

private fun previewRecipientSelectionRowDecorators(
    loadingDestination: String?,
): RecipientSelectionRowDecorators {
    return RecipientSelectionRowDecorators(
        recipientRowTestTag = { item -> item.id },
        destinationRowTestTag = { item, destination -> "${item.id}:$destination" },
        showRecipientTrailingIndicator = { _, destination ->
            destination == loadingDestination
        },
    )
}

private fun previewMultiDestinationContactItem(): RecipientPickerListItem.Contact {
    val baseContact = previewRecipientPickerUiState()
        .items
        .filterIsInstance<RecipientPickerListItem.Contact>()
        .first()
        .contact

    return RecipientPickerListItem.Contact(
        contact = baseContact.copy(
            destinations = persistentListOf<ContactDestinationUiModel>()
                .add(
                    previewContactDestination(
                        dataId = 11L,
                        value = PREVIEW_MOBILE_DESTINATION,
                        displayValue = "+31 6 2222 3333",
                        kind = ContactDestinationUiModel.Kind.PHONE,
                        type = Phone.TYPE_MOBILE,
                        isPrimary = true,
                        isSuperPrimary = true,
                    ),
                )
                .add(
                    previewContactDestination(
                        dataId = 12L,
                        value = PREVIEW_WORK_DESTINATION,
                        displayValue = "+31 6 4444 5555",
                        kind = ContactDestinationUiModel.Kind.PHONE,
                        type = Phone.TYPE_WORK,
                    ),
                )
                .add(
                    previewContactDestination(
                        dataId = 13L,
                        value = PREVIEW_HOME_DESTINATION,
                        displayValue = "+31 6 7777 8888",
                        kind = ContactDestinationUiModel.Kind.PHONE,
                        type = Phone.TYPE_HOME,
                    ),
                )
                .add(
                    previewContactDestination(
                        dataId = 14L,
                        value = PREVIEW_EMAIL_DESTINATION,
                        displayValue = PREVIEW_EMAIL_DESTINATION,
                        kind = ContactDestinationUiModel.Kind.EMAIL,
                        type = Email.TYPE_HOME,
                    ),
                ),
        ),
    )
}

private fun previewLongMultiDestinationContactItem(): RecipientPickerListItem.Contact {
    val baseContact = previewRecipientPickerUiState()
        .items
        .filterIsInstance<RecipientPickerListItem.Contact>()
        .first()
        .contact

    return RecipientPickerListItem.Contact(
        contact = baseContact.copy(
            displayName = PREVIEW_LONG_CONTACT_NAME,
            destinations = persistentListOf<ContactDestinationUiModel>()
                .add(
                    previewContactDestination(
                        dataId = 21L,
                        value = PREVIEW_LONG_PHONE_DESTINATION,
                        displayValue = PREVIEW_LONG_PHONE_DESTINATION,
                        kind = ContactDestinationUiModel.Kind.PHONE,
                        type = Phone.TYPE_CUSTOM,
                        customLabel = "Emergency escalation mobile",
                    ),
                )
                .add(
                    previewContactDestination(
                        dataId = 22L,
                        value = PREVIEW_LONG_EMAIL_DESTINATION,
                        displayValue = PREVIEW_LONG_EMAIL_DESTINATION,
                        kind = ContactDestinationUiModel.Kind.EMAIL,
                        type = Email.TYPE_WORK,
                    ),
                )
                .add(
                    previewContactDestination(
                        dataId = 23L,
                        value = PREVIEW_LONG_LOCATION_DESTINATION,
                        displayValue = PREVIEW_LONG_LOCATION_DESTINATION,
                        kind = ContactDestinationUiModel.Kind.EMAIL,
                        type = Email.TYPE_CUSTOM,
                        customLabel = "Visitor center routing desk",
                    ),
                ),
        ),
    )
}

private fun previewContactDestination(
    dataId: Long,
    value: String,
    displayValue: String,
    kind: ContactDestinationUiModel.Kind,
    type: Int,
    customLabel: String? = null,
    isPrimary: Boolean = false,
    isSuperPrimary: Boolean = false,
): ContactDestinationUiModel {
    return ContactDestinationUiModel(
        dataId = dataId,
        contactId = 1L,
        value = value,
        normalizedValue = value,
        displayValue = displayValue,
        kind = kind,
        type = type,
        customLabel = customLabel,
        isPrimary = isPrimary,
        isSuperPrimary = isSuperPrimary,
    )
}
