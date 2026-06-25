package com.android.messaging.domain.smsstoragelow.usecase

import com.android.messaging.sms.SmsStorageStatusManager
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

class CancelSmsStorageLowNotificationImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun invoke_cancelsSmsStorageLowNotification() {
        runTest(context = testDispatcher) {
            mockkStatic(SmsStorageStatusManager::class)
            try {
                every {
                    SmsStorageStatusManager.cancelStorageLowNotification()
                } just Runs

                createUseCase().invoke().collect()

                verify(exactly = 1) {
                    SmsStorageStatusManager.cancelStorageLowNotification()
                }
            } finally {
                unmockkStatic(SmsStorageStatusManager::class)
            }
        }
    }

    private fun createUseCase(): CancelSmsStorageLowNotificationImpl {
        return CancelSmsStorageLowNotificationImpl(ioDispatcher = testDispatcher)
    }
}
