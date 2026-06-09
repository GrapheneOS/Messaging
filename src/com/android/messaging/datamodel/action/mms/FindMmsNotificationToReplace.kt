package com.android.messaging.datamodel.action.mms

import android.database.Cursor
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.sms.DatabaseMessages.MmsMessage
import com.android.messaging.util.LogUtil

internal class FindMmsNotificationToReplace(
    private val database: DatabaseWrapper,
) {

    operator fun invoke(
        downloadedMms: MmsMessage,
        selfId: String,
        senderParticipantId: String,
    ): MessageData? {
        val transactionId = downloadedMms
            .mTransactionId
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val selection = "${MessageColumns.PROTOCOL}=? AND " +
            "${MessageColumns.MMS_TRANSACTION_ID}=? AND " +
            "${MessageColumns.SELF_PARTICIPANT_ID}=? AND " +
            "${MessageColumns.STATUS} IN $mmsNotificationDownloadStatusInOperand"

        val selectionArgs = arrayOf(
            MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION.toString(),
            transactionId,
            selfId,
            *mmsNotificationDownloadStatusArgs,
        )

        return database.query(
            DatabaseHelper.MESSAGES_TABLE,
            MessageData.getProjection(),
            selection,
            selectionArgs,
            null,
            null,
            "${MessageColumns.RECEIVED_TIMESTAMP} DESC",
        ).use { cursor ->
            when {
                !cursor.moveToNext() -> null
                else -> {
                    readNotificationToReplace(
                        cursor = cursor,
                        downloadedMms = downloadedMms,
                        senderParticipantId = senderParticipantId,
                    )
                }
            }
        }
    }

    private fun readNotificationToReplace(
        cursor: Cursor,
        downloadedMms: MmsMessage,
        senderParticipantId: String,
    ): MessageData {
        val message = MessageData().apply {
            bind(cursor)
        }

        if (senderParticipantId != message.participantId) {
            LogUtil.w(
                LogUtil.BUGLE_TAG,
                "SyncMessageBatch: replacing MMS notification for ${downloadedMms.mUri} " +
                    "even though sender participant differs",
            )
        }

        return message
    }
}
