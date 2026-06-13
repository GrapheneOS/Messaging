package com.android.messaging.ui.recipientselection.model.picker

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
internal data class SelectedRecipient(
    val destination: String,
    val label: String,
    val displayDestination: String,
    val photoUri: String?,
) : Parcelable

internal fun SelectedRecipient.sanitizedOrNull(): SelectedRecipient? {
    val trimmedDestination = destination.trim()

    return when {
        trimmedDestination.isEmpty() -> null
        trimmedDestination == destination -> this
        else -> {
            copy(destination = trimmedDestination)
        }
    }
}

internal fun RecipientPickerListItem.toSelectedRecipient(destination: String): SelectedRecipient? {
    val trimmedDestination = destination.trim()

    if (trimmedDestination.isEmpty()) {
        return null
    }

    return when (this) {
        is RecipientPickerListItem.Contact -> {
            toSelectedContactRecipient(destination = trimmedDestination)
        }

        is RecipientPickerListItem.SyntheticPhone -> {
            toSelectedSyntheticRecipient(destination = trimmedDestination)
        }
    }
}

private fun RecipientPickerListItem.Contact.toSelectedContactRecipient(
    destination: String,
): SelectedRecipient {
    val matchingDestination = destinations.firstOrNull { contactDestination ->
        contactDestination.normalizedValue == destination ||
            contactDestination.value == destination
    }

    val selectedDestination = matchingDestination?.normalizedValue ?: destination
    val displayDestination = matchingDestination?.displayValue ?: destination
    val label = contact.displayName.ifBlank { displayDestination }

    return SelectedRecipient(
        destination = selectedDestination,
        label = label,
        displayDestination = displayDestination,
        photoUri = contact.photoUri,
    )
}

private fun RecipientPickerListItem.SyntheticPhone.toSelectedSyntheticRecipient(
    destination: String,
): SelectedRecipient {
    return SelectedRecipient(
        destination = destination,
        label = rawQuery,
        displayDestination = secondaryText,
        photoUri = null,
    )
}
