package com.android.messaging.datamodel

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.data.conversationstate.ConversationStateMirror
import com.android.messaging.data.databasecompatibility.DatabaseCompatibility
import com.android.messaging.data.databasecompatibility.DatabaseCompatibilityImpl
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizer
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DatabaseUpgradeHelperTest {

    private val conversationStateMirror = mockk<ConversationStateMirror>(relaxed = true)
    private val participantDestinationNormalizer =
        mockk<ParticipantDestinationNormalizer>(relaxed = true)
    private val databaseCompatibility = mockk<DatabaseCompatibility>(relaxed = true)
    private val currentVersion = RuntimeEnvironment.getApplication()
        .getString(R.string.database_version)
        .toInt()

    @Before
    fun setUp() {
        installTestFactory(context = RuntimeEnvironment.getApplication().applicationContext)
        mockkObject(ConversationStateMirror.Companion)
        every { ConversationStateMirror.get(any()) } returns conversationStateMirror
        mockkObject(ParticipantDestinationNormalizer.Companion)
        every { ParticipantDestinationNormalizer.get(any()) } returns
            participantDestinationNormalizer
        mockkObject(DatabaseCompatibility.Companion)
        every { DatabaseCompatibility.get(any()) } returns databaseCompatibility
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    /**
     * doUpgradeWithExceptions() throws unless a handler carries the version all the way to the
     * current one, and doOnUpgrade() answers that by rebuilding every table - which drops all of
     * the user's messages. So every bump of R.string.database_version needs its own handler, even
     * a handler that changes no tables because only a view changed.
     */
    @Test
    fun upgradeFromVersion3_keepsExistingDataAndRebuildsViews() {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            db.insert(
                DatabaseHelper.CONVERSATIONS_TABLE,
                null,
                contentValuesOf(ConversationColumns.NAME to "Weekend plan"),
            )

            DatabaseUpgradeHelper().doOnUpgrade(db, 3, currentVersion)

            assertEquals(
                "upgrade wiped the conversations table",
                1,
                db.countRows(DatabaseHelper.CONVERSATIONS_TABLE),
            )
            assertTrue(
                "conversation_list_view was not rebuilt",
                db.hasColumn(
                    ConversationListItemData.getConversationListView(),
                    "snippet_sender_full_name",
                ),
            )
        }
    }

    @Test
    fun anUpgrade_tellsTheConversationStateMirrorTheVersionItUpgradesFrom() {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)

            DatabaseUpgradeHelper().doOnUpgrade(db, 3, currentVersion)
        }

        verify(exactly = 1) { conversationStateMirror.onDatabaseUpgraded(oldVersion = 3) }
    }

    @Test
    fun anUpgrade_tellsTheParticipantDestinationNormalizerTheVersionItUpgradesFrom() {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)

            DatabaseUpgradeHelper().doOnUpgrade(db, 3, currentVersion)
        }

        verify(exactly = 1) { participantDestinationNormalizer.onDatabaseUpgraded(oldVersion = 3) }
    }

    @Test
    fun anUpgrade_recordsTheDatabaseVersion() {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)

            DatabaseUpgradeHelper().doOnUpgrade(db, 3, currentVersion)
        }

        verify(exactly = 1) { databaseCompatibility.recordDatabaseVersion() }
    }

    /**
     * rebuildTables() drops the parts table, and that takes its sqlite_sequence row along, so
     * parts._id restarts at 1. Notification images are named after the part they were transcoded
     * from and outlive the database, so a leftover image for part 1 would be served as the image
     * of whatever part next takes that id - showing an unrelated photo in a notification.
     */
    @Test
    fun rebuildTables_discardsCachedNotificationImages() {
        val stale = checkNotNull(
            NotificationImageProvider.buildNotificationImageUri("1")
                ?.let(NotificationImageProvider::getFileFromUri)
        )
        stale.writeBytes(byteArrayOf(1, 2, 3))

        SQLiteDatabase.create(null).use(DatabaseHelper::rebuildTables)

        assertFalse(
            "a cached notification image outlived the part id space it was named after",
            stale.exists(),
        )
    }

    /**
     * A corrupt database is deleted and recreated underneath us, and SQLiteOpenHelper answers the
     * resulting version zero with onCreate() rather than rebuildTables(). The cleanup has to sit
     * where both paths meet, or images cached against the old part ids survive to be served as
     * some unrelated part's picture.
     */
    @Test
    fun onCreate_discardsCachedNotificationImages() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        val stale = checkNotNull(
            NotificationImageProvider.buildNotificationImageUri("1")
                ?.let(NotificationImageProvider::getFileFromUri)
        )
        stale.writeBytes(byteArrayOf(1, 2, 3))

        SQLiteDatabase.create(null).use { DatabaseHelper.getInstance(context).onCreate(it) }

        assertFalse(
            "a cached notification image outlived the database it was named against",
            stale.exists(),
        )
    }

    /**
     * Conversation ids name notification channels, shortcuts and per-conversation prefs, which
     * outlive the database - restored without it, or left behind by a rebuild. Recreating a
     * deleted channel id undeletes its old settings, so counting from 1 again would hand each new
     * conversation the settings of whichever old one had its number.
     */
    @Test
    fun rebuildTables_startsEachDatabasesConversationIdsAtItsOwnPointFarPastOne() {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            val firstId = db.insertConversation()
            DatabaseHelper.rebuildTables(db)
            val secondId = db.insertConversation()

            assertTrue(firstId >= DatabaseHelper.CONVERSATION_ID_SEED_MIN)
            assertTrue(secondId >= DatabaseHelper.CONVERSATION_ID_SEED_MIN)
            assertNotEquals("a rebuilt database reused the old id space", firstId, secondId)
        }
    }

    @Test
    fun onCreate_startsConversationIdsFarPastOne() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.getInstance(context).onCreate(db)

            assertTrue(db.insertConversation() >= DatabaseHelper.CONVERSATION_ID_SEED_MIN)
        }
    }

    @Test
    fun upgradeToVersion3_createsPinnedColumnAndIndex() {
        val table = DatabaseHelper.CONVERSATIONS_TABLE
        val pinned = ConversationColumns.PINNED

        SQLiteDatabase.create(null).use { db ->
            db.execSQL("CREATE TABLE $table (_id INTEGER PRIMARY KEY)")

            DatabaseUpgradeHelper().upgradeToVersion3(db)

            assertTrue(db.hasColumn(table, pinned))
            assertTrue(db.hasIndex("index_${table}_$pinned"))
        }
    }

    @Test
    fun upgradeToVersion5_createsConversationTimestampIndex() {
        SQLiteDatabase.create(null).use { db ->
            db.execSQL(
                "CREATE TABLE ${DatabaseHelper.MESSAGES_TABLE} (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "${MessageColumns.CONVERSATION_ID} INTEGER, " +
                    "${MessageColumns.RECEIVED_TIMESTAMP} INTEGER)",
            )

            DatabaseUpgradeHelper().upgradeToVersion5(db)

            assertTrue(
                db.hasIndex("index_${DatabaseHelper.MESSAGES_TABLE}_conversation_timestamp"),
            )
        }
    }

    @Test
    fun upgradeToVersion5_whenTheIndexCannotBeCreated_stillReachesVersion5() {
        SQLiteDatabase.create(null).use { db ->
            // No messages table: execSQL throws exactly as it would with no room left on the disk.
            assertEquals(5, DatabaseUpgradeHelper().upgradeToVersion5(db))
            assertFalse(
                db.hasIndex("index_${DatabaseHelper.MESSAGES_TABLE}_conversation_timestamp"),
            )
        }
    }

    @Test
    fun onDowngrade_fromACompatibleVersion_keepsTheRowsAndRecreatesThisVersionsSchemaObjects() {
        every {
            databaseCompatibility.canKeepRowsOnDowngradeFrom(newerVersion = currentVersion + 1)
        } returns true

        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            val expectedTriggers = db.namesOf(type = "trigger")
            val expectedIndexes = db.namesOf(type = "index")
            db.insertBlockedAndArchivedConversation()
            db.execSQL(ADD_FUTURE_COLUMN_SQL)
            db.execSQL("CREATE TABLE future_labels (_id INTEGER PRIMARY KEY, name TEXT)")
            db.execSQL("INSERT INTO future_labels (_id, name) VALUES (1, 'Bills')")
            db.execSQL(
                "CREATE INDEX future_index ON " +
                    "${DatabaseHelper.CONVERSATIONS_TABLE}($FUTURE_COLUMN)",
            )
            db.execSQL("DROP VIEW ${DatabaseHelper.DRAFT_PARTS_VIEW}")
            db.execSQL("CREATE VIEW ${DatabaseHelper.DRAFT_PARTS_VIEW} AS SELECT 1 AS future")
            db.execSQL(
                "CREATE TRIGGER future_trigger AFTER INSERT ON " +
                    "${DatabaseHelper.CONVERSATIONS_TABLE} BEGIN SELECT 1; END",
            )

            db.downgradeFromNewerVersion()

            assertEquals(
                1,
                db.countRows(
                    DatabaseHelper.PARTICIPANTS_TABLE,
                    where = "${ParticipantColumns.BLOCKED}=1",
                ),
            )
            assertEquals(
                1,
                db.countRows(
                    DatabaseHelper.CONVERSATIONS_TABLE,
                    where = "${ConversationColumns.ARCHIVE_STATUS}=1",
                ),
            )
            assertEquals(1, db.countRows(DatabaseHelper.MESSAGES_TABLE))
            assertTrue(db.hasColumn(DatabaseHelper.CONVERSATIONS_TABLE, FUTURE_COLUMN))
            assertEquals(1, db.countRows("future_labels"))
            assertEquals(expectedIndexes, db.namesOf(type = "index"))
            assertEquals(expectedTriggers, db.namesOf(type = "trigger"))
            assertTrue(
                db.hasColumn(DatabaseHelper.DRAFT_PARTS_VIEW, MessageColumns.CONVERSATION_ID),
            )
        }
    }

    @Test
    fun upgradingAgainAfterADowngradeThatKeptTheRows_keepsThem() {
        val oldestCompatibleVersion =
            DatabaseCompatibilityImpl.OLDEST_COMPATIBLE_VERSIONS[currentVersion] ?: currentVersion

        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            db.insertBlockedAndArchivedConversation()

            DatabaseUpgradeHelper().doUpgradeWithExceptions(
                db,
                oldestCompatibleVersion,
                currentVersion,
            )

            assertEquals(1, db.countRows(DatabaseHelper.CONVERSATIONS_TABLE))
            assertEquals(1, db.countRows(DatabaseHelper.MESSAGES_TABLE))
        }
    }

    @Test
    fun onDowngrade_fromAnIncompatibleVersion_rebuildsTheTables() {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            db.insertBlockedAndArchivedConversation()
            db.execSQL(ADD_FUTURE_COLUMN_SQL)

            db.downgradeFromNewerVersion()

            assertEquals(0, db.countRows(DatabaseHelper.CONVERSATIONS_TABLE))
            assertFalse(db.hasColumn(DatabaseHelper.CONVERSATIONS_TABLE, FUTURE_COLUMN))
        }
    }

    @Test
    fun onDowngrade_whenThisVersionsViewsCannotBeCreated_rebuildsTheTables() {
        every {
            databaseCompatibility.canKeepRowsOnDowngradeFrom(newerVersion = currentVersion + 1)
        } returns true

        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            db.insertBlockedAndArchivedConversation()
            db.execSQL("DROP VIEW ${DatabaseHelper.DRAFT_PARTS_VIEW}")
            db.execSQL("CREATE TABLE ${DatabaseHelper.DRAFT_PARTS_VIEW} (future INT)")

            db.downgradeFromNewerVersion()

            assertEquals(0, db.countRows(DatabaseHelper.CONVERSATIONS_TABLE))
            assertTrue(
                db.hasColumn(DatabaseHelper.DRAFT_PARTS_VIEW, MessageColumns.CONVERSATION_ID),
            )
        }
    }

    private fun SQLiteDatabase.insertConversation(): Long {
        return insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(ConversationColumns.NAME to "Weekend plan"),
        )
    }

    private fun SQLiteDatabase.insertBlockedAndArchivedConversation(): Long {
        val participantId = insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.SUB_ID to ParticipantData.OTHER_THAN_SELF_SUB_ID,
                ParticipantColumns.NORMALIZED_DESTINATION to BLOCKED_DESTINATION,
                ParticipantColumns.SEND_DESTINATION to BLOCKED_DESTINATION,
                ParticipantColumns.BLOCKED to 1,
            ),
        )
        val conversationId = insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.NAME to "Spam",
                ConversationColumns.ARCHIVE_STATUS to 1,
                ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION to
                    BLOCKED_DESTINATION,
            ),
        )
        insert(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ConversationParticipantsColumns.CONVERSATION_ID to conversationId,
                ConversationParticipantsColumns.PARTICIPANT_ID to participantId,
            ),
        )
        insert(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to participantId,
            ),
        )
        return conversationId
    }

    private fun SQLiteDatabase.downgradeFromNewerVersion() {
        setForeignKeyConstraintsEnabled(true)
        beginTransaction()
        try {
            DatabaseUpgradeHelper().onDowngrade(this, currentVersion + 1, currentVersion)
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun SQLiteDatabase.namesOf(type: String): Set<String> {
        return rawQuery(
            "SELECT name FROM sqlite_master WHERE type=? AND name NOT LIKE 'sqlite_%'",
            arrayOf(type),
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }
    }

    private fun SQLiteDatabase.hasColumn(table: String, column: String): Boolean {
        return rawQuery("SELECT * FROM $table LIMIT 0", null).use { cursor ->
            cursor.getColumnIndex(column) != -1
        }
    }

    private fun SQLiteDatabase.countRows(table: String, where: String = "1"): Int {
        return rawQuery("SELECT COUNT(*) FROM $table WHERE $where", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private fun SQLiteDatabase.hasIndex(name: String): Boolean {
        return rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name=?",
            arrayOf(name),
        ).use(Cursor::moveToFirst)
    }

    private companion object {
        const val BLOCKED_DESTINATION = "+15550100"
        const val FUTURE_COLUMN = "future_flag"
        const val ADD_FUTURE_COLUMN_SQL = "ALTER TABLE ${DatabaseHelper.CONVERSATIONS_TABLE} " +
            "ADD COLUMN $FUTURE_COLUMN INT DEFAULT(0)"
    }
}
