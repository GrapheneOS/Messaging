package com.android.messaging.ui.recipientselection.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.participant.participantAvatarLabel
import com.android.messaging.ui.common.components.participant.participantColorSeed
import com.android.messaging.ui.common.components.selection.SelectionListAvatar
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.preview.previewRecipientPickerUiState

@Composable
internal fun RecipientSelectionContactAvatar(
    item: RecipientPickerListItem,
    isSelected: Boolean,
) {
    val displayName = recipientSelectionItemPrimaryText(item = item)

    SelectionListAvatar(
        avatarUri = recipientSelectionPhotoUri(item = item),
        fallbackLabel = participantAvatarLabel(source = displayName),
        colorSeedCode = participantColorSeed(
            normalizedDestination = recipientSelectionNormalizedDestination(item = item),
        ),
        isSelected = isSelected,
    )
}

@Composable
internal fun recipientSelectionItemPrimaryText(
    item: RecipientPickerListItem,
): String {
    return when (item) {
        is RecipientPickerListItem.Contact -> item.contact.displayName
        is RecipientPickerListItem.SyntheticPhone -> {
            stringResource(
                id = R.string.contact_list_send_to_text,
                item.displayName,
            )
        }
    }
}

private fun recipientSelectionPhotoUri(item: RecipientPickerListItem): String? {
    return when (item) {
        is RecipientPickerListItem.Contact -> item.contact.photoUri
        is RecipientPickerListItem.SyntheticPhone -> null
    }
}

private fun recipientSelectionNormalizedDestination(item: RecipientPickerListItem): String? {
    return when (item) {
        is RecipientPickerListItem.Contact -> {
            item.contact.destinations.firstOrNull()?.normalizedValue
        }

        is RecipientPickerListItem.SyntheticPhone -> item.normalizedDestination
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactAvatarPreview() {
    MessagingPreviewColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            previewRecipientPickerUiState().items.forEach { item ->
                RecipientSelectionContactAvatar(
                    item = item,
                    isSelected = false,
                )
                RecipientSelectionContactAvatar(
                    item = item,
                    isSelected = true,
                )
            }
        }
    }
}
