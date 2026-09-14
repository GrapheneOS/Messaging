package com.android.messaging.ui.blockedparticipants.screen.model

import com.android.messaging.ui.contact.model.AddContactRequest

internal sealed interface BlockedParticipantsScreenEffect {

    data class ShowMessage(
        val messageResId: Int,
    ) : BlockedParticipantsScreenEffect

    data class PlacePhoneCall(
        val destination: String,
    ) : BlockedParticipantsScreenEffect

    data class ShowContactCard(
        val contactId: Long,
        val contactLookupKey: String,
    ) : BlockedParticipantsScreenEffect

    data class AddContact(
        val request: AddContactRequest,
    ) : BlockedParticipantsScreenEffect
}
