package com.android.messaging.util.db.ext

import com.android.messaging.datamodel.DatabaseWrapper

inline fun DatabaseWrapper.withTransaction(block: () -> Unit) {
    beginTransaction()
    try {
        block()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}
