package com.android.messaging.ui.recipientselection.component

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.android.messaging.ui.contact.model.ContactDestinationUiModel
import com.android.messaging.ui.contact.model.ContactUiModel
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionRowDecorators
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf

internal const val RECIPIENT_ROW_PREVIEW_PRIMARY_DESTINATION = "+31622223333"
internal const val RECIPIENT_ROW_PREVIEW_SECONDARY_DESTINATION = "+31644445555"
internal const val RECIPIENT_ROW_PREVIEW_EMAIL_DESTINATION = "ada@example.com"
internal const val RECIPIENT_ROW_PREVIEW_SYNTHETIC_DESTINATION = "+31655550199"
internal const val RECIPIENT_ROW_PREVIEW_LONG_PHONE_DESTINATION = "+1 415 555 0198 ext. 4827"
internal const val RECIPIENT_ROW_PREVIEW_LONG_EMAIL_DESTINATION =
    "alexandria.montgomery-washington@international-partnerships.example"
internal const val RECIPIENT_ROW_PREVIEW_LONG_SYNTHETIC_DESTINATION = "+141555501984827"

private const val PREVIEW_LONG_CONTACT_NAME =
    "Alexandria Cassandra Montgomery-Washington from International Partnerships"
private const val PREVIEW_LONG_SYNTHETIC_QUERY =
    "+1 (415) 555-0198 extension 4827 for international routing desk"
private const val PREVIEW_LONG_SYNTHETIC_SECONDARY_TEXT =
    "Possible mobile number from pasted search text with country code and extension"

@Composable
internal fun PreviewRecipientSelectionContactRow(
    item: RecipientPickerListItem,
    selectedDestinations: ImmutableSet<String>,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(size = contactCornerRadius),
    loadingDestination: String? = null,
    onDestinationLongClick: ((destination: String) -> Unit)? = { _ -> },
) {
    RecipientSelectionContactRow(
        item = item,
        enabled = enabled,
        selectedDestinations = selectedDestinations,
        onDestinationClick = { _ -> },
        shape = shape,
        rowDecorators = previewRecipientSelectionRowDecorators(
            loadingDestination = loadingDestination,
        ),
        onDestinationLongClick = onDestinationLongClick,
    )
}

internal fun previewRecipientSelectionContactRowGroupedItems():
    ImmutableList<RecipientPickerListItem> {
    return persistentListOf<RecipientPickerListItem>()
        .add(previewRecipientSelectionSingleDestinationContactItem())
        .add(previewRecipientSelectionSyntheticPhoneItem())
        .add(previewRecipientSelectionMultiDestinationContactItem())
        .add(previewRecipientSelectionEmptyDestinationContactItem())
}

internal fun previewRecipientSelectionSingleDestinationContactItem(
    contactId: Long = 1L,
    destination: String = RECIPIENT_ROW_PREVIEW_PRIMARY_DESTINATION,
    displayDestination: String = "+31 6 2222 3333",
    kind: ContactDestinationUiModel.Kind = ContactDestinationUiModel.Kind.PHONE,
    type: Int = Phone.TYPE_MOBILE,
    displayName: String = "Ada Lovelace",
): RecipientPickerListItem.Contact {
    return RecipientPickerListItem.Contact(
        contact = ContactUiModel(
            id = contactId,
            lookupKey = "preview-contact-$contactId",
            displayName = displayName,
            photoUri = null,
            destinations = persistentListOf(
                previewContactDestination(
                    contactId = contactId,
                    dataId = contactId * 10L,
                    value = destination,
                    displayValue = displayDestination,
                    kind = kind,
                    type = type,
                    isPrimary = true,
                    isSuperPrimary = true,
                ),
            ),
        ),
    )
}

internal fun previewRecipientSelectionSingleEmailDestinationContactItem():
    RecipientPickerListItem.Contact {
    return previewRecipientSelectionSingleDestinationContactItem(
        contactId = 2L,
        destination = RECIPIENT_ROW_PREVIEW_EMAIL_DESTINATION,
        displayDestination = RECIPIENT_ROW_PREVIEW_EMAIL_DESTINATION,
        kind = ContactDestinationUiModel.Kind.EMAIL,
        type = Email.TYPE_HOME,
    )
}

