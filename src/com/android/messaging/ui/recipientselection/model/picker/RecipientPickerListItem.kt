package com.android.messaging.ui.recipientselection.model.picker

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.contact.model.ContactDestinationUiModel
import com.android.messaging.ui.contact.model.ContactUiModel
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface RecipientPickerListItem {
    val id: String

    @Immutable
    data class Contact(
        val contact: ContactUiModel,
    ) : RecipientPickerListItem {
        override val id: String = "contact:${contact.id}"

        val destinations: ImmutableList<ContactDestinationUiModel>
            get() = contact.destinations
    }

    @Immutable
    data class SyntheticPhone(
        override val id: String,
        val rawQuery: String,
        val destination: String,
        val normalizedDestination: String,
        val displayName: String = rawQuery,
        val secondaryText: String = normalizedDestination,
    ) : RecipientPickerListItem
}
