package com.android.messaging.datamodel.action.mms

import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.sms.DatabaseMessages.LocalDatabaseMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MmsNotificationDownloadTest {

    @Test
    fun mmsNotificationDownloadStatuses_containsOnlyActiveDownloadStatuses() {
        assertArrayEquals(
            intArrayOf(
                MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD,
                MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING,
                MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD,
                MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            ),
            mmsNotificationDownloadStatuses,
        )
    }

    @Test
    fun isMmsNotificationExpired_returnsTrueForPastPositiveExpiry() {
        val result = isMmsNotificationExpired(
            mmsExpiry = 999L,
            nowMillis = 1_000L,
        )

        assertTrue(result)
    }

    @Test
    fun isMmsNotificationExpired_returnsFalseForFutureExpiry() {
        val result = isMmsNotificationExpired(
            mmsExpiry = 1_001L,
            nowMillis = 1_000L,
        )

        assertFalse(result)
    }

    @Test
    fun isMmsNotificationExpired_returnsFalseForUnknownExpiry() {
        val result = isMmsNotificationExpired(
            mmsExpiry = 0L,
            nowMillis = 1_000L,
        )

        assertFalse(result)
    }

    @Test
    fun shouldProtectMmsNotificationDownload_returnsTrueForUnexpiredDownloadNotification() {
        val result = shouldProtectMmsNotificationDownload(
            localMessage = localMessage(
                protocol = MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION,
                status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
                mmsExpiry = Long.MAX_VALUE,
            ),
        )

        assertTrue(result)
    }

    @Test
    fun shouldProtectMmsNotificationDownload_returnsFalseForExpiredDownloadNotification() {
        val result = shouldProtectMmsNotificationDownload(
            localMessage = localMessage(
                protocol = MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION,
                status = MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
                mmsExpiry = 1L,
            ),
        )

        assertFalse(result)
    }

    @Test
    fun shouldProtectMmsNotificationDownload_returnsFalseForCompletedMms() {
        val result = shouldProtectMmsNotificationDownload(
            localMessage = localMessage(
                protocol = MessageData.PROTOCOL_MMS,
                status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                mmsExpiry = Long.MAX_VALUE,
            ),
        )

        assertFalse(result)
    }

    @Test
    fun shouldProtectMmsNotificationDownload_returnsFalseForYetToManualDownloadNotification() {
        val result = shouldProtectMmsNotificationDownload(
            localMessage = localMessage(
                protocol = MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION,
                status = MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD,
                mmsExpiry = Long.MAX_VALUE,
            ),
        )

        assertFalse(result)
    }

    @Test
    fun shouldProtectMmsNotificationDownload_returnsFalseForTerminalNotificationStatuses() {
        listOf(
            MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED,
            MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE,
        ).forEach { status ->
            val result = shouldProtectMmsNotificationDownload(
                localMessage = localMessage(
                    protocol = MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION,
                    status = status,
                    mmsExpiry = Long.MAX_VALUE,
                ),
            )

            assertFalse("Status $status should not be protected", result)
        }
    }

    private fun localMessage(
        protocol: Int,
        status: Int,
        mmsExpiry: Long,
    ): LocalDatabaseMessage {
        return LocalDatabaseMessage(
            1L,
            protocol,
            "content://mms/1",
            1L,
            "conversation",
            status,
            mmsExpiry,
        )
    }
}
