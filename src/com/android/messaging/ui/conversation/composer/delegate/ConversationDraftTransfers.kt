package com.android.messaging.ui.conversation.composer.delegate

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.awaitCancellation

internal interface ConversationDraftTransfers {
    suspend fun commitDraftWhileActive(
        conversationId: ConversationId,
        commitDraft: suspend () -> Unit,
    ): Nothing

    suspend fun transferDraft(
        fromConversationId: ConversationId,
        toConversationId: ConversationId,
    )
}

internal class ConversationDraftTransfersImpl @Inject constructor(
    private val conversationDraftsRepository: ConversationDraftsRepository,
) : ConversationDraftTransfers {

    private val draftCommitters = ConcurrentHashMap<ConversationId, List<suspend () -> Unit>>()

    override suspend fun commitDraftWhileActive(
        conversationId: ConversationId,
        commitDraft: suspend () -> Unit,
    ): Nothing {
        draftCommitters.merge(conversationId, listOf(commitDraft)) { current, added ->
            current + added
        }
        try {
            awaitCancellation()
        } finally {
            draftCommitters.computeIfPresent(conversationId) { _, current ->
                (current - commitDraft).ifEmpty { null }
            }
        }
    }

    override suspend fun transferDraft(
        fromConversationId: ConversationId,
        toConversationId: ConversationId,
    ) {
        when (fromConversationId) {
            toConversationId -> Unit

            else -> {
                draftCommitters[fromConversationId].orEmpty().forEach { commitDraft ->
                    commitDraft()
                }
                conversationDraftsRepository.moveDraft(
                    fromConversationId = fromConversationId,
                    toConversationId = toConversationId,
                )
            }
        }
    }
}
