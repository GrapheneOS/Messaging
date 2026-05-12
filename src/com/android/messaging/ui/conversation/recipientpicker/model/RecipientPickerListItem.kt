package com.android.messaging.ui.conversation.recipientpicker.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.contact.model.ContactDestination
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface RecipientPickerListItem {
    val id: String

    @Immutable
    data class Contact(
        val contact: com.android.messaging.data.contact.model.Contact,
    ) : RecipientPickerListItem {
        override val id: String = "contact:${contact.id}"

        val destinations: ImmutableList<ContactDestination>
            get() = contact.destinations
    }

    @Immutable
    data class SyntheticPhone(
        override val id: String,
        val rawQuery: String,
        val destination: String,
        val normalizedDestination: String,
        val secondaryText: String = normalizedDestination,
    ) : RecipientPickerListItem
}
