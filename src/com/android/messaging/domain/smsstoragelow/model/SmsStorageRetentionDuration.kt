package com.android.messaging.domain.smsstoragelow.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SmsStorageRetentionDuration(
    val count: Int,
    val unit: DurationUnit,
    val millis: Long,
) {

    internal enum class DurationUnit {
        WEEK,
        MONTH,
        YEAR,
    }
}
