package com.android.messaging.ui.blockedparticipants.screen.model

internal sealed interface BlockedParticipantsScreenEffect {

    data class ShowMessage(
        val messageResId: Int,
    ) : BlockedParticipantsScreenEffect

    data class OpenParticipantChat(
        val conversationId: String,
    ) : BlockedParticipantsScreenEffect

    data class PlacePhoneCall(
        val destination: String,
    ) : BlockedParticipantsScreenEffect

    data class ShowOrAddContact(
        val contactId: Long,
        val contactLookupKey: String?,
        val avatarUri: String?,
        val normalizedDestination: String?,
    ) : BlockedParticipantsScreenEffect
}
