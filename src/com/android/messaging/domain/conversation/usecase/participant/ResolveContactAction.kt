package com.android.messaging.domain.conversation.usecase.participant

import com.android.messaging.domain.conversation.usecase.participant.model.ResolveContactActionResult
import javax.inject.Inject

internal fun interface ResolveContactAction {
    operator fun invoke(
        contactId: Long,
        lookupKey: String?,
        destination: String?,
    ): ResolveContactActionResult
}

internal class ResolveContactActionImpl @Inject constructor(
    private val isContactSaved: IsContactSaved,
    private val canAddContact: CanAddContact,
) : ResolveContactAction {

    override operator fun invoke(
        contactId: Long,
        lookupKey: String?,
        destination: String?,
    ): ResolveContactActionResult {
        val isSaved = isContactSaved(
            contactId = contactId,
            lookupKey = lookupKey,
        )
        val canAdd = canAddContact(
            isGroup = false,
            lookupKey = lookupKey,
            destination = destination,
        )

        return when {
            isSaved && lookupKey != null -> {
                ResolveContactActionResult.ShowContactCard(
                    contactId = contactId,
                    lookupKey = lookupKey,
                )
            }

            canAdd && destination != null -> {
                ResolveContactActionResult.AddContact(
                    destination = destination,
                )
            }

            else -> ResolveContactActionResult.Unavailable
        }
    }
}
