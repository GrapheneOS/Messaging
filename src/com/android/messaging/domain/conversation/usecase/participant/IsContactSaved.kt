package com.android.messaging.domain.conversation.usecase.participant

import javax.inject.Inject

internal fun interface IsContactSaved {
    operator fun invoke(contactId: Long, lookupKey: String?): Boolean
}

internal class IsContactSavedImpl @Inject constructor() : IsContactSaved {

    override operator fun invoke(
        contactId: Long,
        lookupKey: String?,
    ): Boolean {
        return contactId >= FIRST_CONTACTS_PROVIDER_ID && !lookupKey.isNullOrBlank()
    }
}

private const val FIRST_CONTACTS_PROVIDER_ID = 1L
