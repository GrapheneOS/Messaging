package com.android.messaging.datamodel

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MESSAGES_TABLE
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.testutil.installTestFactory
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Every delete path in BugleDatabaseOperations deletes messages and conversations only, and leaves
 * parts and conversation_participants to the ON DELETE CASCADE the schema declares. That holds only
 * while foreign keys are enforced on the connection the app itself opens.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseHelperForeignKeysTest {

    @Before
    fun setUp() {
        installTestFactory(context = RuntimeEnvironment.getApplication().applicationContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun openedDatabase_cascadesDeletesToPartsAndConversationParticipants() {
        val db = DatabaseHelper.getInstance(applicationContext).writableDatabase

        val conversationId = db.insertConversation()
        db.insertConversationParticipant(conversationId, db.insertParticipant("+15550001"))
        val keptMessageId = db.insertMessage(conversationId)
        db.insertPart(db.insertMessage(conversationId), conversationId, "deleted message body")
        db.insertPart(keptMessageId, conversationId, "kept message body")

        db.delete(
            DatabaseHelper.MESSAGES_TABLE,
            "${MessageColumns._ID} NOT IN (?)",
            arrayOf(keptMessageId),
        )

        assertEquals(
            "parts of a deleted message still hold its text",
            listOf("kept message body"),
            db.partTexts(),
        )

        db.delete(DatabaseHelper.CONVERSATIONS_TABLE, "_id=?", arrayOf(conversationId))

        assertEquals("parts outlived their conversation", emptyList<String>(), db.partTexts())
        assertEquals(
            "conversation_participants outlived their conversation",
            0,
            db.countRows(DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE),
        )
    }

    @Test
    fun upgrade_purgesRowsOrphanedWhileCascadesWereInert() {
        val currentVersion = applicationContext.getString(R.string.database_version).toInt()

        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            // Reproduce a pre-fix database, where nothing stopped children outliving their parent.
            db.execSQL("PRAGMA foreign_keys=OFF")
            val conversationId = db.insertConversation()
            val participantId = db.insertParticipant("+15550001")
            val liveMessageId = db.insertMessage(conversationId, senderId = participantId)
            db.insertPart(liveMessageId, conversationId, "live message body")
            // One row per foreign key the schema declares, each pointing at a row that is gone.
            db.insertPart(DELETED_ROW_ID, conversationId, "message-less part")
            db.insertPart(liveMessageId, DELETED_ROW_ID, "conversation-less part")
            db.insertMessage(DELETED_ROW_ID)
            db.insertConversationParticipant(conversationId, participantId)
            db.insertConversationParticipant(DELETED_ROW_ID, participantId)
            db.insertConversationParticipant(conversationId, DELETED_ROW_ID)
            val danglingSenderId = db.insertMessage(conversationId, senderId = DELETED_ROW_ID)

            db.execSQL("PRAGMA foreign_keys=ON")

            DatabaseUpgradeHelper().doOnUpgrade(db, 3, currentVersion)

            assertEquals(
                "orphaned parts still hold the text of deleted messages",
                listOf("live message body"),
                db.partTexts(),
            )
            assertEquals(
                "orphaned conversation_participants survived the upgrade",
                1,
                db.countRows(DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE),
            )
            assertEquals("orphaned messages survived the upgrade", 2, db.countRows(MESSAGES_TABLE))
            assertEquals(
                "a sender_id pointing at a deleted participant was not nulled out",
                null,
                db.senderIdOf(danglingSenderId),
            )
            assertEquals(
                "a sender_id pointing at a live participant was nulled out",
                participantId,
                db.senderIdOf(liveMessageId),
            )
            assertEquals("upgrade left foreign key violations behind", 0, db.foreignKeyViolations())
        }
    }

    /** onDowngrade() and the failed-upgrade path wipe a populated database while cascades run. */
    @Test
    fun rebuildTables_dropsEveryTableWhileForeignKeysAreEnforced() {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            db.setForeignKeyConstraintsEnabled(true)
            val conversationId = db.insertConversation()
            db.insertPart(db.insertMessage(conversationId), conversationId, "message body")
            db.insertConversationParticipant(conversationId, db.insertParticipant("+15550001"))

            db.beginTransaction()
            try {
                DatabaseHelper.rebuildTables(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            assertEquals("rebuild left rows behind", emptyList<String>(), db.partTexts())
            assertEquals(
                "rebuild left the conversations table behind",
                0,
                db.countRows(DatabaseHelper.CONVERSATIONS_TABLE),
            )
        }
    }

    private val applicationContext
        get() = RuntimeEnvironment.getApplication().applicationContext

    private fun SQLiteDatabase.insertConversation(): String {
        return insertOrThrow(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(ConversationColumns.NAME to "Conversation"),
        ).toString()
    }

    private fun SQLiteDatabase.insertParticipant(destination: String): String {
        return insertOrThrow(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(ParticipantColumns.NORMALIZED_DESTINATION to destination),
        ).toString()
    }

    private fun SQLiteDatabase.insertConversationParticipant(
        conversationId: String,
        participantId: String,
    ) {
        insertOrThrow(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ConversationParticipantsColumns.CONVERSATION_ID to conversationId,
                ConversationParticipantsColumns.PARTICIPANT_ID to participantId,
            ),
        )
    }

    private fun SQLiteDatabase.insertMessage(
        conversationId: String,
        senderId: String? = null,
    ): String {
        return insertOrThrow(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to senderId,
            ),
        ).toString()
    }

    private fun SQLiteDatabase.senderIdOf(messageId: String): String? {
        val sql = "SELECT ${MessageColumns.SENDER_PARTICIPANT_ID} FROM $MESSAGES_TABLE WHERE _id=?"
        return rawQuery(sql, arrayOf(messageId)).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }
    }

    private fun SQLiteDatabase.foreignKeyViolations(): Int {
        return rawQuery("SELECT COUNT(*) FROM pragma_foreign_key_check", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private fun SQLiteDatabase.insertPart(
        messageId: String,
        conversationId: String,
        text: String,
    ) {
        insertOrThrow(
            DatabaseHelper.PARTS_TABLE,
            null,
            contentValuesOf(
                PartColumns.MESSAGE_ID to messageId,
                PartColumns.CONVERSATION_ID to conversationId,
                PartColumns.TEXT to text,
            ),
        )
    }

    private fun SQLiteDatabase.partTexts(): List<String> {
        val texts = mutableListOf<String>()
        val sql = "SELECT ${PartColumns.TEXT} FROM ${DatabaseHelper.PARTS_TABLE}"
        rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                texts += cursor.getString(0)
            }
        }
        return texts
    }

    private fun SQLiteDatabase.countRows(table: String): Int {
        return rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private companion object {
        const val DELETED_ROW_ID = "9001"
    }
}
