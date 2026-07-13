package com.android.messaging.ui.conversation.messages.mapper.conversationmessage

import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageUiModelMapperStatusTest :
    BaseConversationMessageUiModelMapperTest() {

    @Test
    fun map_mapsEachBugleStatusToMatchingUiStatus() {
        val statusToUiStatus = mapOf(
            MessageData.BUGLE_STATUS_UNKNOWN to Status.Unknown,
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE to Status.Outgoing.Complete,
            MessageData.BUGLE_STATUS_OUTGOING_DELIVERED to Status.Outgoing.Delivered,
            MessageData.BUGLE_STATUS_OUTGOING_DRAFT to Status.Outgoing.Draft,
            MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND to Status.Outgoing.YetToSend,
            MessageData.BUGLE_STATUS_OUTGOING_SENDING to Status.Outgoing.Sending,
            MessageData.BUGLE_STATUS_OUTGOING_RESENDING to Status.Outgoing.Resending,
            MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY to Status.Outgoing.AwaitingRetry,
            MessageData.BUGLE_STATUS_OUTGOING_FAILED to Status.Outgoing.Failed,
            MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER to
                Status.Outgoing.FailedEmergencyNumber,
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE to Status.Incoming.Complete,
            MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD to
                Status.Incoming.YetToManualDownload,
            MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD to
                Status.Incoming.RetryingManualDownload,
            MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING to
                Status.Incoming.ManualDownloading,
            MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD to
                Status.Incoming.RetryingAutoDownload,
            MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING to Status.Incoming.AutoDownloading,
            MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED to Status.Incoming.DownloadFailed,
            MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE to
                Status.Incoming.ExpiredOrNotAvailable,
        )

        statusToUiStatus.forEach { (bugleStatus, expectedUiStatus) ->
            val uiModel = mapper.map(
                messageData(status = bugleStatus),
                isYouTubePreviewEnabled = false,
            )

            assertEquals("status=$bugleStatus", expectedUiStatus, uiModel.status)
        }
    }

    @Test
    fun map_withUnexpectedStatus_mapsToUnknown() {
        val uiModel = mapper.map(messageData(status = 9999), isYouTubePreviewEnabled = false)

        assertEquals(Status.Unknown, uiModel.status)
    }
}
