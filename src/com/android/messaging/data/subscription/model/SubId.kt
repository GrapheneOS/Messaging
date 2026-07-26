package com.android.messaging.data.subscription.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
internal value class SubId(
    val value: Int,
)
