package com.android.messaging.domain.smsstoragelow.mapper

import com.android.messaging.domain.smsstoragelow.model.SmsStorageRetentionDuration
import com.android.messaging.sms.SmsReleaseStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SmsStorageRetentionDurationMapperImplTest {

    private val mapper = SmsStorageRetentionDurationMapperImpl()

    @Test
    fun map_whenDurationIsWeek_returnsWeekDuration() {
        val duration = SmsReleaseStorage.Duration(
            2,
            SmsReleaseStorage.Duration.UNIT_WEEK,
        )

        assertEquals(
            SmsStorageRetentionDuration(
                count = 2,
                unit = SmsStorageRetentionDuration.DurationUnit.WEEK,
                millis = 2L * 7L * 24L * 3600L * 1000L,
            ),
            mapper.map(duration = duration),
        )
    }

    @Test
    fun map_whenDurationIsMonth_returnsMonthDuration() {
        val duration = SmsReleaseStorage.Duration(
            3,
            SmsReleaseStorage.Duration.UNIT_MONTH,
        )

        assertEquals(
            SmsStorageRetentionDuration(
                count = 3,
                unit = SmsStorageRetentionDuration.DurationUnit.MONTH,
                millis = 3L * 30L * 24L * 3600L * 1000L,
            ),
            mapper.map(duration = duration),
        )
    }

    @Test
    fun map_whenDurationIsYear_returnsYearDuration() {
        val duration = SmsReleaseStorage.Duration(
            4,
            SmsReleaseStorage.Duration.UNIT_YEAR,
        )

        assertEquals(
            SmsStorageRetentionDuration(
                count = 4,
                unit = SmsStorageRetentionDuration.DurationUnit.YEAR,
                millis = 4L * 365L * 24L * 3600L * 1000L,
            ),
            mapper.map(duration = duration),
        )
    }

    @Test
    fun map_whenDurationUnitIsInvalid_throws() {
        val duration = SmsReleaseStorage.Duration(
            1,
            'd'.code,
        )

        assertThrows(IllegalArgumentException::class.java) {
            mapper.map(duration = duration)
        }
    }
}
