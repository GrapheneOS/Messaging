package com.android.messaging.domain.conversation.usecase

import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapper
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.action.InsertNewMessageAction
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.util.core.extension.unitFlow
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal interface SendConversationDraft {
    operator fun invoke(
        conversationId: String,
        draft: ConversationDraft,
    ): Flow<Unit>
}

internal class SendConversationDraftImpl @Inject constructor(
    private val conversationDraftMessageDataMapper: ConversationDraftMessageDataMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : SendConversationDraft {

    override operator fun invoke(
        conversationId: String,
        draft: ConversationDraft,
    ): Flow<Unit> {
        if (conversationId.isBlank()) {
            throw BlankConversationIdException()
        }

        if (!draft.hasContent) {
            throw EmptyConversationDraftException(
                conversationId = conversationId,
            )
        }

        return unitFlow {
            try {
                withContext(context = defaultDispatcher) {
                    val message = conversationDraftMessageDataMapper.map(
                        conversationId = conversationId,
                        draft = draft,
                    )

                    message.consolidateText()
                    InsertNewMessageAction.insertNewMessage(message)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                throw DraftDispatchFailedException(
                    conversationId = conversationId,
                    cause = exception,
                )
            }
        }
    }
}

internal sealed class SendConversationDraftException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

internal class BlankConversationIdException :
    SendConversationDraftException(
        message = "Conversation id must not be blank.",
    )

internal class EmptyConversationDraftException(
    conversationId: String,
) : SendConversationDraftException(
    message = "Draft must contain content before it can be sent " +
        "for conversation $conversationId.",
)

internal class DraftDispatchFailedException(
    conversationId: String,
    cause: Throwable,
) : SendConversationDraftException(
    message = "Failed to enqueue outgoing draft for conversation $conversationId.",
    cause = cause,
)
