package com.android.messaging.domain.notification.usecase

import org.junit.Assert.assertNotEquals
import org.junit.Test

internal class GenerateNotificationIdImplTest {

    @Test
    fun invoke_sameElapsedRealtime_returnsDifferentIds() {
        val generateNotificationId = GenerateNotificationIdImpl { ELAPSED_REALTIME_MILLIS }

        val firstId = generateNotificationId()
        val secondId = generateNotificationId()

        assertNotEquals(firstId, secondId)
    }

    private companion object {
        private const val ELAPSED_REALTIME_MILLIS = 123_456L
    }
}