internal fun previewRecipientSelectionMultiDestinationContactItem():
    RecipientPickerListItem.Contact {
    return RecipientPickerListItem.Contact(
        contact = ContactUiModel(
            id = 3L,
            lookupKey = "preview-contact-multi",
            displayName = "Ada Lovelace",
            photoUri = null,
            destinations = persistentListOf<ContactDestinationUiModel>()
                .add(
                    previewContactDestination(
                        contactId = 3L,
                        dataId = 31L,
                        value = RECIPIENT_ROW_PREVIEW_PRIMARY_DESTINATION,
                        displayValue = "+31 6 2222 3333",
                        kind = ContactDestinationUiModel.Kind.PHONE,
                        type = Phone.TYPE_MOBILE,
                        isPrimary = true,
                        isSuperPrimary = true,
                    ),
                )
                .add(
                    previewContactDestination(
                        contactId = 3L,
                        dataId = 32L,
                        value = RECIPIENT_ROW_PREVIEW_SECONDARY_DESTINATION,
                        displayValue = "+31 6 4444 5555",
                        kind = ContactDestinationUiModel.Kind.PHONE,
                        type = Phone.TYPE_WORK,
                    ),
                )
                .add(
                    previewContactDestination(
                        contactId = 3L,
                        dataId = 33L,
                        value = RECIPIENT_ROW_PREVIEW_EMAIL_DESTINATION,
                        displayValue = RECIPIENT_ROW_PREVIEW_EMAIL_DESTINATION,
                        kind = ContactDestinationUiModel.Kind.EMAIL,
                        type = Email.TYPE_HOME,
                    ),
                ),
        ),
    )
}

internal fun previewRecipientSelectionLongSingleDestinationContactItem():
    RecipientPickerListItem.Contact {
    return previewRecipientSelectionSingleDestinationContactItem(
        contactId = 4L,
        destination = RECIPIENT_ROW_PREVIEW_LONG_PHONE_DESTINATION,
        displayDestination = RECIPIENT_ROW_PREVIEW_LONG_PHONE_DESTINATION,
        kind = ContactDestinationUiModel.Kind.PHONE,
        type = Phone.TYPE_CUSTOM,
        displayName = PREVIEW_LONG_CONTACT_NAME,
    )
}

internal fun previewRecipientSelectionLongMultiDestinationContactItem():
    RecipientPickerListItem.Contact {
    return RecipientPickerListItem.Contact(
        contact = ContactUiModel(
            id = 5L,
            lookupKey = "preview-contact-long-multi",
            displayName = PREVIEW_LONG_CONTACT_NAME,
            photoUri = null,
            destinations = persistentListOf<ContactDestinationUiModel>()
                .add(
                    previewContactDestination(
                        contactId = 5L,
                        dataId = 51L,
                        value = RECIPIENT_ROW_PREVIEW_LONG_PHONE_DESTINATION,
                        displayValue = RECIPIENT_ROW_PREVIEW_LONG_PHONE_DESTINATION,
                        kind = ContactDestinationUiModel.Kind.PHONE,
                        type = Phone.TYPE_CUSTOM,
                        customLabel = "Emergency escalation mobile",
                    ),
                )
                .add(
                    previewContactDestination(
                        contactId = 5L,
                        dataId = 52L,
                        value = RECIPIENT_ROW_PREVIEW_LONG_EMAIL_DESTINATION,
                        displayValue = RECIPIENT_ROW_PREVIEW_LONG_EMAIL_DESTINATION,
                        kind = ContactDestinationUiModel.Kind.EMAIL,
                        type = Email.TYPE_CUSTOM,
                        customLabel = "International partnerships shared mailbox",
                    ),
                ),
        ),
    )
}

internal fun previewRecipientSelectionEmptyDestinationContactItem():
    RecipientPickerListItem.Contact {
    return RecipientPickerListItem.Contact(
        contact = ContactUiModel(
            id = 6L,
            lookupKey = "preview-contact-empty",
            displayName = "No Destination Contact",
            photoUri = null,
            destinations = persistentListOf(),
        ),
    )
}

internal fun previewRecipientSelectionSyntheticPhoneItem(): RecipientPickerListItem.SyntheticPhone {
    return RecipientPickerListItem.SyntheticPhone(
        id = "synthetic:${RECIPIENT_ROW_PREVIEW_SYNTHETIC_DESTINATION}",
        rawQuery = "+31 6 5555 0199",
        destination = RECIPIENT_ROW_PREVIEW_SYNTHETIC_DESTINATION,
        normalizedDestination = RECIPIENT_ROW_PREVIEW_SYNTHETIC_DESTINATION,
        displayName = "+31 6 5555 0199",
        secondaryText = "Mobile",
    )
}

internal fun previewRecipientSelectionLongSyntheticPhoneItem():
    RecipientPickerListItem.SyntheticPhone {
    return RecipientPickerListItem.SyntheticPhone(
        id = "synthetic:${RECIPIENT_ROW_PREVIEW_LONG_SYNTHETIC_DESTINATION}",
        rawQuery = PREVIEW_LONG_SYNTHETIC_QUERY,
        destination = RECIPIENT_ROW_PREVIEW_LONG_SYNTHETIC_DESTINATION,
        normalizedDestination = RECIPIENT_ROW_PREVIEW_LONG_SYNTHETIC_DESTINATION,
        displayName = PREVIEW_LONG_SYNTHETIC_QUERY,
        secondaryText = PREVIEW_LONG_SYNTHETIC_SECONDARY_TEXT,
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

private fun previewContactDestination(
    contactId: Long,
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
        contactId = contactId,
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
