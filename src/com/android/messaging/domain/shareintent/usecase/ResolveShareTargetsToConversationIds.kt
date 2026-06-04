package com.android.messaging.domain.shareintent.usecase

import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.domain.shareintent.model.ShareSendTarget
import javax.inject.Inject

internal interface ResolveShareTargetsToConversationIds {
    suspend operator fun invoke(targets: Set<ShareSendTarget>): Set<String>
}

internal class ResolveShareTargetsToConversationIdsImpl @Inject constructor(
    private val resolveConversationId: ResolveConversationId,
) : ResolveShareTargetsToConversationIds {

    override suspend fun invoke(targets: Set<ShareSendTarget>): Set<String> {
        return targets.mapNotNullTo(LinkedHashSet(targets.size)) { target ->
            resolveTarget(target)
        }
    }

    private suspend fun resolveTarget(target: ShareSendTarget): String? {
        return when (target) {
            is ShareSendTarget.Conversation -> target.conversationId

            is ShareSendTarget.Contact -> {
                resolveContactConversationId(destination = target.destination)
            }
        }
    }

    private suspend fun resolveContactConversationId(destination: String): String? {
        return when (val result = resolveConversationId(destinations = listOf(destination))) {
            is ResolveConversationIdResult.Resolved -> result.conversationId

            ResolveConversationIdResult.EmptyDestinations -> null
            ResolveConversationIdResult.NotResolved -> null
        }
    }
}
