package com.android.messaging.ui.conversation.v2.recipientpicker.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient

@Immutable
internal sealed interface RecipientPickerListItem {
    val id: String
    val destination: String
    val secondaryText: String?

    @Immutable
    data class Contact(
        val recipient: ConversationRecipient,
        override val id: String = recipient.id,
        override val destination: String = recipient.destination,
        override val secondaryText: String? = recipient.secondaryText,
    ) : RecipientPickerListItem

    @Immutable
    data class SyntheticPhone(
        override val id: String,
        val rawQuery: String,
        override val destination: String,
        val normalizedDestination: String,
        override val secondaryText: String = normalizedDestination,
    ) : RecipientPickerListItem
}
