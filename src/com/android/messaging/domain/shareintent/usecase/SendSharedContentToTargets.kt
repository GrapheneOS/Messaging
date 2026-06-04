package com.android.messaging.domain.shareintent.usecase

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.domain.shareintent.model.ShareSendTarget
import javax.inject.Inject

internal interface SendSharedContentToTargets {
    suspend operator fun invoke(draft: ConversationDraft, targets: Set<ShareSendTarget>)
}

internal class SendSharedContentToTargetsImpl @Inject constructor(
    private val resolveShareTargetsToConversationIds: ResolveShareTargetsToConversationIds,
    private val sendSharedContentToConversations: SendSharedContentToConversations,
) : SendSharedContentToTargets {

    override suspend fun invoke(
        draft: ConversationDraft,
        targets: Set<ShareSendTarget>,
    ) {
        val conversationIds = resolveShareTargetsToConversationIds(targets)
        sendSharedContentToConversations(draft, conversationIds)
    }
}
