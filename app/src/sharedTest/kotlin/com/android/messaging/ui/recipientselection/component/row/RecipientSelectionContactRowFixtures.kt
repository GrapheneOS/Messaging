package com.android.messaging.ui.recipientselection.component.row

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.android.messaging.ui.contact.model.ContactDestinationUiModel
import com.android.messaging.ui.contact.model.ContactUiModel
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import kotlinx.collections.immutable.persistentListOf

internal const val CONTACT_DISPLAY_NAME = "Ada Lovelace"
internal const val CONTACT_ID = 42L
internal const val EMPTY_CONTACT_DISPLAY_NAME = "No Destination"
internal const val HOME_PHONE_DESTINATION = "+1 555 0300"
internal const val HOME_PHONE_NORMALIZED_DESTINATION = "+15550300"
internal const val MOBILE_DESTINATION = "+1 555 0100"
internal const val MOBILE_NORMALIZED_DESTINATION = "+15550100"
internal const val RECIPIENT_DESTINATION_ROW_TEST_TAG_PREFIX = "recipient_destination_row_"
internal const val RECIPIENT_ROW_TEST_TAG_PREFIX = "recipient_row_"
internal const val SYNTHETIC_PHONE_DESTINATION = "+1 555 7777"
internal const val SYNTHETIC_PHONE_NORMALIZED_DESTINATION = "+15557777"
internal const val TRAILING_INDICATOR_TEST_TAG = "recipient_trailing_indicator"
internal const val WORK_EMAIL_DESTINATION = "ada@example.com"
internal const val WORK_EMAIL_NORMALIZED_DESTINATION = "ada@example.com"

internal fun multiDestinationContactItem(): RecipientPickerListItem.Contact {
    return RecipientPickerListItem.Contact(
        contact = ContactUiModel(
            id = CONTACT_ID,
            lookupKey = "lookup-$CONTACT_ID",
            displayName = CONTACT_DISPLAY_NAME,
            photoUri = null,
            destinations = persistentListOf(
                mobileDestination(),
                workEmailDestination(),
                homePhoneDestination(),
            ),
        ),
    )
}

internal fun contactItem(
    id: Long = CONTACT_ID,
    displayName: String = CONTACT_DISPLAY_NAME,
    destination: String = MOBILE_DESTINATION,
    normalizedDestination: String = MOBILE_NORMALIZED_DESTINATION,
    dataId: Long = id,
    kind: ContactDestinationUiModel.Kind = ContactDestinationUiModel.Kind.PHONE,
    type: Int = Phone.TYPE_MOBILE,
): RecipientPickerListItem.Contact {
    return RecipientPickerListItem.Contact(
        contact = ContactUiModel(
            id = id,
            lookupKey = "lookup-$id",
            displayName = displayName,
            photoUri = null,
            destinations = persistentListOf(
                ContactDestinationUiModel(
                    dataId = dataId,
                    contactId = id,
                    value = destination,
                    normalizedValue = normalizedDestination,
                    displayValue = destination,
                    kind = kind,
                    type = type,
                    customLabel = null,
                    isPrimary = true,
                    isSuperPrimary = true,
                ),
            ),
        ),
    )
}

internal fun singleDestinationContactItem(): RecipientPickerListItem.Contact {
    return contactItem()
}

internal fun contactWithoutDestinations(): RecipientPickerListItem.Contact {
    return RecipientPickerListItem.Contact(
        contact = ContactUiModel(
            id = CONTACT_ID,
            lookupKey = "lookup-$CONTACT_ID",
            displayName = EMPTY_CONTACT_DISPLAY_NAME,
            photoUri = null,
            destinations = persistentListOf(),
        ),
    )
}

internal fun syntheticPhoneItem(): RecipientPickerListItem.SyntheticPhone {
    return RecipientPickerListItem.SyntheticPhone(
        id = "synthetic:$SYNTHETIC_PHONE_NORMALIZED_DESTINATION",
        rawQuery = SYNTHETIC_PHONE_DESTINATION,
        destination = SYNTHETIC_PHONE_DESTINATION,
        normalizedDestination = SYNTHETIC_PHONE_NORMALIZED_DESTINATION,
    )
}

internal fun selectedRecipient(
    destination: String = MOBILE_NORMALIZED_DESTINATION,
    label: String = CONTACT_DISPLAY_NAME,
    displayDestination: String = MOBILE_DESTINATION,
): SelectedRecipient {
    return SelectedRecipient(
        destination = destination,
        label = label,
        displayDestination = displayDestination,
        photoUri = null,
    )
}

internal fun mobileDestination(): ContactDestinationUiModel {
    return ContactDestinationUiModel(
        dataId = 101L,
        contactId = CONTACT_ID,
        value = MOBILE_DESTINATION,
        normalizedValue = MOBILE_NORMALIZED_DESTINATION,
        displayValue = MOBILE_DESTINATION,
        kind = ContactDestinationUiModel.Kind.PHONE,
        type = Phone.TYPE_MOBILE,
        customLabel = null,
        isPrimary = true,
        isSuperPrimary = true,
    )
}

internal fun workEmailDestination(): ContactDestinationUiModel {
    return ContactDestinationUiModel(
        dataId = 102L,
        contactId = CONTACT_ID,
        value = WORK_EMAIL_DESTINATION,
        normalizedValue = WORK_EMAIL_NORMALIZED_DESTINATION,
        displayValue = WORK_EMAIL_DESTINATION,
        kind = ContactDestinationUiModel.Kind.EMAIL,
        type = Email.TYPE_WORK,
        customLabel = null,
        isPrimary = false,
        isSuperPrimary = false,
    )
}

internal fun homePhoneDestination(): ContactDestinationUiModel {
    return ContactDestinationUiModel(
        dataId = 103L,
        contactId = CONTACT_ID,
        value = HOME_PHONE_DESTINATION,
        normalizedValue = HOME_PHONE_NORMALIZED_DESTINATION,
        displayValue = HOME_PHONE_DESTINATION,
        kind = ContactDestinationUiModel.Kind.PHONE,
        type = Phone.TYPE_HOME,
        customLabel = null,
        isPrimary = false,
        isSuperPrimary = false,
    )
}

internal fun recipientRowTestTag(item: RecipientPickerListItem): String {
    return "$RECIPIENT_ROW_TEST_TAG_PREFIX${item.id}"
}

internal fun destinationRowTestTag(
    item: RecipientPickerListItem,
    destination: String,
): String {
    return "$RECIPIENT_DESTINATION_ROW_TEST_TAG_PREFIX${item.id}_$destination"
}
