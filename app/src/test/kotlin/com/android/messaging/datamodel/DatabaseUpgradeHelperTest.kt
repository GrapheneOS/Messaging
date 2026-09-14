package com.android.messaging.datamodel

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.testutil.installTestFactory
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DatabaseUpgradeHelperTest {

    @Before
    fun setUp() {
        installTestFactory(context = RuntimeEnvironment.getApplication().applicationContext)
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
        val context = RuntimeEnvironment.getApplication().applicationContext
        val currentVersion = context.getString(R.string.database_version).toInt()

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

    private fun SQLiteDatabase.hasColumn(table: String, column: String): Boolean {
        return rawQuery("SELECT * FROM $table LIMIT 0", null).use { cursor ->
            cursor.getColumnIndex(column) != -1
        }
    }

    private fun SQLiteDatabase.countRows(table: String): Int {
        return rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
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
}
