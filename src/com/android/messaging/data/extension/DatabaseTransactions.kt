package com.android.messaging.data.extension

import com.android.messaging.datamodel.DataModel
import com.android.messaging.util.db.ext.withTransaction

internal fun runInTransaction(block: () -> Unit) {
    DataModel.get().database.withTransaction(block)
}
