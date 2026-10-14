package com.android.messaging.util.db.ext

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.core.content.contentValuesOf
import com.android.messaging.datamodel.simulateFullDisk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SQLiteDatabaseExtensionsTest {

    private lateinit var database: SQLiteDatabase

    @Before
    fun setUp() {
        database = SQLiteDatabase.create(null)
        database.execSQL("CREATE TABLE $TABLE ($COLUMN TEXT)")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aCompletedBlock_isCommitted() {
        database.withTransaction {
            insertNote(text = NOTE)
        }

        assertEquals(1, database.noteCount())
    }

    @Test
    fun aFailedBlock_isRolledBackAndRethrown() {
        val failure = IllegalStateException(NOTE)

        val thrown = assertThrows(IllegalStateException::class.java) {
            database.withTransaction {
                insertNote(text = NOTE)
                throw failure
            }
        }

        assertSame(failure, thrown)
        assertEquals(0, database.noteCount())
    }

    @Test
    fun aFullDisk_throwsTheFullDiskRatherThanTheFailedRollback() {
        database.simulateFullDisk()

        val thrown = assertThrows(SQLiteFullException::class.java) {
            database.withTransaction {
                insertNote(text = NOTE)
                insertNote(text = NOTE.repeat(OVERSIZED_NOTE_REPEATS))
            }
        }

        assertTrue(thrown.suppressed.single() is SQLiteException)
        assertEquals(0, database.noteCount())
    }

    private fun SQLiteDatabase.insertNote(text: String) {
        insertOrThrow(TABLE, null, contentValuesOf(COLUMN to text))
    }

    private fun SQLiteDatabase.noteCount(): Int {
        return rawQuery("SELECT COUNT(*) FROM $TABLE", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private companion object {
        private const val TABLE = "notes"
        private const val COLUMN = "text"
        private const val NOTE = "note"
        private const val OVERSIZED_NOTE_REPEATS = 20_000
    }
}
