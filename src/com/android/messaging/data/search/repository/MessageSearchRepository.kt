package com.android.messaging.data.search.repository

import android.content.ContentResolver
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.search.model.MessageSearchResult
import com.android.messaging.datamodel.MessageSearchQuery
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.util.db.ext.getLong
import com.android.messaging.util.db.ext.getStringOrNull
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal interface MessageSearchRepository {
    fun search(query: String): Flow<ImmutableList<MessageSearchResult>>
}

internal class MessageSearchRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : MessageSearchRepository {

    override fun search(query: String): Flow<ImmutableList<MessageSearchResult>> {
        return flow {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                emit(persistentListOf())
                return@flow
            }

            emit(queryMessages(trimmed))
        }.flowOn(messagingDbDispatcher)
    }

    private fun queryMessages(query: String): ImmutableList<MessageSearchResult> {
        val uri = MessagingContentProvider.buildSearchUri(query)

        val cursor = contentResolver.query(
            uri,
            null,
            null,
            null,
            null,
        ) ?: return persistentListOf()

        return cursor.use { searchCursor ->
            buildList {
                while (searchCursor.moveToNext()) {
                    val conversationId = searchCursor
                        .getStringOrNull(MessageSearchQuery.CONVERSATION_ID)
                        ?.let { value -> ConversationId.fromOrNull(value) }

                    val messageId = searchCursor
                        .getStringOrNull(MessageSearchQuery.MESSAGE_ID)
                        ?.let { value -> MessageId.fromOrNull(value) }

                    if (conversationId != null && messageId != null) {
                        add(
                            MessageSearchResult(
                                messageId = messageId,
                                conversationId = conversationId,
                                snippet = searchCursor
                                    .getStringOrNull(MessageSearchQuery.SNIPPET)
                                    .orEmpty(),
                                timestamp = searchCursor.getLong(
                                    MessageSearchQuery.SORT_TIMESTAMP,
                                ),
                                conversationName = searchCursor
                                    .getStringOrNull(MessageSearchQuery.CONVERSATION_NAME),
                                senderName = searchCursor
                                    .getStringOrNull(MessageSearchQuery.SENDER_FULL_NAME)
                                    ?: searchCursor
                                        .getStringOrNull(MessageSearchQuery.SENDER_FIRST_NAME)
                                    ?: searchCursor
                                        .getStringOrNull(MessageSearchQuery.SENDER_DESTINATION),
                            ),
                        )
                    }
                }
            }
        }.toImmutableList()
    }
}
