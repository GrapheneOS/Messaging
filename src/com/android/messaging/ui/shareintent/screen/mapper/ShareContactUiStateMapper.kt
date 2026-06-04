package com.android.messaging.ui.shareintent.screen.mapper

import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat.LTR
import com.android.messaging.data.contact.model.Contact
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ShareContactUiStateMapper {
    fun map(contacts: ImmutableList<Contact>): ImmutableList<ShareTargetUiState.Contact>
}

internal class ShareContactUiStateMapperImpl @Inject constructor() : ShareContactUiStateMapper {

    override fun map(
        contacts: ImmutableList<Contact>,
    ): ImmutableList<ShareTargetUiState.Contact> {
        return contacts
            .mapNotNull(::toContactUiState)
            .toImmutableList()
    }

    private fun toContactUiState(contact: Contact): ShareTargetUiState.Contact? {
        val destination = contact.destinations.firstOrNull() ?: return null

        val formatter = BidiFormatter.getInstance()
        val name = contact.displayName.ifBlank { destination.displayValue }
        val details = destination.displayValue.takeIf { it.isNotEmpty() && it != name }

        return ShareTargetUiState.Contact(
            contactId = contact.id,
            destination = destination.value,
            normalizedDestination = destination.normalizedValue,
            displayName = formatter.unicodeWrap(name, LTR),
            details = details?.let { formatter.unicodeWrap(it, LTR) },
            avatarUri = contact.photoUri,
        )
    }
}
