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
