package com.android.messaging.datamodel

import android.content.Context
import android.database.sqlite.SQLiteDatabase

internal fun createInMemoryActionSyncTestDatabase(context: Context): DatabaseWrapper {
    val sqliteDatabase = SQLiteDatabase.create(null)
    DatabaseHelper.rebuildTables(sqliteDatabase)

    return DatabaseWrapper(context, sqliteDatabase)
}
