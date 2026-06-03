package com.android.messaging.domain.shareintent.usecase

import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.exception.SendConversationDraftException
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UriUtil
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

internal interface SendSharedContentToConversations {
    suspend operator fun invoke(draft: ConversationDraft, conversationIds: Set<String>)
}

internal class SendSharedContentToConversationsImpl @Inject constructor(
    private val sendConversationDraft: SendConversationDraft,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : SendSharedContentToConversations {

    override suspend fun invoke(
        draft: ConversationDraft,
        conversationIds: Set<String>,
    ) {
        if (conversationIds.isEmpty()) {
            return
        }

        val drafts = perConversationDrafts(draft, conversationIds.size)

        conversationIds.forEachIndexed { index, conversationId ->
            sendToConversation(conversationId, drafts[index])
        }
    }

    private suspend fun perConversationDrafts(
        template: ConversationDraft,
        count: Int,
    ): List<ConversationDraft> {
        if (template.attachments.isEmpty()) {
            return List(count) { template }
        }

        return withContext(ioDispatcher) {
            List(count) { index ->
                when (index) {
                    0 -> template
                    else -> template.copy(
                        attachments = copyAttachments(template.attachments),
                    )
                }
            }
        }
    }

    private fun copyAttachments(
        attachments: ImmutableList<ConversationDraftAttachment>,
    ): ImmutableList<ConversationDraftAttachment> {
        return attachments.mapNotNull { attachment ->
            val copyUri = UriUtil.persistContentToScratchSpace(attachment.contentUri.toUri())
            copyUri?.let { attachment.copy(contentUri = it.toString()) }
        }.toImmutableList()
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
