package com.android.messaging.data.databasecompatibility.model

internal data class RecordedDatabaseVersion(
    val databaseVersion: Int,
    val oldestCompatibleVersion: Int,
)
