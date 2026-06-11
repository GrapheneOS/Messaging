package com.android.messaging.util.db

import android.database.MatrixCursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReversedCursorTest {

    @Test
    fun reversedCursor_iteratesRowsInReverseOrder() {
        val reversedCursor = createReversedCursor(
            rows = listOf("first", "second", "third"),
        )

        reversedCursor.use { cursor ->
            assertTrue(cursor.isBeforeFirst)
            assertEquals(-1, cursor.position)

            assertTrue(cursor.moveToNext())
            assertEquals("third", cursor.getString(VALUE_COLUMN_INDEX))
            assertTrue(cursor.isFirst)
            assertFalse(cursor.isLast)
            assertEquals(0, cursor.position)

            assertTrue(cursor.moveToNext())
            assertEquals("second", cursor.getString(VALUE_COLUMN_INDEX))
            assertFalse(cursor.isFirst)
            assertFalse(cursor.isLast)
            assertEquals(1, cursor.position)

            assertTrue(cursor.moveToNext())
            assertEquals("first", cursor.getString(VALUE_COLUMN_INDEX))
            assertFalse(cursor.isFirst)
            assertTrue(cursor.isLast)
            assertEquals(2, cursor.position)

            assertFalse(cursor.moveToNext())
            assertTrue(cursor.isAfterLast)
            assertEquals(3, cursor.position)
        }
    }

    @Test
    fun reversedCursor_supportsRandomAccessAndReverseNavigation() {
        val reversedCursor = createReversedCursor(
            rows = listOf("first", "second", "third"),
        )

        reversedCursor.use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("third", cursor.getString(VALUE_COLUMN_INDEX))

            assertTrue(cursor.moveToLast())
            assertEquals("first", cursor.getString(VALUE_COLUMN_INDEX))

            assertTrue(cursor.moveToPosition(1))
            assertEquals("second", cursor.getString(VALUE_COLUMN_INDEX))
            assertEquals(1, cursor.position)

            assertTrue(cursor.move(1))
            assertEquals("first", cursor.getString(VALUE_COLUMN_INDEX))
            assertTrue(cursor.isLast)

            assertTrue(cursor.move(-2))
            assertEquals("third", cursor.getString(VALUE_COLUMN_INDEX))
            assertTrue(cursor.isFirst)

            assertTrue(cursor.moveToPosition(0))
            assertFalse(cursor.moveToPrevious())
            assertTrue(cursor.isBeforeFirst)
            assertEquals(-1, cursor.position)
        }
    }

    @Test
    fun reversedCursor_handlesEmptyCursor() {
        val reversedCursor = createReversedCursor(rows = emptyList())

        reversedCursor.use { cursor ->
            assertTrue(cursor.isBeforeFirst)
            assertFalse(cursor.moveToFirst())
            assertFalse(cursor.moveToNext())
            assertTrue(cursor.isAfterLast)
            assertEquals(0, cursor.position)
        }
    }

    private fun createReversedCursor(rows: List<String>): ReversedCursor {
        val cursor = MatrixCursor(arrayOf(VALUE_COLUMN_NAME))
        for (value in rows) {
            cursor.addRow(arrayOf(value))
        }
        return ReversedCursor(cursor = cursor)
    }

    private companion object {
        private const val VALUE_COLUMN_NAME = "value"
        private const val VALUE_COLUMN_INDEX = 0
    }
}
