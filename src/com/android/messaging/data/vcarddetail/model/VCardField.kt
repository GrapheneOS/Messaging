package com.android.messaging.data.vcarddetail.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class VCardField(
    val value: String,
    val label: String?,
    val action: VCardFieldAction,
)
