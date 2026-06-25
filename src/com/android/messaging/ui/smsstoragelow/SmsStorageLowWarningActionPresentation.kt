package com.android.messaging.ui.smsstoragelow

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mms
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction
import com.android.messaging.domain.smsstoragelow.model.SmsStorageRetentionDuration

internal fun actionIcon(action: SmsStorageLowWarningAction): ImageVector {
    return when (action) {
        is SmsStorageLowWarningAction.DeleteMediaMessages -> Icons.Default.Mms
        is SmsStorageLowWarningAction.DeleteOldMessages -> Icons.Default.Delete
    }
}

@Composable
internal fun actionTitle(action: SmsStorageLowWarningAction): String {
    return when (action) {
        is SmsStorageLowWarningAction.DeleteMediaMessages -> {
            stringResource(id = R.string.delete_all_media)
        }

        is SmsStorageLowWarningAction.DeleteOldMessages -> {
            stringResource(
                id = R.string.delete_oldest_messages,
                retentionDurationText(duration = action.retentionDuration),
            )
        }
    }
}

@Composable
internal fun confirmationMessage(action: SmsStorageLowWarningAction): String {
    return when (action) {
        is SmsStorageLowWarningAction.DeleteMediaMessages -> {
            stringResource(id = R.string.delete_all_media_confirmation)
        }

        is SmsStorageLowWarningAction.DeleteOldMessages -> {
            stringResource(
                id = R.string.delete_oldest_messages_confirmation,
                retentionDurationText(duration = action.retentionDuration),
            )
        }
    }
}

@Composable
private fun retentionDurationText(duration: SmsStorageRetentionDuration): String {
    return when (duration.unit) {
        SmsStorageRetentionDuration.DurationUnit.WEEK -> {
            pluralStringResource(
                id = R.plurals.week_count,
                count = duration.count,
                duration.count,
            )
        }

        SmsStorageRetentionDuration.DurationUnit.MONTH -> {
            pluralStringResource(
                id = R.plurals.month_count,
                count = duration.count,
                duration.count,
            )
        }

        SmsStorageRetentionDuration.DurationUnit.YEAR -> {
            pluralStringResource(
                id = R.plurals.year_count,
                count = duration.count,
                duration.count,
            )
        }
    }
}
