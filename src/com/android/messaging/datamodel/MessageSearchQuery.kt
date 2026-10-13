package com.android.messaging.datamodel

import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.util.ContentType

/**
 * SQL and column names backing the message text search exposed by
 * [MessagingContentProvider.SEARCH_URI].
 */
object MessageSearchQuery {
    const val MESSAGE_ID = "message_id"
    const val CONVERSATION_ID = "conversation_id"
    const val SNIPPET = "snippet"
    const val SORT_TIMESTAMP = "sort_timestamp"
    const val CONVERSATION_NAME = "conversation_name"
    const val SENDER_FULL_NAME = "sender_full_name"
    const val SENDER_FIRST_NAME = "sender_first_name"
    const val SENDER_DESTINATION = "sender_destination"

    const val MAX_RESULTS = 100

    private const val LIKE_ESCAPE = '\\'

    /**
     * Messages with a plain-text part matching the LIKE pattern bound as the only argument, newest
     * first, with their conversation and sender. Matching stays in SQLite so a search never pulls
     * message text into the app, and SQLite stops sorting at [MAX_RESULTS]. LIKE folds case for
     * ASCII only.
     *
     * Drafts are real rows in the messages table, so they must be excluded here just like every
     * other message query does.
     */
    @JvmField
    val SQL: String = buildString {
        append("SELECT ")
        append("p.").append(PartColumns.MESSAGE_ID).append(" AS ").append(MESSAGE_ID).append(", ")
        append("p.").append(PartColumns.CONVERSATION_ID).append(" AS ").append(CONVERSATION_ID)
            .append(", ")
        append("p.").append(PartColumns.TEXT).append(" AS ").append(SNIPPET).append(", ")
        append("m.").append(MessageColumns.RECEIVED_TIMESTAMP).append(" AS ")
            .append(SORT_TIMESTAMP).append(", ")
        append("c.").append(ConversationColumns.NAME).append(" AS ").append(CONVERSATION_NAME)
            .append(", ")
        append("pr.").append(ParticipantColumns.FULL_NAME).append(" AS ")
            .append(SENDER_FULL_NAME).append(", ")
        append("pr.").append(ParticipantColumns.FIRST_NAME).append(" AS ")
            .append(SENDER_FIRST_NAME).append(", ")
        append("pr.").append(ParticipantColumns.DISPLAY_DESTINATION).append(" AS ")
            .append(SENDER_DESTINATION).append(" ")
        append("FROM ").append(DatabaseHelper.PARTS_TABLE).append(" AS p ")
        append("INNER JOIN ").append(DatabaseHelper.MESSAGES_TABLE).append(" AS m ")
        append("ON p.").append(PartColumns.MESSAGE_ID).append(" = m.").append(MessageColumns._ID)
            .append(" ")
        append("LEFT JOIN ").append(DatabaseHelper.CONVERSATIONS_TABLE).append(" AS c ")
        append("ON p.").append(PartColumns.CONVERSATION_ID).append(" = c.")
            .append(ConversationColumns._ID).append(" ")
        append("LEFT JOIN ").append(DatabaseHelper.PARTICIPANTS_TABLE).append(" AS pr ")
        append("ON m.").append(MessageColumns.SENDER_PARTICIPANT_ID).append(" = pr.")
            .append(ParticipantColumns._ID).append(" ")
        append("WHERE p.").append(PartColumns.CONTENT_TYPE).append(" = '")
            .append(ContentType.TEXT_PLAIN).append("' ")
        append("AND p.").append(PartColumns.TEXT).append(" LIKE ? ESCAPE '")
            .append(LIKE_ESCAPE).append("' ")
        append("AND m.").append(MessageColumns.STATUS).append(" <> ")
            .append(MessageData.BUGLE_STATUS_OUTGOING_DRAFT).append(" ")
        // A message can have several text parts; list it once.
        append("GROUP BY p.").append(PartColumns.MESSAGE_ID).append(" ")
        append("ORDER BY m.").append(MessageColumns.RECEIVED_TIMESTAMP).append(" DESC, p.")
            .append(PartColumns.MESSAGE_ID).append(" DESC ")
        append("LIMIT ").append(MAX_RESULTS)
    }

    /** Matches [query] anywhere in the text, with `%`, `_` and the escape character literal. */
    @JvmStatic
    fun likePattern(query: String): String {
        val escaped = query
            .replace("$LIKE_ESCAPE", "$LIKE_ESCAPE$LIKE_ESCAPE")
            .replace("%", "$LIKE_ESCAPE%")
            .replace("_", "${LIKE_ESCAPE}_")

        return "%$escaped%"
    }
}
