package com.android.messaging.util.db

import android.database.Cursor
import android.database.CursorWrapper

/**
 * Presents a cursor in reverse row order while preserving standard cursor navigation semantics
 */
internal class ReversedCursor(
    cursor: Cursor,
) : CursorWrapper(cursor) {
    private val count: Int = cursor.count

    init {
        cursor.moveToPosition(count)
    }

    override fun moveToPosition(position: Int): Boolean {
        return super.moveToPosition(count - position - 1)
    }

    override fun getPosition(): Int {
        return count - super.getPosition() - 1
    }

    override fun isAfterLast(): Boolean {
        return super.isBeforeFirst
    }

    override fun isBeforeFirst(): Boolean {
        return super.isAfterLast
    }

    override fun isFirst(): Boolean {
        return super.isLast
    }

    override fun isLast(): Boolean {
        return super.isFirst
    }

    override fun move(offset: Int): Boolean {
        return super.move(-offset)
    }

    override fun moveToFirst(): Boolean {
        return super.moveToLast()
    }

    override fun moveToLast(): Boolean {
        return super.moveToFirst()
    }

    override fun moveToNext(): Boolean {
        return super.moveToPrevious()
    }

    override fun moveToPrevious(): Boolean {
        return super.moveToNext()
    }
}
