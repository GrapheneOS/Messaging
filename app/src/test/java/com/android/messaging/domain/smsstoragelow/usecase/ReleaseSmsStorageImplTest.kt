package com.android.messaging.domain.smsstoragelow.usecase

import com.android.messaging.datamodel.action.HandleLowStorageAction
import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction
import com.android.messaging.domain.smsstoragelow.model.SmsStorageRetentionDuration
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReleaseSmsStorageImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun invoke_whenActionDeletesMediaMessages_startsDeleteMediaMessagesAction() {
        runTest(context = testDispatcher) {
            mockkStatic(HandleLowStorageAction::class)
            try {
                val retentionDuration = retentionDuration()
                val action = SmsStorageLowWarningAction.DeleteMediaMessages(
                    retentionDuration = retentionDuration,
                )
                every {
                    HandleLowStorageAction.handleDeleteMediaMessages(any())
                } just Runs
                every {
                    HandleLowStorageAction.handleDeleteOldMessages(any())
                } just Runs

                createUseCase().invoke(action = action).collect()

                verify(exactly = 1) {
                    HandleLowStorageAction.handleDeleteMediaMessages(retentionDuration.millis)
                }
                verify(exactly = 0) {
                    HandleLowStorageAction.handleDeleteOldMessages(any())
                }
            } finally {
                unmockkStatic(HandleLowStorageAction::class)
            }
        }
    }

    @Test
    fun invoke_whenActionDeletesOldMessages_startsDeleteOldMessagesAction() {
        runTest(context = testDispatcher) {
            mockkStatic(HandleLowStorageAction::class)
            try {
                val retentionDuration = retentionDuration()
                val action = SmsStorageLowWarningAction.DeleteOldMessages(
                    retentionDuration = retentionDuration,
                )
                every {
                    HandleLowStorageAction.handleDeleteOldMessages(any())
                } just Runs
                every {
                    HandleLowStorageAction.handleDeleteMediaMessages(any())
                } just Runs

                createUseCase().invoke(action = action).collect()

                verify(exactly = 1) {
                    HandleLowStorageAction.handleDeleteOldMessages(retentionDuration.millis)
                }
                verify(exactly = 0) {
                    HandleLowStorageAction.handleDeleteMediaMessages(any())
                }
            } finally {
                unmockkStatic(HandleLowStorageAction::class)
            }
        }
    }

    private fun createUseCase(): ReleaseSmsStorageImpl {
        return ReleaseSmsStorageImpl(ioDispatcher = testDispatcher)
    }

    private fun retentionDuration(): SmsStorageRetentionDuration {
        return SmsStorageRetentionDuration(
            count = 1,
            unit = SmsStorageRetentionDuration.DurationUnit.MONTH,
            millis = 30L * 24L * 60L * 60L * 1000L,
        )
    }
}
