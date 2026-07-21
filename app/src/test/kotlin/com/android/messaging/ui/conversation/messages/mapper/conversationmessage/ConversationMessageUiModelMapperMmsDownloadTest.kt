package com.android.messaging.ui.conversation.messages.mapper.conversationmessage

import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import com.android.messaging.util.OsUtil
import io.mockk.every
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageUiModelMapperMmsDownloadTest :
    BaseConversationMessageUiModelMapperTest() {

    @Test
    fun map_awaitingManualDownloadStatus_buildsMmsDownloadWithSizeAndExpiry() {
        val uiModel = mapper.map(
            messageData(
                status = MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD,
                smsMessageSize = 2_048,
                mmsExpiry = 1_700_000_000_000L,
            ),
        )

        assertEquals(
            MmsDownloadUiModel(
                state = MmsDownloadUiModel.State.AwaitingManualDownload,
                sizeBytes = 2_048L,
                expiryTimestamp = 1_700_000_000_000L,
                isSecondaryUser = false,
            ),
            uiModel.mmsDownload,
        )
    }

    @Test
    fun map_awaitingManualDownloadStatusAsSecondaryUser_buildsSecondaryUserMmsDownload() {
        every { OsUtil.isSecondaryUser() } returns true

        val uiModel = mapper.map(
            messageData(
                status = MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD,
                smsMessageSize = 2_048,
                mmsExpiry = 1_700_000_000_000L,
            ),
        )

        assertEquals(
            MmsDownloadUiModel(
                state = MmsDownloadUiModel.State.AwaitingManualDownload,
                sizeBytes = 2_048L,
                expiryTimestamp = 1_700_000_000_000L,
                isSecondaryUser = true,
            ),
            uiModel.mmsDownload,
        )
    }

    @Test
    fun map_downloadingStatuses_buildDownloadingMmsDownload() {
        val downloadingStatuses = listOf(
            MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING,
            MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD,
            MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD,
        )

        downloadingStatuses.forEach { status ->
            val uiModel = mapper.map(messageData(status = status))

            assertEquals(
                "status=$status",
                MmsDownloadUiModel.State.Downloading,
                uiModel.mmsDownload?.state,
            )
        }
    }

    @Test
    fun map_downloadFailedStatus_buildsDownloadFailedMmsDownload() {
        val uiModel = mapper.map(
            messageData(status = MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED),
        )

        assertEquals(
            MmsDownloadUiModel.State.DownloadFailed,
            uiModel.mmsDownload?.state,
        )
    }

    @Test
    fun map_expiredStatus_buildsExpiredOrUnavailableMmsDownload() {
        val uiModel = mapper.map(
            messageData(status = MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE),
        )

        assertEquals(
            MmsDownloadUiModel.State.ExpiredOrUnavailable,
            uiModel.mmsDownload?.state,
        )
    }

    @Test
    fun map_statusWithoutDownloadState_hasNoMmsDownload() {
        val nonDownloadStatuses = listOf(
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
            MessageData.BUGLE_STATUS_UNKNOWN,
        )

        nonDownloadStatuses.forEach { status ->
            val uiModel = mapper.map(messageData(status = status))

            assertNull("status=$status", uiModel.mmsDownload)
        }
    }
}
