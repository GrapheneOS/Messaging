package com.android.messaging.ui.shareintent.screen.mapper

import com.android.messaging.data.contact.model.Contact
import com.android.messaging.ui.shareintent.screen.formatter.ShareTargetTextFormatter
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ShareContactUiStateMapper {
    fun map(contacts: ImmutableList<Contact>): ImmutableList<ShareTargetUiState.Contact>
}

internal class ShareContactUiStateMapperImpl @Inject constructor(
    private val textFormatter: ShareTargetTextFormatter,
) : ShareContactUiStateMapper {

    override fun map(
        contacts: ImmutableList<Contact>,
    ): ImmutableList<ShareTargetUiState.Contact> {
        return contacts
            .mapNotNull(::toContactUiState)
            .toImmutableList()
    }

    private fun toContactUiState(contact: Contact): ShareTargetUiState.Contact? {
        val destination = contact.destinations.firstOrNull() ?: return null
        val name = contact.displayName.ifBlank { destination.displayValue }

        return ShareTargetUiState.Contact(
            contactId = contact.id,
            destination = destination.value,
            normalizedDestination = destination.normalizedValue,
            displayName = textFormatter.wrap(name),
            details = textFormatter.detailsOrNull(
                name = name,
                value = destination.displayValue,
            ),
            avatarUri = contact.photoUri,
        )
    }
}
