package com.android.messaging.data.participantdestination.store

import androidx.core.content.contentValuesOf
import com.android.messaging.data.participantdestination.model.BareNumberParticipant
import com.android.messaging.data.participantdestination.model.ParticipantTwin
import com.android.messaging.data.participantdestination.model.Renormalization
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.util.PhoneUtils
import com.android.messaging.util.db.ext.withTransaction
import javax.inject.Inject

internal interface ParticipantDestinationDatabaseStore {

    fun readBareNumberParticipants(): List<BareNumberParticipant>

    fun readSubIdsByParticipant(): Map<String, Set<Int>>

    fun readSenderDestinations(): Set<String>

    fun readLastParticipantId(): Long

    fun applyRenormalizations(renormalizations: List<Renormalization>)
}

internal class ParticipantDestinationDatabaseStoreImpl @Inject constructor() :
    ParticipantDestinationDatabaseStore {

    override fun readBareNumberParticipants(): List<BareNumberParticipant> {
        return DataModel
            .get()
            .database
            .rawQuery(BARE_NUMBER_PARTICIPANTS_SQL, null)
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            BareNumberParticipant(
                                participantId = cursor.getString(0),
                                destination = cursor.getString(1),
                                isBlocked = cursor.getInt(2) == 1,
                            ),
                        )
                    }
                }
            }
    }

    override fun readSubIdsByParticipant(): Map<String, Set<Int>> {
        val subIdsByParticipant = mutableMapOf<String, MutableSet<Int>>()

        DataModel
            .get()
            .database
            .rawQuery(BARE_NUMBER_SUB_IDS_SQL, null)
            .use { cursor ->
                while (cursor.moveToNext()) {
                    subIdsByParticipant
                        .getOrPut(cursor.getString(0)) { mutableSetOf() }
                        .add(cursor.getInt(1))
                }
            }
        return subIdsByParticipant
    }

    override fun readSenderDestinations(): Set<String> {
        return DataModel
            .get()
            .database
            .rawQuery(SENDER_DESTINATIONS_SQL, null)
            .use { cursor ->
                buildSet {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(0))
                    }
                }
            }
    }

    override fun readLastParticipantId(): Long {
        return DataModel
            .get()
            .database
            .rawQuery(LAST_PARTICIPANT_ID_SQL, null)
            .use { cursor ->
                when {
                    cursor.moveToFirst() -> cursor.getLong(0)
                    else -> 0L
                }
            }
    }

    override fun applyRenormalizations(renormalizations: List<Renormalization>) {
        if (renormalizations.isNotEmpty()) {
            val db = DataModel.get().database
            db.withTransaction {
                renormalizations
                    .flatMapTo(mutableSetOf()) { renormalization ->
                        applyRenormalization(db = db, renormalization = renormalization)
                    }
                    .forEach { conversationId ->
                        refreshConversation(db = db, conversationId = conversationId)
                    }
            }
            BugleDatabaseOperations.clearParticipantIdCache()
        }
    }

    private fun applyRenormalization(
        db: DatabaseWrapper,
        renormalization: Renormalization,
    ): List<String> {
        val conversationIds = conversationIdsOf(
            db = db,
            participantId = renormalization.participant.participantId,
        )
        val twin = twinOf(db = db, normalizedDestination = renormalization.canonicalDestination)
        when (twin) {
            null -> normalizeInPlace(db = db, renormalization = renormalization)
            else -> mergeInto(db = db, renormalization = renormalization, twin = twin)
        }
        return conversationIds
    }

    private fun normalizeInPlace(db: DatabaseWrapper, renormalization: Renormalization) {
        val canonicalDestination = renormalization.canonicalDestination
        db.update(
            DatabaseHelper.PARTICIPANTS_TABLE,
            contentValuesOf(
                ParticipantColumns.NORMALIZED_DESTINATION to canonicalDestination,
                ParticipantColumns.DISPLAY_DESTINATION to
                    PhoneUtils.getDefault().formatForDisplay(canonicalDestination),
                ParticipantColumns.CONTACT_ID to
                    ParticipantData.PARTICIPANT_CONTACT_ID_NOT_RESOLVED,
            ),
            "${ParticipantColumns._ID}=?",
            arrayOf(renormalization.participant.participantId),
        )
    }

    private fun mergeInto(
        db: DatabaseWrapper,
        renormalization: Renormalization,
        twin: ParticipantTwin,
    ) {
        val participant = renormalization.participant
        val participantIdArgs = arrayOf(participant.participantId)
        db.update(
            DatabaseHelper.MESSAGES_TABLE,
            contentValuesOf(MessageColumns.SENDER_PARTICIPANT_ID to twin.participantId),
            "${MessageColumns.SENDER_PARTICIPANT_ID}=?",
            participantIdArgs,
        )
        db.execSQL(
            REPOINT_CONVERSATION_PARTICIPANT_SQL,
            arrayOf(twin.participantId, participant.participantId),
        )
        db.delete(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            "${ConversationParticipantsColumns.PARTICIPANT_ID}=?",
            participantIdArgs,
        )
        carryBlock(db = db, renormalization = renormalization, twin = twin)
        db.delete(
            DatabaseHelper.PARTICIPANTS_TABLE,
            "${ParticipantColumns._ID}=?",
            participantIdArgs,
        )
    }

    private fun carryBlock(
        db: DatabaseWrapper,
        renormalization: Renormalization,
        twin: ParticipantTwin,
    ) {
        val participant = renormalization.participant
        when {
            participant.isBlocked && !twin.isBlocked -> {
                db.update(
                    DatabaseHelper.PARTICIPANTS_TABLE,
                    contentValuesOf(ParticipantColumns.BLOCKED to 1),
                    "${ParticipantColumns._ID}=?",
                    arrayOf(twin.participantId),
                )
                archiveDirectConversations(
                    db = db,
                    destination = renormalization.canonicalDestination,
                )
            }
            twin.isBlocked && !participant.isBlocked -> {
                archiveDirectConversations(db = db, destination = participant.destination)
            }
        }
    }

    private fun archiveDirectConversations(db: DatabaseWrapper, destination: String) {
        db.update(
            DatabaseHelper.CONVERSATIONS_TABLE,
            contentValuesOf(ConversationColumns.ARCHIVE_STATUS to 1),
            "${ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION}=?",
            arrayOf(destination),
        )
    }

    private fun refreshConversation(db: DatabaseWrapper, conversationId: String) {
        db.execSQL(UPDATE_PARTICIPANT_COUNT_SQL, arrayOf(conversationId, conversationId))
        BugleDatabaseOperations.updateConversationNameAndAvatarInTransaction(db, conversationId)
    }

    private fun conversationIdsOf(db: DatabaseWrapper, participantId: String): List<String> {
        return db.query(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            arrayOf(ConversationParticipantsColumns.CONVERSATION_ID),
            "${ConversationParticipantsColumns.PARTICIPANT_ID}=?",
            arrayOf(participantId),
            null,
            null,
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }
    }

    private fun twinOf(db: DatabaseWrapper, normalizedDestination: String): ParticipantTwin? {
        return db.query(
            DatabaseHelper.PARTICIPANTS_TABLE,
            arrayOf(ParticipantColumns._ID, ParticipantColumns.BLOCKED),
            "${ParticipantColumns.NORMALIZED_DESTINATION}=? AND ${ParticipantColumns.SUB_ID}=?",
            arrayOf(normalizedDestination, ParticipantData.OTHER_THAN_SELF_SUB_ID.toString()),
            null,
            null,
            null,
        ).use { cursor ->
            when {
                cursor.moveToFirst() -> ParticipantTwin(
                    participantId = cursor.getString(0),
                    isBlocked = cursor.getInt(1) == 1,
                )
                else -> null
            }
        }
    }

    private companion object {
        private val BARE_NUMBER_CONDITION = """
            ${ParticipantColumns.SUB_ID} = ${ParticipantData.OTHER_THAN_SELF_SUB_ID}
                AND ${ParticipantColumns.NORMALIZED_DESTINATION} <> ''
                AND ${ParticipantColumns.NORMALIZED_DESTINATION} NOT GLOB '+*'
                AND ${ParticipantColumns.NORMALIZED_DESTINATION} NOT GLOB '*[A-Za-z@]*'
        """.trimIndent()

        private val BARE_NUMBER_PARTICIPANTS_SQL = """
            SELECT ${ParticipantColumns._ID}, ${ParticipantColumns.NORMALIZED_DESTINATION},
                ${ParticipantColumns.BLOCKED}
            FROM ${DatabaseHelper.PARTICIPANTS_TABLE}
            WHERE $BARE_NUMBER_CONDITION
        """.trimIndent()

        private val SENDER_DESTINATIONS_SQL = """
            SELECT ${ParticipantColumns.NORMALIZED_DESTINATION}
            FROM ${DatabaseHelper.PARTICIPANTS_TABLE}
            WHERE ${ParticipantColumns.SUB_ID} = ${ParticipantData.OTHER_THAN_SELF_SUB_ID}
        """.trimIndent()

        private val LAST_PARTICIPANT_ID_SQL = """
            SELECT MAX(${ParticipantColumns._ID}) FROM ${DatabaseHelper.PARTICIPANTS_TABLE}
        """.trimIndent()

        private val BARE_NUMBER_IDS_SQL = """
            SELECT ${ParticipantColumns._ID} FROM ${DatabaseHelper.PARTICIPANTS_TABLE}
            WHERE $BARE_NUMBER_CONDITION
        """.trimIndent()

        // CROSS JOIN keeps the conversations as the outer loop: the other way round takes seconds
        private val BARE_NUMBER_SUB_IDS_SQL = """
            SELECT conversationParticipant.${ConversationParticipantsColumns.PARTICIPANT_ID},
                COALESCE(self.${ParticipantColumns.SUB_ID}, ${ParticipantData.DEFAULT_SELF_SUB_ID})
            FROM ${DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE} AS conversationParticipant
            CROSS JOIN ${DatabaseHelper.MESSAGES_TABLE} AS message
                ON message.${MessageColumns.CONVERSATION_ID} =
                    conversationParticipant.${ConversationParticipantsColumns.CONVERSATION_ID}
            LEFT JOIN ${DatabaseHelper.PARTICIPANTS_TABLE} AS self
                ON self.${ParticipantColumns._ID} = message.${MessageColumns.SELF_PARTICIPANT_ID}
            WHERE conversationParticipant.${ConversationParticipantsColumns.PARTICIPANT_ID} IN (
                $BARE_NUMBER_IDS_SQL
            )
            UNION
            SELECT message.${MessageColumns.SENDER_PARTICIPANT_ID},
                COALESCE(self.${ParticipantColumns.SUB_ID}, ${ParticipantData.DEFAULT_SELF_SUB_ID})
            FROM ${DatabaseHelper.MESSAGES_TABLE} AS message
            LEFT JOIN ${DatabaseHelper.PARTICIPANTS_TABLE} AS self
                ON self.${ParticipantColumns._ID} = message.${MessageColumns.SELF_PARTICIPANT_ID}
            WHERE message.${MessageColumns.SENDER_PARTICIPANT_ID} IN ($BARE_NUMBER_IDS_SQL)
        """.trimIndent()

        private val REPOINT_CONVERSATION_PARTICIPANT_SQL = """
            UPDATE OR IGNORE ${DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE}
            SET ${ConversationParticipantsColumns.PARTICIPANT_ID} = ?
            WHERE ${ConversationParticipantsColumns.PARTICIPANT_ID} = ?
        """.trimIndent()

        private val UPDATE_PARTICIPANT_COUNT_SQL = """
            UPDATE ${DatabaseHelper.CONVERSATIONS_TABLE}
            SET ${ConversationColumns.PARTICIPANT_COUNT} = (
                SELECT COUNT(*) FROM ${DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE}
                WHERE ${ConversationParticipantsColumns.CONVERSATION_ID} = ?
            )
            WHERE ${ConversationColumns._ID} = ?
        """.trimIndent()
    }
}
