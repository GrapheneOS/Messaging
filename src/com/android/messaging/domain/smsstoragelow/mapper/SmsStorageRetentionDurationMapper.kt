package com.android.messaging.domain.smsstoragelow.mapper

import com.android.messaging.domain.smsstoragelow.model.SmsStorageRetentionDuration
import com.android.messaging.sms.SmsReleaseStorage
import javax.inject.Inject

internal interface SmsStorageRetentionDurationMapper {
    fun map(duration: SmsReleaseStorage.Duration): SmsStorageRetentionDuration
}

internal class SmsStorageRetentionDurationMapperImpl @Inject constructor() :
    SmsStorageRetentionDurationMapper {

    override fun map(duration: SmsReleaseStorage.Duration): SmsStorageRetentionDuration {
        return SmsStorageRetentionDuration(
            count = duration.mCount,
            unit = mapUnit(unit = duration.mUnit),
            millis = SmsReleaseStorage.durationToTimeInMillis(duration),
        )
    }

    private fun mapUnit(unit: Int): SmsStorageRetentionDuration.DurationUnit {
        return when (unit) {
            SmsReleaseStorage.Duration.UNIT_WEEK -> {
                SmsStorageRetentionDuration.DurationUnit.WEEK
            }

            SmsReleaseStorage.Duration.UNIT_MONTH -> {
                SmsStorageRetentionDuration.DurationUnit.MONTH
            }

            SmsReleaseStorage.Duration.UNIT_YEAR -> {
                SmsStorageRetentionDuration.DurationUnit.YEAR
            }

            else -> {
                throw IllegalArgumentException("Invalid storage duration unit $unit")
            }
        }
    }
}
