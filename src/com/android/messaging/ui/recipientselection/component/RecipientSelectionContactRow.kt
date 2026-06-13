package com.android.messaging.ui.recipientselection.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.common.components.selection.SelectionListItem
import com.android.messaging.ui.common.components.selection.SelectionListItemTokens
import com.android.messaging.ui.common.components.selection.SelectionListTrailingIndicator
import com.android.messaging.ui.contact.model.ContactDestinationUiModel
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionRowDecorators
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

internal val contactCornerRadius = SelectionListItemTokens.cornerRadius
internal val contactMiddleCornerRadius = SelectionListItemTokens.middleCornerRadius
internal val avatarToTextSpacing = SelectionListItemTokens.avatarToTextSpacing
internal val rowHorizontalPadding = SelectionListItemTokens.rowHorizontalPadding
internal val rowVerticalPadding = SelectionListItemTokens.rowVerticalPadding

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
    destination: ContactDestinationUiModel,
    enabled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    shape: RoundedCornerShape,
    rowDecorators: RecipientSelectionRowDecorators,
    modifier: Modifier = Modifier,
) {
    SelectionListItem(
        modifier = modifier,
        primaryText = item.contact.displayName,
        secondaryText = destination.displayValue,
        isSelected = isSelected,
        enabled = enabled,
        shape = shape,
        onClick = onClick,
        onLongClick = onLongClick,
        testTag = rowDecorators.recipientRowTestTag(item),
        leadingContent = {
            RecipientSelectionContactAvatar(
                item = item,
                isSelected = isSelected,
            )
        },
        trailingContent = {
            SelectionListTrailingIndicator(
                visible = rowDecorators.showRecipientTrailingIndicator(
                    item,
                    destination.normalizedValue,
                ),
                testTag = rowDecorators.trailingIndicatorTestTag,
            )
        },
    )
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
    SelectionListItem(
        modifier = modifier,
        primaryText = recipientSelectionItemPrimaryText(item = item),
        secondaryText = item.secondaryText,
        isSelected = isSelected,
        enabled = enabled,
        shape = shape,
        onClick = onClick,
        onLongClick = onLongClick,
        testTag = rowDecorators.recipientRowTestTag(item),
        leadingContent = {
            RecipientSelectionContactAvatar(
                item = item,
                isSelected = isSelected,
            )
        },
        trailingContent = {
            SelectionListTrailingIndicator(
                visible = rowDecorators.showRecipientTrailingIndicator(
                    item,
                    item.normalizedDestination,
                ),
                testTag = rowDecorators.trailingIndicatorTestTag,
            )
        },
    )
}

internal fun recipientSelectionContactRowShape(
    index: Int,
    totalCount: Int,
): RoundedCornerShape {
    return SelectionListItemTokens.shape(
        index = index,
        totalCount = totalCount,
    )
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactRowGroupedListPreview() {
    val items = previewRecipientSelectionContactRowGroupedItems()
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
            items.forEachIndexed { index, item ->
                PreviewRecipientSelectionContactRow(
                    item = item,
                    enabled = true,
                    selectedDestinations = persistentSetOf(
                        RECIPIENT_ROW_PREVIEW_PRIMARY_DESTINATION,
                    ),
                    shape = recipientSelectionContactRowShape(
                        index = index,
                        totalCount = items.size,
                    ),
                    loadingDestination = RECIPIENT_ROW_PREVIEW_SYNTHETIC_DESTINATION,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactRowSingleDestinationStatesPreview() {
    val phoneContactItem = previewRecipientSelectionSingleDestinationContactItem()
    val emailContactItem = previewRecipientSelectionSingleEmailDestinationContactItem()
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            PreviewRecipientSelectionContactRow(
                item = phoneContactItem,
                selectedDestinations = persistentSetOf(),
            )

            PreviewRecipientSelectionContactRow(
                item = phoneContactItem,
                selectedDestinations = persistentSetOf(
                    RECIPIENT_ROW_PREVIEW_PRIMARY_DESTINATION,
                ),
            )

            PreviewRecipientSelectionContactRow(
                item = emailContactItem,
                selectedDestinations = persistentSetOf(),
                loadingDestination = RECIPIENT_ROW_PREVIEW_EMAIL_DESTINATION,
            )

            PreviewRecipientSelectionContactRow(
                item = emailContactItem,
                enabled = false,
                selectedDestinations = persistentSetOf(
                    RECIPIENT_ROW_PREVIEW_EMAIL_DESTINATION,
                ),
                onDestinationLongClick = null,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactRowSyntheticPhoneStatesPreview() {
    val syntheticPhoneItem = previewRecipientSelectionSyntheticPhoneItem()
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            PreviewRecipientSelectionContactRow(
                item = syntheticPhoneItem,
                selectedDestinations = persistentSetOf(),
            )

            PreviewRecipientSelectionContactRow(
                item = syntheticPhoneItem,
                selectedDestinations = persistentSetOf(
                    RECIPIENT_ROW_PREVIEW_SYNTHETIC_DESTINATION,
                ),
            )

            PreviewRecipientSelectionContactRow(
                item = syntheticPhoneItem,
                selectedDestinations = persistentSetOf(
                    RECIPIENT_ROW_PREVIEW_SYNTHETIC_DESTINATION,
                ),
                loadingDestination = RECIPIENT_ROW_PREVIEW_SYNTHETIC_DESTINATION,
            )

            PreviewRecipientSelectionContactRow(
                item = syntheticPhoneItem,
                enabled = false,
                selectedDestinations = persistentSetOf(),
                onDestinationLongClick = null,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactRowMultiDestinationWrapperPreview() {
    val contactItem = previewRecipientSelectionMultiDestinationContactItem()
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            PreviewRecipientSelectionContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf(),
            )

            PreviewRecipientSelectionContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf(
                    RECIPIENT_ROW_PREVIEW_SECONDARY_DESTINATION,
                ),
            )

            PreviewRecipientSelectionContactRow(
                item = contactItem,
                selectedDestinations = persistentSetOf<String>()
                    .add(RECIPIENT_ROW_PREVIEW_PRIMARY_DESTINATION)
                    .add(RECIPIENT_ROW_PREVIEW_SECONDARY_DESTINATION)
                    .add(RECIPIENT_ROW_PREVIEW_EMAIL_DESTINATION),
                loadingDestination = RECIPIENT_ROW_PREVIEW_EMAIL_DESTINATION,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactRowLongTextPreview() {
    MessagingPreviewColumn {
        Column(
            modifier = Modifier.width(width = 320.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            PreviewRecipientSelectionContactRow(
                item = previewRecipientSelectionLongSingleDestinationContactItem(),
                selectedDestinations = persistentSetOf(
                    RECIPIENT_ROW_PREVIEW_LONG_PHONE_DESTINATION,
                ),
            )

            PreviewRecipientSelectionContactRow(
                item = previewRecipientSelectionLongMultiDestinationContactItem(),
                selectedDestinations = persistentSetOf(
                    RECIPIENT_ROW_PREVIEW_LONG_EMAIL_DESTINATION,
                ),
            )

            PreviewRecipientSelectionContactRow(
                item = previewRecipientSelectionLongSyntheticPhoneItem(),
                selectedDestinations = persistentSetOf(
                    RECIPIENT_ROW_PREVIEW_LONG_SYNTHETIC_DESTINATION,
                ),
            )
        }
    }
}
