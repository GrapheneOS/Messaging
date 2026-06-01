package com.android.messaging.domain.shareintent.usecase

import android.content.Intent
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.exception.SendConversationDraftException
import com.android.messaging.util.LogUtil
import javax.inject.Inject
import kotlinx.coroutines.flow.collect

internal interface SendSharedContentToConversations {
    suspend operator fun invoke(intent: Intent, conversationIds: Set<String>)
}

internal class SendSharedContentToConversationsImpl @Inject constructor(
    private val buildSharedConversationDraft: BuildSharedConversationDraft,
    private val sendConversationDraft: SendConversationDraft,
) : SendSharedContentToConversations {

    override suspend fun invoke(intent: Intent, conversationIds: Set<String>) {
        val draft = buildSharedConversationDraft(intent) ?: return
        for (conversationId in conversationIds) {
            sendToConversation(conversationId, draft)
        }
    }

    private suspend fun sendToConversation(conversationId: String, draft: ConversationDraft) {
        try {
            sendConversationDraft(conversationId, draft).collect()
        } catch (exception: SendConversationDraftException) {
            LogUtil.w(TAG, "Failed to send shared draft to $conversationId", exception)
        }
    }

    private companion object {
        private const val TAG = "SendSharedContent"
    }
}
