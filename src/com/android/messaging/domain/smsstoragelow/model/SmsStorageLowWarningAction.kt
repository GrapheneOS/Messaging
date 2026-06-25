package com.android.messaging.domain.smsstoragelow.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface SmsStorageLowWarningAction {
    val retentionDuration: SmsStorageRetentionDuration

    data class DeleteMediaMessages(
        override val retentionDuration: SmsStorageRetentionDuration,
    ) : SmsStorageLowWarningAction

    data class DeleteOldMessages(
        override val retentionDuration: SmsStorageRetentionDuration,
    ) : SmsStorageLowWarningAction
}
