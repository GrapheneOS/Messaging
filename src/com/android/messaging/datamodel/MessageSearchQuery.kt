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

    @JvmField
    val COLUMNS: Array<String> = arrayOf(
        MESSAGE_ID,
        CONVERSATION_ID,
        SNIPPET,
        SORT_TIMESTAMP,
        CONVERSATION_NAME,
        SENDER_FULL_NAME,
        SENDER_FIRST_NAME,
        SENDER_DESTINATION,
    )

    /**
     * Every plain-text part, newest first, with its conversation and sender. The text is matched
     * by the provider rather than with LIKE because SQLite's LIKE only folds case for ASCII, which
     * would make search case-sensitive for most non-Latin scripts.
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
        append("p.").append(PartColumns.TIMESTAMP).append(" AS ").append(SORT_TIMESTAMP)
            .append(", ")
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
        append("AND p.").append(PartColumns.TEXT).append(" IS NOT NULL ")
        append("AND m.").append(MessageColumns.STATUS).append(" <> ")
            .append(MessageData.BUGLE_STATUS_OUTGOING_DRAFT).append(" ")
        append("ORDER BY p.").append(PartColumns.TIMESTAMP).append(" DESC, p.")
            .append(PartColumns._ID).append(" DESC")
    }
}
