package com.android.messaging.domain.smsstoragelow.usecase

import com.android.messaging.domain.smsstoragelow.mapper.SmsStorageRetentionDurationMapper
import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction
import com.android.messaging.domain.smsstoragelow.model.SmsStorageRetentionDuration
import com.android.messaging.sms.SmsReleaseStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSmsStorageLowWarningActionsImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mapper = mockk<SmsStorageRetentionDurationMapper>()

    @Test
    fun invoke_returnsMediaAndOldMessageActionsWithMappedRetentionDuration() {
        runTest(context = testDispatcher) {
            mockkStatic(SmsReleaseStorage::class)
            try {
                val legacyDuration = SmsReleaseStorage.Duration(
                    2,
                    SmsReleaseStorage.Duration.UNIT_WEEK,
                )
                val retentionDuration = SmsStorageRetentionDuration(
                    count = 2,
                    unit = SmsStorageRetentionDuration.DurationUnit.WEEK,
                    millis = 2L * 7L * 24L * 3600L * 1000L,
                )
                every { SmsReleaseStorage.parseMessageRetainingDuration() } returns legacyDuration
                every { mapper.map(duration = legacyDuration) } returns retentionDuration

                val actions = createUseCase().invoke().first()

                assertEquals(
                    listOf(
                        SmsStorageLowWarningAction.DeleteMediaMessages(
                            retentionDuration = retentionDuration,
                        ),
                        SmsStorageLowWarningAction.DeleteOldMessages(
                            retentionDuration = retentionDuration,
                        ),
                    ),
                    actions,
                )
                verify(exactly = 1) {
                    SmsReleaseStorage.parseMessageRetainingDuration()
                }
                verify(exactly = 1) {
                    mapper.map(duration = legacyDuration)
                }
            } finally {
                unmockkStatic(SmsReleaseStorage::class)
            }
        }
    }

    private fun createUseCase(): GetSmsStorageLowWarningActionsImpl {
        return GetSmsStorageLowWarningActionsImpl(
            smsStorageRetentionDurationMapper = mapper,
            ioDispatcher = testDispatcher,
        )
    }
}
