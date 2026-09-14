package com.android.messaging.domain.conversation.usecase.participant.model

internal sealed interface ResolveContactActionResult {

    data object Unavailable : ResolveContactActionResult

    data class ShowContactCard(
        val contactId: Long,
        val lookupKey: String,
    ) : ResolveContactActionResult

    data class AddContact(
        val destination: String,
    ) : ResolveContactActionResult
}
