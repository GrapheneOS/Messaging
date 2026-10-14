package com.android.messaging.datamodel

import android.content.Context
import android.database.sqlite.SQLiteDatabase

internal fun createInMemoryActionSyncTestDatabase(context: Context): DatabaseWrapper {
    val sqliteDatabase = SQLiteDatabase.create(null)
    DatabaseHelper.rebuildTables(sqliteDatabase)
    // Match the connections DatabaseHelper.onConfigure() hands the app.
    sqliteDatabase.setForeignKeyConstraintsEnabled(true)

    return DatabaseWrapper(context, sqliteDatabase)
}

internal fun SQLiteDatabase.simulateFullDisk() {
    val pageCount = rawQuery("PRAGMA page_count", null).use { cursor ->
        cursor.moveToFirst()
        cursor.getLong(0)
    }
    rawQuery("PRAGMA max_page_count = $pageCount", null).use { cursor ->
        cursor.moveToFirst()
    }
}
