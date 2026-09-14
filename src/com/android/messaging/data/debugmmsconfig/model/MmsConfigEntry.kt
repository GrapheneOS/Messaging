package com.android.messaging.data.debugmmsconfig.model

internal data class MmsConfigEntry(
    val key: String,
    val keyType: MmsConfigKeyType,
    val value: String,
)
