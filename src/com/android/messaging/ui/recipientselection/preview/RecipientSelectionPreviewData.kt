package com.android.messaging.ui.recipientselection.preview

import com.android.messaging.ui.contact.model.ContactDestinationUiModel
import com.android.messaging.ui.contact.model.ContactUiModel
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionPrimaryActionUiState
import kotlinx.collections.immutable.persistentListOf

internal fun previewRecipientPickerUiState(): RecipientPickerUiState {
    return RecipientPickerUiState(
        query = "Ada",
        items = persistentListOf(
            RecipientPickerListItem.Contact(contact = previewContact()),
            RecipientPickerListItem.SyntheticPhone(
                id = "synthetic:+31655550199",
                rawQuery = "+31 6 5555 0199",
                destination = "+31655550199",
                normalizedDestination = "+31655550199",
                displayName = "+31 6 5555 0199",
                secondaryText = "Mobile",
            ),
        ),
        canLoadMore = true,
        hasContactsPermission = true,
        isLoading = false,
        isLoadingMore = false,
    )
}

internal fun previewRecipientSelectionContentUiState(): RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = previewRecipientPickerUiState(),
        primaryAction = RecipientSelectionPrimaryActionUiState(
            text = "Start chat",
            isEnabled = true,
        ),
        selectedRecipients = persistentListOf(previewSelectedRecipient()),
        isQueryEnabled = true,
    )
}

internal fun previewSelectedRecipient(): SelectedRecipient {
    return SelectedRecipient(
        destination = "+31622223333",
        label = "Ada Lovelace",
        displayDestination = "+31 6 2222 3333",
        photoUri = null,
    )
}

internal fun previewContact(): ContactUiModel {
    return ContactUiModel(
        id = 1L,
        lookupKey = "preview-contact",
        displayName = "Ada Lovelace",
        photoUri = null,
        destinations = persistentListOf(
            ContactDestinationUiModel(
                dataId = 11L,
                contactId = 1L,
                value = "+31 6 2222 3333",
                normalizedValue = "+31622223333",
                displayValue = "+31 6 2222 3333",
                kind = ContactDestinationUiModel.Kind.PHONE,
                type = 2,
                customLabel = null,
                isPrimary = true,
                isSuperPrimary = true,
            ),
            ContactDestinationUiModel(
                dataId = 12L,
                contactId = 1L,
                value = "ada@example.com",
                normalizedValue = "ada@example.com",
                displayValue = "ada@example.com",
                kind = ContactDestinationUiModel.Kind.EMAIL,
                type = 1,
                customLabel = null,
                isPrimary = false,
                isSuperPrimary = false,
            ),
        ),
    )
}
