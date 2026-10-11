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

            emit(queryMessages(query = trimmed))
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
                                    .orEmpty()
                                    .snippetAround(query = query),
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

private const val SNIPPET_LEADING_CONTEXT = 24

/**
 * Results show a single line, so start the snippet shortly before the first match to keep the
 * matched text visible in long messages.
 */
internal fun String.snippetAround(query: String): String {
    val matchIndex = indexOf(string = query, ignoreCase = true)
    if (matchIndex <= SNIPPET_LEADING_CONTEXT) {
        return this
    }

    val contextStart = matchIndex - SNIPPET_LEADING_CONTEXT
    // Prefer starting on a word boundary inside the leading context.
    val start = indexOf(char = ' ', startIndex = contextStart)
        .takeIf { index -> index in contextStart until matchIndex }
        ?.plus(1)
        ?: contextStart

    return "\u2026" + substring(startIndex = start).trimStart()
}
