package com.android.messaging.util.db.ext

import android.database.Cursor
import androidx.core.database.getStringOrNull

fun Cursor.getStringOrNull(columnName: String): String? {
    return getColumnIndexOrThrow(columnName)
        .let(::getStringOrNull)
}

fun Cursor.getNonBlankStringOrNull(columnIndex: Int): String? {
    return getStringOrNull(columnIndex)?.takeIf { it.isNotBlank() }
}

fun Cursor.getStringOrEmpty(columnName: String): String {
    return getStringOrNull(columnName = columnName).orEmpty()
}

fun Cursor.getInt(columnName: String): Int {
    return getColumnIndexOrThrow(columnName)
        .let(::getInt)
}

fun Cursor.getLong(columnName: String): Long {
    return getColumnIndexOrThrow(columnName)
        .let(::getLong)
}
