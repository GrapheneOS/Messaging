package com.android.messaging.util.db.ext

import android.database.Cursor
import androidx.core.database.getStringOrNull

fun Cursor.getStringOrNull(columnName: String): String? {
    return getColumnIndexOrThrow(columnName)
        .let(::getStringOrNull)
}

fun Cursor.getStringOrEmpty(columnName: String): String {
    return getStringOrNull(columnName = columnName).orEmpty()
}

fun Cursor.getInt(columnName: String): Int {
    return getColumnIndexOrThrow(columnName)
        .let(::getInt)
}
