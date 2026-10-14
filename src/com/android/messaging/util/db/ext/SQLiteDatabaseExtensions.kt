package com.android.messaging.util.db.ext

import android.database.sqlite.SQLiteDatabase

internal inline fun SQLiteDatabase.withTransaction(block: SQLiteDatabase.() -> Unit) {
    beginTransaction()
    runCatching {
        block()
        setTransactionSuccessful()
    }.onSuccess {
        endTransaction()
    }.onFailure { failure ->
        // A full disk can make SQLite roll back by itself, so the rollback here fails too
        runCatching { endTransaction() }.onFailure(failure::addSuppressed)
        throw failure
    }
}
