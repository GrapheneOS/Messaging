package com.android.messaging.ui.conversation.v2.messages.mapper

import android.util.Log
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessageUiModel
import javax.inject.Inject

internal interface ConversationMessageUiModelMapper {
    fun map(data: ConversationMessageData): ConversationMessageUiModel?
}

internal class ConversationMessageUiModelMapperImpl @Inject constructor() : ConversationMessageUiModelMapper {

    // TODO: Check if empty default values are ok
    override fun map(data: ConversationMessageData): ConversationMessageUiModel? {
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

    private fun mapStatus(javaStatus: Int): ConversationMessageUiModel.Status {
        return when (javaStatus) {
            MessageData.BUGLE_STATUS_UNKNOWN -> ConversationMessageUiModel.Status.Unknown

            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE -> ConversationMessageUiModel.Status.Outgoing.Complete
            MessageData.BUGLE_STATUS_OUTGOING_DELIVERED -> ConversationMessageUiModel.Status.Outgoing.Delivered
            MessageData.BUGLE_STATUS_OUTGOING_DRAFT -> ConversationMessageUiModel.Status.Outgoing.Draft
            MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND -> ConversationMessageUiModel.Status.Outgoing.YetToSend
            MessageData.BUGLE_STATUS_OUTGOING_SENDING -> ConversationMessageUiModel.Status.Outgoing.Sending
            MessageData.BUGLE_STATUS_OUTGOING_RESENDING -> ConversationMessageUiModel.Status.Outgoing.Resending
            MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY -> ConversationMessageUiModel.Status.Outgoing.AwaitingRetry
            MessageData.BUGLE_STATUS_OUTGOING_FAILED -> ConversationMessageUiModel.Status.Outgoing.Failed
            MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER ->
                ConversationMessageUiModel.Status.Outgoing.FailedEmergencyNumber

            MessageData.BUGLE_STATUS_INCOMING_COMPLETE -> ConversationMessageUiModel.Status.Incoming.Complete
            MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD ->
                ConversationMessageUiModel.Status.Incoming.YetToManualDownload

            MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD ->
                ConversationMessageUiModel.Status.Incoming.RetryingManualDownload

            MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING ->
                ConversationMessageUiModel.Status.Incoming.ManualDownloading

            MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD ->
                ConversationMessageUiModel.Status.Incoming.RetryingAutoDownload

            MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING ->
                ConversationMessageUiModel.Status.Incoming.AutoDownloading

            MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED ->
                ConversationMessageUiModel.Status.Incoming.DownloadFailed

            MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE ->
                ConversationMessageUiModel.Status.Incoming.ExpiredOrNotAvailable

            else -> {
                Log.e(LOG_TAG, "mapStatus: unexpected value=$javaStatus")

                ConversationMessageUiModel.Status.Unknown
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

        if (primaryTimestamp > 0L) {
            return primaryTimestamp
        }

        return when {
            isIncoming -> sentTimestamp
            else -> receivedTimestamp
        }
    }

    private companion object {
        private const val LOG_TAG = "ConversationMessageUiModelMapper"
    }
}
