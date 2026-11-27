package com.android.messaging.datamodel.action

import android.database.Cursor
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.data.ParticipantData

internal data class IncomingMessageRow(
    val id: String,
    val conversationId: String,
    val senderDestination: String?,
    val selfId: String,
    val protocol: Int,
    val status: Int,
    val read: Boolean,
    val seen: Boolean,
    val text: String?,
    val smsMessageUri: String?,
)

internal data class ConversationRow(
    val id: String,
    val threadId: Long,
    val selfId: String,
    val archived: Boolean,
    val latestMessageId: String?,
    val snippet: String?,
    val smsServiceCenter: String?,
    val participantDestinations: List<String>,
)

internal fun OutgoingPipelineWorld.incomingMessage(messageId: String): IncomingMessageRow {
    return requireNotNull(
        queryIncomingMessages(where = "m._id = ?", arguments = arrayOf(messageId)).singleOrNull(),
    ) { "Message $messageId is missing" }
}

internal fun OutgoingPipelineWorld.incomingMessagesIn(
    conversationId: String,
): List<IncomingMessageRow> {
    return queryIncomingMessages(
        where = "m.${MessageColumns.CONVERSATION_ID} = ?",
        arguments = arrayOf(conversationId),
    )
}

internal fun OutgoingPipelineWorld.allIncomingMessages(): List<IncomingMessageRow> {
    return queryIncomingMessages(where = "1", arguments = emptyArray())
}

internal fun OutgoingPipelineWorld.conversationIds(): List<String> {
    return withDatabase { db ->
        db.rawQuery(
            "SELECT _id FROM ${DatabaseHelper.CONVERSATIONS_TABLE} ORDER BY _id",
            null,
        ).use { cursor -> cursor.readAll { cursor.getString(0) } }
    }
}

internal fun OutgoingPipelineWorld.conversation(conversationId: String): ConversationRow {
    return withDatabase { db ->
        val participants = db.rawQuery(
            CONVERSATION_PARTICIPANTS_QUERY,
            arrayOf(conversationId),
        ).use { cursor -> cursor.readAll { cursor.getString(0) } }
        db.rawQuery(CONVERSATION_QUERY, arrayOf(conversationId)).use { cursor ->
            require(cursor.moveToFirst()) { "Conversation $conversationId is missing" }
            ConversationRow(
                id = conversationId,
                threadId = cursor.getLong(0),
                selfId = cursor.getString(1),
                archived = cursor.getInt(2) != 0,
                latestMessageId = cursor.getString(3),
                snippet = cursor.getString(4),
                smsServiceCenter = cursor.getString(5),
                participantDestinations = participants,
            )
        }
    }
}

internal fun OutgoingPipelineWorld.blockDestination(destination: String) {
    withDatabase { db ->
        db.inTransaction {
            BugleDatabaseOperations.getOrCreateParticipantInTransaction(
                db,
                ParticipantData.getFromRawPhoneBySimLocale(destination, FakeTelephony.SUB_ID_SIM_1),
            )
        }
        BugleDatabaseOperations.updateDestination(db, destination, true)
    }
}

internal fun OutgoingPipelineWorld.archiveConversation(conversationId: String) {
    withDatabase { db ->
        db.inTransaction {
            BugleDatabaseOperations.updateConversationArchiveStatusInTransaction(
                db,
                conversationId,
                true,
            )
        }
    }
}

private fun OutgoingPipelineWorld.queryIncomingMessages(
    where: String,
    arguments: Array<String>,
): List<IncomingMessageRow> {
    return withDatabase { db ->
        db.rawQuery(
            "$INCOMING_MESSAGE_QUERY WHERE $where ORDER BY m._id",
            arguments,
        ).use { cursor ->
            cursor.readAll {
                IncomingMessageRow(
                    id = cursor.getString(0),
                    conversationId = cursor.getString(1),
                    senderDestination = cursor.getString(2),
                    selfId = cursor.getString(3),
                    protocol = cursor.getInt(4),
                    status = cursor.getInt(5),
                    read = cursor.getInt(6) != 0,
                    seen = cursor.getInt(7) != 0,
                    text = cursor.getString(8),
                    smsMessageUri = cursor.getString(9),
                )
            }
        }
    }
}

private fun <T> Cursor.readAll(readRow: () -> T): List<T> {
    return buildList {
        while (moveToNext()) {
            add(readRow())
        }
    }
}

private fun DatabaseWrapper.inTransaction(block: () -> Unit) {
    beginTransaction()
    try {
        block()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

private val INCOMING_MESSAGE_QUERY = """
    SELECT m._id, m.${MessageColumns.CONVERSATION_ID}, p.${ParticipantColumns.SEND_DESTINATION},
        m.${MessageColumns.SELF_PARTICIPANT_ID}, m.${MessageColumns.PROTOCOL},
        m.${MessageColumns.STATUS}, m.${MessageColumns.READ}, m.${MessageColumns.SEEN},
        (SELECT group_concat(${PartColumns.TEXT}, '\n') FROM ${DatabaseHelper.PARTS_TABLE}
            WHERE ${PartColumns.MESSAGE_ID} = m._id AND ${PartColumns.TEXT} IS NOT NULL),
        m.${MessageColumns.SMS_MESSAGE_URI}
    FROM ${DatabaseHelper.MESSAGES_TABLE} m
    LEFT JOIN ${DatabaseHelper.PARTICIPANTS_TABLE} p ON p._id = m.${MessageColumns.SENDER_PARTICIPANT_ID}
""".trimIndent()

private val CONVERSATION_QUERY = """
    SELECT ${ConversationColumns.SMS_THREAD_ID}, ${ConversationColumns.CURRENT_SELF_ID},
        ${ConversationColumns.ARCHIVE_STATUS}, ${ConversationColumns.LATEST_MESSAGE_ID},
        ${ConversationColumns.SNIPPET_TEXT}, ${ConversationColumns.SMS_SERVICE_CENTER}
    FROM ${DatabaseHelper.CONVERSATIONS_TABLE} WHERE _id = ?
""".trimIndent()

private val CONVERSATION_PARTICIPANTS_QUERY = """
    SELECT p.${ParticipantColumns.SEND_DESTINATION}
    FROM ${DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE} cp
    JOIN ${DatabaseHelper.PARTICIPANTS_TABLE} p
        ON p._id = cp.${ConversationParticipantsColumns.PARTICIPANT_ID}
    WHERE cp.${ConversationParticipantsColumns.CONVERSATION_ID} = ?
    ORDER BY p.${ParticipantColumns.SEND_DESTINATION}
""".trimIndent()
