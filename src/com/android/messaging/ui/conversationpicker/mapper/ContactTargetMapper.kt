package com.android.messaging.ui.conversationpicker.mapper

import com.android.messaging.ui.conversationpicker.formatter.TargetTextFormatter
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import javax.inject.Inject

internal interface ContactTargetMapper {
    fun map(item: RecipientPickerListItem, destination: String): TargetUiState.Contact
}

internal class ContactTargetMapperImpl @Inject constructor(
    private val textFormatter: TargetTextFormatter,
) : ContactTargetMapper {

    override fun map(
        item: RecipientPickerListItem,
        destination: String,
    ): TargetUiState.Contact {
        return when (item) {
            is RecipientPickerListItem.Contact -> mapContact(item, destination)
            is RecipientPickerListItem.SyntheticPhone -> mapSyntheticPhone(item)
        }
    }

    private fun mapContact(
        item: RecipientPickerListItem.Contact,
        destination: String,
    ): TargetUiState.Contact {
        val matchingDestination = item.destinations.firstOrNull {
            it.normalizedValue == destination || it.value == destination
        }
        val displayDestination = matchingDestination?.displayValue ?: destination
        val name = item.contact.displayName.ifBlank { displayDestination }

        return TargetUiState.Contact(
            destination = matchingDestination?.normalizedValue ?: destination,
            normalizedDestination = matchingDestination?.normalizedValue ?: destination,
            displayName = textFormatter.wrap(name),
            details = textFormatter.detailsOrNull(
                name = name,
                value = displayDestination,
            ),
            avatarUri = item.contact.photoUri,
        )
    }

    private fun mapSyntheticPhone(
        item: RecipientPickerListItem.SyntheticPhone,
    ): TargetUiState.Contact {
        val name = item.displayName.ifBlank { item.rawQuery }

        return TargetUiState.Contact(
            destination = item.normalizedDestination,
            normalizedDestination = item.normalizedDestination,
            displayName = textFormatter.wrap(name),
            details = textFormatter.detailsOrNull(
                name = name,
                value = item.secondaryText,
            ),
            avatarUri = null,
        )
    }
}
