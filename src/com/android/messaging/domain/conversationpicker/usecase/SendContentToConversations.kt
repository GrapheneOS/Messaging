package com.android.messaging.domain.conversationpicker.usecase

import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.exception.SendConversationDraftException
import com.android.messaging.domain.conversationpicker.model.SendContentResult
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UriUtil
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

internal interface SendContentToConversations {
    suspend operator fun invoke(
        draft: ConversationDraft,
        conversationIds: Set<ConversationId>,
    ): SendContentResult
}

internal class SendContentToConversationsImpl @Inject constructor(
    private val sendConversationDraft: SendConversationDraft,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : SendContentToConversations {

    override suspend fun invoke(
        draft: ConversationDraft,
        conversationIds: Set<ConversationId>,
    ): SendContentResult {
        if (conversationIds.isEmpty()) {
            return SendContentResult.Success
        }

        val drafts = perConversationDrafts(
            draft = draft,
            count = conversationIds.size,
        )

        var anyFailed = false
        conversationIds.forEachIndexed { index, conversationId ->
            val perConversationDraft = drafts[index]
            if (perConversationDraft == null) {
                LogUtil.w(
                    TAG,
                    "Skipping send to ${conversationId.value}: failed to copy attachments"
                )
                anyFailed = true
                return@forEachIndexed
            }

            if (!sendToConversation(conversationId, perConversationDraft)) {
                anyFailed = true
            }
        }

        return when {
            anyFailed -> SendContentResult.Failure
            else -> SendContentResult.Success
        }
    }

    private suspend fun perConversationDrafts(
        draft: ConversationDraft,
        count: Int,
    ): List<ConversationDraft?> {
        if (draft.attachments.isEmpty()) {
            return List(count) { draft }
        }

        return withContext(ioDispatcher) {
            List(count) { index ->
                when (index) {
                    0 -> draft
                    else -> copyAttachments(draft.attachments)
                        ?.let { draft.copy(attachments = it) }
                }
            }
        }
    }

    private fun copyAttachments(
        attachments: ImmutableList<ConversationDraftAttachment>,
    ): ImmutableList<ConversationDraftAttachment>? {
        return attachments.map { attachment ->
            val copiedUri = UriUtil.persistContentToScratchSpace(
                attachment.contentUri.toUri()
            ) ?: return null
            attachment.copy(contentUri = copiedUri.toString())
        }.toImmutableList()
    }

    private suspend fun sendToConversation(
        conversationId: ConversationId,
        draft: ConversationDraft,
    ): Boolean {
        return try {
            sendConversationDraft(conversationId, draft).collect()
            true
        } catch (exception: SendConversationDraftException) {
            LogUtil.w(TAG, "Failed to send shared draft to ${conversationId.value}", exception)
            false
        }
    }

    private companion object {
        private const val TAG = "SendContent"
    }
}
