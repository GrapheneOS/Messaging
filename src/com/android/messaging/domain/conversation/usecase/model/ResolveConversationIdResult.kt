package com.android.messaging.domain.conversation.usecase.model

internal sealed interface ResolveConversationIdResult {
    data object EmptyDestinations : ResolveConversationIdResult

    data object NotResolved : ResolveConversationIdResult

    data class Resolved(
        val conversationId: String,
    ) : ResolveConversationIdResult
}
