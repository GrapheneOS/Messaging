package com.android.messaging.domain.conversation.usecase.participant

import javax.inject.Inject

internal fun interface CanShowOrAddContact {
    operator fun invoke(
        isGroup: Boolean,
        contactId: Long,
        lookupKey: String?,
        destination: String?,
    ): Boolean
}

internal class CanShowOrAddContactImpl @Inject constructor(
    private val canAddContact: CanAddContact,
    private val isContactSaved: IsContactSaved,
) : CanShowOrAddContact {

    override operator fun invoke(
        isGroup: Boolean,
        contactId: Long,
        lookupKey: String?,
        destination: String?,
    ): Boolean {
        val isContactSaved = isContactSaved(
            contactId = contactId,
            lookupKey = lookupKey,
        )
        val canAddContact = canAddContact(
            isGroup = isGroup,
            lookupKey = lookupKey,
            destination = destination,
        )

        return !isGroup && (isContactSaved || canAddContact)
    }
}
