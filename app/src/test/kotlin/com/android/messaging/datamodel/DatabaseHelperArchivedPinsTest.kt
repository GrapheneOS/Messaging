package com.android.messaging.datamodel

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.testutil.installTestFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DatabaseHelperArchivedPinsTest {

    @Before
    fun setUp() {
        installTestFactory(context = applicationContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun onOpen_unpinsArchivedConversationsOnly() {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            val archivedPinnedId = db.insertConversation(isArchived = true, isPinned = true)
            val archivedId = db.insertConversation(isArchived = true, isPinned = false)
            val pinnedId = db.insertConversation(isArchived = false, isPinned = true)
            val plainId = db.insertConversation(isArchived = false, isPinned = false)

            DatabaseHelper.getInstance(applicationContext).onOpen(db)

            assertEquals(
                mapOf(
                    archivedPinnedId to (true to false),
                    archivedId to (true to false),
                    pinnedId to (false to true),
                    plainId to (false to false),
                ),
                db.archivedAndPinnedById(),
            )
        }
    }

    /**
     * A failure here must cost nothing but the unpinning: run as an upgrade step, a full disk
     * rolled back the upgrade transaction and the fallback rebuilt every table.
     */
    @Test
    fun onOpen_onAFullDisk_keepsEveryRowAndUnpinsOnTheNextOpen() {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            db.setForeignKeyConstraintsEnabled(true)
            db.insertConversationsAndMessage()
            val pinnedBefore = db.countRows(where = "${ConversationColumns.PINNED}=1")
            val archivedPinnedBefore = db.countRows(where = ARCHIVED_AND_PINNED)
            db.simulateFullDisk()

            DatabaseHelper.getInstance(applicationContext).onOpen(db)

            assertEquals(FULL_DISK_CONVERSATION_COUNT, db.countRows())
            assertEquals(1, db.countRows(table = DatabaseHelper.MESSAGES_TABLE))
            assertEquals(
                "the fixture no longer fills the disk, so the failure path never ran",
                archivedPinnedBefore,
                db.countRows(where = ARCHIVED_AND_PINNED),
            )

            db.freeDisk()
            DatabaseHelper.getInstance(applicationContext).onOpen(db)

            assertEquals(0, db.countRows(where = ARCHIVED_AND_PINNED))
            assertEquals(
                pinnedBefore - archivedPinnedBefore,
                db.countRows(where = "${ConversationColumns.PINNED}=1"),
            )
        }
    }

    private val applicationContext
        get() = RuntimeEnvironment.getApplication().applicationContext

    private fun SQLiteDatabase.insertConversation(isArchived: Boolean, isPinned: Boolean): Long {
        return insertOrThrow(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.ARCHIVE_STATUS to isArchived,
                ConversationColumns.PINNED to isPinned,
            ),
        )
    }

    private fun SQLiteDatabase.insertConversationsAndMessage() {
        beginTransaction()
        try {
            for (index in 1..FULL_DISK_CONVERSATION_COUNT) {
                val conversationId = insertConversation(
                    isArchived = index % 3 == 0,
                    isPinned = index % 2 == 0,
                )
                if (index == 1) {
                    insertOrThrow(
                        DatabaseHelper.MESSAGES_TABLE,
                        null,
                        contentValuesOf(MessageColumns.CONVERSATION_ID to conversationId),
                    )
                }
            }
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun SQLiteDatabase.freeDisk() {
        rawQuery("PRAGMA max_page_count = $UNLIMITED_PAGE_COUNT", null).use { it.moveToFirst() }
    }

    private fun SQLiteDatabase.archivedAndPinnedById(): Map<Long, Pair<Boolean, Boolean>> {
        return rawQuery(
            "SELECT _id, ${ConversationColumns.ARCHIVE_STATUS}, ${ConversationColumns.PINNED}" +
                " FROM ${DatabaseHelper.CONVERSATIONS_TABLE}",
            null,
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    put(cursor.getLong(0), (cursor.getInt(1) == 1) to (cursor.getInt(2) == 1))
                }
            }
        }
    }

    private fun SQLiteDatabase.countRows(
        table: String = DatabaseHelper.CONVERSATIONS_TABLE,
        where: String = "1",
    ): Int {
        return rawQuery("SELECT COUNT(*) FROM $table WHERE $where", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private companion object {
        // Fewer rows leave the update enough free pages to succeed.
        const val FULL_DISK_CONVERSATION_COUNT = 5000
        const val UNLIMITED_PAGE_COUNT = 1_073_741_823
        const val ARCHIVED_AND_PINNED =
            "${ConversationColumns.ARCHIVE_STATUS}=1 AND ${ConversationColumns.PINNED}=1"
    }
}
