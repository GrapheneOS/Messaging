package com.android.messaging.datamodel

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseUpgradeHelperTest {

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

    private fun SQLiteDatabase.hasIndex(name: String): Boolean {
        return rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name=?",
            arrayOf(name),
        ).use(Cursor::moveToFirst)
    }
}
