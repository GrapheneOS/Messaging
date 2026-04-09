package com.android.messaging.ui.conversation.v2.messages.mapper

import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.util.LogUtil
import javax.inject.Inject

internal interface ConversationMessageUiModelMapper {
    fun map(data: ConversationMessageData): ConversationMessageUiModel
}

internal class ConversationMessageUiModelMapperImpl @Inject constructor() :
    ConversationMessageUiModelMapper {

    override fun map(data: ConversationMessageData): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = data.messageId ?: "",
            conversationId = data.conversationId ?: "",
            text = data.text,
            parts = data.parts?.map(::mapPart) ?: emptyList(),
            sentTimestamp = data.sentTimeStamp,
            receivedTimestamp = data.receivedTimeStamp,
            displayTimestamp = conversationMessageDisplayTimestamp(
                sentTimestamp = data.sentTimeStamp,
                receivedTimestamp = data.receivedTimeStamp,
                isIncoming = data.isIncoming,
            ),
            status = mapStatus(data.status),
            isIncoming = data.isIncoming,
            senderDisplayName = data.senderDisplayName,
            senderAvatarUri = data.senderProfilePhotoUri,
            senderContactLookupKey = data.senderContactLookupKey,
            canClusterWithPrevious = data.canClusterWithPreviousMessage,
            canClusterWithNext = data.canClusterWithNextMessage,
            mmsSubject = data.mmsSubject,
            protocol = mapProtocol(data),
        )
    }

    private fun mapPart(part: MessagePartData): ConversationMessagePartUiModel {
        return ConversationMessagePartUiModel(
            contentType = part.contentType ?: "",
            text = part.text,
            contentUri = part.contentUri,
            width = part.width,
            height = part.height,
        )
    }

    private fun mapStatus(javaStatus: Int): Status {
        return when (javaStatus) {
            MessageData.BUGLE_STATUS_UNKNOWN -> Status.Unknown

            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE -> Status.Outgoing.Complete
            MessageData.BUGLE_STATUS_OUTGOING_DELIVERED -> Status.Outgoing.Delivered
            MessageData.BUGLE_STATUS_OUTGOING_DRAFT -> Status.Outgoing.Draft
            MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND -> Status.Outgoing.YetToSend
            MessageData.BUGLE_STATUS_OUTGOING_SENDING -> Status.Outgoing.Sending
            MessageData.BUGLE_STATUS_OUTGOING_RESENDING -> Status.Outgoing.Resending
            MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY -> Status.Outgoing.AwaitingRetry
            MessageData.BUGLE_STATUS_OUTGOING_FAILED -> Status.Outgoing.Failed
            MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER ->
                Status.Outgoing.FailedEmergencyNumber

            MessageData.BUGLE_STATUS_INCOMING_COMPLETE -> Status.Incoming.Complete
            MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD ->
                Status.Incoming.YetToManualDownload

            MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD ->
                Status.Incoming.RetryingManualDownload

            MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING ->
                Status.Incoming.ManualDownloading

            MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD ->
                Status.Incoming.RetryingAutoDownload

            MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING -> Status.Incoming.AutoDownloading
            MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED -> Status.Incoming.DownloadFailed

            MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE ->
                Status.Incoming.ExpiredOrNotAvailable

            else -> {
                LogUtil.e(LOG_TAG, "mapStatus: unexpected value=$javaStatus")

                Status.Unknown
            }
        }
    }

    private fun mapProtocol(data: ConversationMessageData): ConversationMessageUiModel.Protocol {
        return when {
            data.isSms -> ConversationMessageUiModel.Protocol.SMS
            data.isMmsNotification -> ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION
            data.isMms -> ConversationMessageUiModel.Protocol.MMS
            else -> ConversationMessageUiModel.Protocol.UNKNOWN
        }
    }

    private fun conversationMessageDisplayTimestamp(
        sentTimestamp: Long,
        receivedTimestamp: Long,
        isIncoming: Boolean,
    ): Long {
        val primaryTimestamp = when {
            isIncoming -> receivedTimestamp
            else -> sentTimestamp
        }

        return when {
            primaryTimestamp > 0L -> primaryTimestamp
            isIncoming -> sentTimestamp
            else -> receivedTimestamp
        }
    }

    private companion object {
        private const val LOG_TAG = "ConversationMessageUiModelMapper"
    }
}
