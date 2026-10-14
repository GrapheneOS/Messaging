package com.android.messaging.data.conversationstate.store

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.android.messaging.data.conversationstate.model.MirroredConversationState
import com.android.messaging.data.extension.archiveDirectConversations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.util.PhoneUtils
import com.android.messaging.util.db.ext.withTransaction
import javax.inject.Inject

internal interface ConversationStateDatabaseStore {

    fun readBlockedDestinations(): Set<String>

    fun readArchivedThreadIds(): Set<String>

    fun readUnarchivedBlockedThreadIds(): Set<String>

    fun restore(mirroredState: MirroredConversationState)
}

internal class ConversationStateDatabaseStoreImpl @Inject constructor() :
    ConversationStateDatabaseStore {

    override fun readBlockedDestinations(): Set<String> {
        return readStrings(sql = BLOCKED_DESTINATIONS_SQL)
    }

    override fun readArchivedThreadIds(): Set<String> {
        return readStrings(sql = ARCHIVED_THREAD_IDS_SQL)
    }

    override fun readUnarchivedBlockedThreadIds(): Set<String> {
        return readStrings(sql = UNARCHIVED_BLOCKED_THREAD_IDS_SQL)
    }

    override fun restore(mirroredState: MirroredConversationState) {
        // DatabaseWrapper swallows a full disk, which would mark a partial restore as done
        DataModel.get().database.database.withTransaction {
            mirroredState.blockedDestinations.forEach { destination ->
                blockDestination(
                    db = this,
                    destination = destination,
                    unarchivedThreadIds = mirroredState.unarchivedBlockedThreadIds,
                )
            }
            mirroredState.archivedThreadIds.forEach { threadId ->
                archiveThread(db = this, threadId = threadId, mirroredAt = mirroredState.mirroredAt)
            }
        }
    }

    private fun blockDestination(
        db: SQLiteDatabase,
        destination: String,
        unarchivedThreadIds: Set<String>,
    ) {
        val blockedCount = db.update(
            DatabaseHelper.PARTICIPANTS_TABLE,
            contentValuesOf(ParticipantColumns.BLOCKED to 1),
            "${ParticipantColumns.NORMALIZED_DESTINATION}=? AND ${ParticipantColumns.SUB_ID}=?",
            arrayOf(destination, ParticipantData.OTHER_THAN_SELF_SUB_ID.toString()),
        )
        if (blockedCount == 0) {
            db.insertOrThrow(
                DatabaseHelper.PARTICIPANTS_TABLE,
                null,
                contentValuesOf(
                    ParticipantColumns.SUB_ID to ParticipantData.OTHER_THAN_SELF_SUB_ID,
                    ParticipantColumns.NORMALIZED_DESTINATION to destination,
                    ParticipantColumns.SEND_DESTINATION to destination,
                    ParticipantColumns.DISPLAY_DESTINATION to
                        PhoneUtils.getDefault().formatForDisplay(destination),
                    ParticipantColumns.BLOCKED to 1,
                ),
            )
        }
        db.archiveDirectConversations(
            destination = destination,
            exceptThreadIds = unarchivedThreadIds,
        )
    }

    private fun archiveThread(db: SQLiteDatabase, threadId: String, mirroredAt: Long) {
        db.update(
            DatabaseHelper.CONVERSATIONS_TABLE,
            contentValuesOf(ConversationColumns.ARCHIVE_STATUS to 1),
            "${ConversationColumns.SMS_THREAD_ID}=? AND ${ConversationColumns.SORT_TIMESTAMP}<=?",
            arrayOf(threadId, mirroredAt.toString()),
        )
    }

    private fun readStrings(sql: String): Set<String> {
        return DataModel.get().database.rawQuery(sql, null).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }
    }

    private companion object {
        private val BLOCKED_DESTINATIONS_SQL = """
            SELECT ${ParticipantColumns.NORMALIZED_DESTINATION}
            FROM ${DatabaseHelper.PARTICIPANTS_TABLE}
            WHERE ${ParticipantColumns.SUB_ID} = ${ParticipantData.OTHER_THAN_SELF_SUB_ID}
                AND ${ParticipantColumns.BLOCKED} = 1
        """.trimIndent()

        private val ARCHIVED_THREAD_IDS_SQL = """
            SELECT ${ConversationColumns.SMS_THREAD_ID} FROM ${DatabaseHelper.CONVERSATIONS_TABLE}
            WHERE ${ConversationColumns.ARCHIVE_STATUS} = 1
                AND ${ConversationColumns.SMS_THREAD_ID} > 0
        """.trimIndent()

        private val UNARCHIVED_BLOCKED_THREAD_IDS_SQL = """
            SELECT conversation.${ConversationColumns.SMS_THREAD_ID}
            FROM ${DatabaseHelper.CONVERSATIONS_TABLE} AS conversation
            JOIN ${DatabaseHelper.PARTICIPANTS_TABLE} AS sender
                ON sender.${ParticipantColumns.NORMALIZED_DESTINATION} =
                    conversation.${ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION}
            WHERE sender.${ParticipantColumns.SUB_ID} = ${ParticipantData.OTHER_THAN_SELF_SUB_ID}
                AND sender.${ParticipantColumns.BLOCKED} = 1
                AND conversation.${ConversationColumns.ARCHIVE_STATUS} = 0
                AND conversation.${ConversationColumns.SMS_THREAD_ID} > 0
        """.trimIndent()
    }
}
