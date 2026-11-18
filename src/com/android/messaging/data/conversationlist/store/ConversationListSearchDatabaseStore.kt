package com.android.messaging.data.conversationlist.store

import android.database.Cursor
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversationlist.model.SearchableMessage
import com.android.messaging.data.conversationlist.model.SearchableParticipant
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.util.LogUtil
import com.android.messaging.util.db.ext.getInt
import com.android.messaging.util.db.ext.getLong
import com.android.messaging.util.db.ext.getStringOrNull
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

internal interface ConversationListSearchDatabaseStore {

    fun readParticipants(): ImmutableMap<ConversationId, ImmutableList<SearchableParticipant>>

    /**
     * The newest messages with a word starting with each word of the query, drafts left out.
     */
    fun findMessages(query: String): ImmutableList<SearchableMessage>
}

internal class ConversationListSearchDatabaseStoreImpl @Inject constructor() :
    ConversationListSearchDatabaseStore {

    override fun readParticipants():
        ImmutableMap<ConversationId, ImmutableList<SearchableParticipant>> {
        val participantsByConversation =
            LinkedHashMap<ConversationId, MutableList<SearchableParticipant>>()

        DataModel.get().database.rawQuery(PARTICIPANTS_SQL, null).use { cursor ->
            while (cursor.moveToNext()) {
                val conversationId = ConversationId.fromOrNull(
                    value = cursor.getStringOrNull(ConversationParticipantsColumns.CONVERSATION_ID),
                ) ?: continue

                participantsByConversation
                    .getOrPut(conversationId, ::mutableListOf)
                    .add(
                        SearchableParticipant(
                            fullName = cursor.getStringOrNull(ParticipantColumns.FULL_NAME),
                            firstName = cursor.getStringOrNull(ParticipantColumns.FIRST_NAME),
                            displayDestination = cursor.getStringOrNull(
                                ParticipantColumns.DISPLAY_DESTINATION,
                            ),
                            normalizedDestination = cursor.getStringOrNull(
                                ParticipantColumns.NORMALIZED_DESTINATION,
                            ),
                        ),
                    )
            }
        }

        return participantsByConversation
            .mapValues { (_, participants) -> participants.toImmutableList() }
            .toImmutableMap()
    }

    override fun findMessages(query: String): ImmutableList<SearchableMessage> {
        // Splitting on everything but letters, digits and marks also drops the characters the
        // text index would read as query syntax
        val words = query
            .split(NON_WORD_REGEX)
            .filter { word -> word.any(Char::isLetterOrDigit) }

        if (words.isEmpty()) {
            return persistentListOf()
        }

        val database = DataModel.get().database
        val messageQuery = when {
            database.hasPartsTextIndex() -> textIndexQuery(words = words)

            else -> {
                LogUtil.w(TAG, "Searching the messages without the parts text index")
                textScanQuery(words = words)
            }
        }

        return database.rawQuery(messageQuery.sql, messageQuery.args)
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        cursor.toSearchableMessage()?.let(::add)
                    }
                }
            }
            // A message with several matching text parts is found once
            .distinctBy(SearchableMessage::messageId)
            .toImmutableList()
    }

    private fun DatabaseWrapper.hasPartsTextIndex(): Boolean {
        return rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'trigger' AND name = ?",
            arrayOf(DatabaseHelper.PARTS_TEXT_INDEX_INSERT_TRIGGER),
        ).use(Cursor::moveToFirst)
    }

    private fun textIndexQuery(words: List<String>): MessageQuery {
        // Lowercase words can't be read as the AND, OR, NOT and NEAR operators
        val match = words.joinToString(separator = " ") { word -> "${word.lowercase()}*" }

        return MessageQuery(
            sql = messagesSql(
                source = "${DatabaseHelper.PARTS_TEXT_INDEX_TABLE} " +
                    "JOIN ${DatabaseHelper.PARTS_TABLE} AS part " +
                    "ON part.${PartColumns._ID} = ${DatabaseHelper.PARTS_TEXT_INDEX_TABLE}.docid",
                condition = "${DatabaseHelper.PARTS_TEXT_INDEX_TABLE} MATCH ?",
            ),
            args = arrayOf(match),
        )
    }

    private fun textScanQuery(words: List<String>): MessageQuery {
        // LIKE ignores case only in ASCII, which is what the index is for. The words hold no
        // LIKE wildcards: they consist of letters, digits and marks
        return MessageQuery(
            sql = messagesSql(
                source = "${DatabaseHelper.PARTS_TABLE} AS part",
                condition = words.joinToString(separator = " AND ") {
                    "part.${PartColumns.TEXT} LIKE ?"
                },
            ),
            args = words
                .map { word -> "%$word%" }
                .toTypedArray(),
        )
    }

    private fun messagesSql(
        source: String,
        condition: String,
    ): String {
        return """
            SELECT message.${MessageColumns._ID} AS $MESSAGE_ID_COLUMN,
                message.${MessageColumns.CONVERSATION_ID} AS $CONVERSATION_ID_COLUMN,
                part.${PartColumns.TEXT} AS $TEXT_COLUMN,
                message.${MessageColumns.RECEIVED_TIMESTAMP} AS $TIMESTAMP_COLUMN,
                message.${MessageColumns.STATUS} AS $STATUS_COLUMN,
                sender.${ParticipantColumns.FIRST_NAME} AS $SENDER_FIRST_NAME_COLUMN,
                sender.${ParticipantColumns.FULL_NAME} AS $SENDER_FULL_NAME_COLUMN,
                sender.${ParticipantColumns.DISPLAY_DESTINATION}
                    AS $SENDER_DISPLAY_DESTINATION_COLUMN
            FROM $source
            JOIN ${DatabaseHelper.MESSAGES_TABLE} AS message
                ON message.${MessageColumns._ID} = part.${PartColumns.MESSAGE_ID}
            LEFT JOIN ${DatabaseHelper.PARTICIPANTS_TABLE} AS sender
                ON sender.${ParticipantColumns._ID} = message.${MessageColumns.SENDER_PARTICIPANT_ID}
            WHERE $condition
                AND message.${MessageColumns.STATUS} != ${MessageData.BUGLE_STATUS_OUTGOING_DRAFT}
            ORDER BY message.${MessageColumns.RECEIVED_TIMESTAMP} DESC,
                message.${MessageColumns._ID} DESC
            LIMIT $MESSAGE_RESULTS_LIMIT
        """.trimIndent()
    }

    private fun Cursor.toSearchableMessage(): SearchableMessage? {
        val messageId = MessageId.fromOrNull(value = getStringOrNull(MESSAGE_ID_COLUMN))
        val conversationId = ConversationId.fromOrNull(
            value = getStringOrNull(CONVERSATION_ID_COLUMN),
        )
        val text = getStringOrNull(TEXT_COLUMN)

        if (messageId == null || conversationId == null || text == null) {
            return null
        }

        return SearchableMessage(
            messageId = messageId,
            conversationId = conversationId,
            text = text,
            receivedTimestamp = getLong(TIMESTAMP_COLUMN),
            isIncoming = MessageData.getIsIncoming(getInt(STATUS_COLUMN)),
            // The same name the conversation list shows for a group's latest message
            senderName = listOf(
                SENDER_FIRST_NAME_COLUMN,
                SENDER_FULL_NAME_COLUMN,
                SENDER_DISPLAY_DESTINATION_COLUMN,
            ).firstNotNullOfOrNull { column ->
                getStringOrNull(column)?.takeIf(String::isNotEmpty)
            },
        )
    }

    private class MessageQuery(
        val sql: String,
        val args: Array<String>,
    )

    private companion object {
        private const val TAG = "ConvListSearchStore"

        private const val MESSAGE_RESULTS_LIMIT = 200

        private const val MESSAGE_ID_COLUMN = "message_id"
        private const val CONVERSATION_ID_COLUMN = "conversation_id"
        private const val TEXT_COLUMN = "text"
        private const val TIMESTAMP_COLUMN = "received_timestamp"
        private const val STATUS_COLUMN = "message_status"
        private const val SENDER_FIRST_NAME_COLUMN = "sender_first_name"
        private const val SENDER_FULL_NAME_COLUMN = "sender_full_name"
        private const val SENDER_DISPLAY_DESTINATION_COLUMN = "sender_display_destination"

        private val NON_WORD_REGEX = Regex("[^\\p{L}\\p{N}\\p{M}]+")

        private val PARTICIPANTS_SQL = """
            SELECT member.${ConversationParticipantsColumns.CONVERSATION_ID},
                participant.${ParticipantColumns.FULL_NAME},
                participant.${ParticipantColumns.FIRST_NAME},
                participant.${ParticipantColumns.DISPLAY_DESTINATION},
                participant.${ParticipantColumns.NORMALIZED_DESTINATION}
            FROM ${DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE} AS member
            JOIN ${DatabaseHelper.PARTICIPANTS_TABLE} AS participant
                ON participant.${ParticipantColumns._ID} =
                    member.${ConversationParticipantsColumns.PARTICIPANT_ID}
            ORDER BY member.${ConversationParticipantsColumns._ID}
        """.trimIndent()
    }
}
