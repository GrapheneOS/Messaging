package com.android.messaging.datamodel.action

import android.content.ContentUris
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.Telephony.Mms
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.mmslib.pdu.CharacterSets
import com.android.messaging.mmslib.pdu.PduHeaders
import com.android.messaging.mmslib.pdu.RetrieveConf
import com.android.messaging.sms.DatabaseMessages
import com.android.messaging.sms.MmsSender
import com.android.messaging.sms.MmsUtils
import com.android.messaging.util.ContentType
import io.mockk.every
import io.mockk.mockk

internal data class MmsNotifyResponse(
    val subId: Int,
    val transactionId: String,
    val contentLocation: String,
    val status: Int,
)

internal data class PlatformMmsDownload(
    val subId: Int,
    val contentLocation: String,
    val extras: Bundle,
    val localStatusAtDownload: Int?,
)

internal data class TelephonyDownloadedMmsInsert(
    val notificationUri: Uri,
    val subId: Int,
    val autoDownload: Boolean,
    val uri: Uri,
)

internal data class DownloadedMms(
    val sender: String,
    val text: String,
    val otherRecipients: List<String> = emptyList(),
)

/**
 * Fakes what lies below the incoming MMS actions: the WAP push PDU, the MMS rows in the telephony
 * provider and the platform download. A notification is pushed by a test; a download completes
 * only when a test delivers its result.
 */
internal class FakeIncomingMms(
    private val telephony: FakeTelephony,
) {

    val notifyResponses: MutableList<MmsNotifyResponse> = mutableListOf()
    val downloads: MutableList<PlatformMmsDownload> = mutableListOf()
    val downloadedInserts: MutableList<TelephonyDownloadedMmsInsert> = mutableListOf()

    private val notificationsByPushData = mutableMapOf<String, PushedNotification>()
    private val telephonyMessages = mutableMapOf<String, StoredMms>()
    private val stagedDownloads = ArrayDeque<DownloadedMms>()
    private var nextTransaction = 1

    fun install() {
        every { MmsUtils.processReceivedPdu(any(), any(), any(), any()) } answers {
            receivePdu(pushData = secondArg(), subId = thirdArg())
        }
        every { MmsUtils.getMmsSender(any(), any()) } answers {
            synchronized(this@FakeIncomingMms) { telephonyMessages[secondArg()]?.sender }
        }
        every { MmsUtils.loadMms(any()) } answers {
            synchronized(this@FakeIncomingMms) {
                telephonyMessages[firstArg<Uri>().toString()]?.toMmsMessage()
            }
        }
        every {
            MmsUtils.sendNotifyResponseForMmsDownload(any(), any(), any(), any(), any())
        } answers {
            notifyResponses += MmsNotifyResponse(
                subId = secondArg(),
                transactionId = String(thirdArg<ByteArray>()),
                contentLocation = arg(3),
                status = arg(4),
            )
        }
        every { MmsSender.downloadMms(any(), any(), any(), any()) } answers {
            val extras = Bundle(arg<Bundle>(3))
            downloads += PlatformMmsDownload(
                subId = secondArg(),
                contentLocation = thirdArg(),
                extras = extras,
                localStatusAtDownload = localStatusOf(
                    messageId = extras.getString(DownloadMmsAction.EXTRA_MESSAGE_ID),
                ),
            )
        }
        val retrieveConf = mockk<RetrieveConf>(relaxed = true)
        every { MmsSender.parseRetrieveConf(any(), any()) } answers {
            when {
                firstArg<ByteArray?>() contentEquals RETRIEVE_CONF_RESPONSE -> retrieveConf
                else -> null
            }
        }
        every {
            MmsUtils.insertDownloadedMessageAndSendResponse(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } answers {
            insertDownloaded(
                notificationUri = secondArg(),
                subId = thirdArg(),
                autoDownload = arg(6),
                receivedTimestampInSeconds = arg(7),
            )
        }
    }

    fun pushNotification(sender: String, subId: Int): ByteArray {
        val notification = synchronized(this) {
            val transaction = nextTransaction++
            PushedNotification(
                sender = sender,
                subId = subId,
                transactionId = "$TRANSACTION_ID_PREFIX$transaction",
                contentLocation = "$CONTENT_LOCATION_PREFIX$transaction",
            )
        }
        val pushData = "$PUSH_DATA_PREFIX${notification.transactionId}"
        synchronized(this) { notificationsByPushData[pushData] = notification }
        return pushData.toByteArray()
    }

    fun stageDownload(content: DownloadedMms) {
        synchronized(this) { stagedDownloads.addLast(content) }
    }

    private fun receivePdu(pushData: ByteArray, subId: Int): DatabaseMessages.MmsMessage? {
        val notification = synchronized(this) { notificationsByPushData[String(pushData)] }
        return notification?.let { pushed ->
            val stored = StoredMms(
                uri = telephony.allocateMmsUri(),
                sender = pushed.sender,
                threadId = telephony.threadIdFor(recipients = listOf(pushed.sender)),
                subId = subId,
                messageType = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND,
                timestampInSeconds = System.currentTimeMillis() / MILLIS_PER_SECOND,
                transactionId = pushed.transactionId,
                contentLocation = pushed.contentLocation,
                text = null,
            )
            synchronized(this) { telephonyMessages[stored.uri.toString()] = stored }
            stored.toMmsMessage()
        }
    }

    private fun insertDownloaded(
        notificationUri: Uri,
        subId: Int,
        autoDownload: Boolean,
        receivedTimestampInSeconds: Long,
    ): MmsUtils.StatusPlusUri {
        val content = requireNotNull(synchronized(this) { stagedDownloads.removeFirstOrNull() }) {
            "No downloaded MMS was staged"
        }
        val notification = requireNotNull(
            synchronized(this) { telephonyMessages[notificationUri.toString()] },
        ) { "No MMS notification at $notificationUri" }
        val stored = StoredMms(
            uri = telephony.allocateMmsUri(),
            sender = content.sender,
            threadId = telephony.threadIdFor(
                recipients = listOf(content.sender) + content.otherRecipients,
            ),
            subId = subId,
            messageType = PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF,
            timestampInSeconds = receivedTimestampInSeconds,
            transactionId = notification.transactionId,
            contentLocation = notification.contentLocation,
            text = content.text,
        )
        synchronized(this) { telephonyMessages[stored.uri.toString()] = stored }
        downloadedInserts += TelephonyDownloadedMmsInsert(
            notificationUri = notificationUri,
            subId = subId,
            autoDownload = autoDownload,
            uri = stored.uri,
        )
        return MmsUtils.StatusPlusUri(
            MmsUtils.MMS_REQUEST_SUCCEEDED,
            PduHeaders.RETRIEVE_STATUS_OK,
            stored.uri,
        )
    }

    private fun localStatusOf(messageId: String?): Int? {
        return DataModel.get().database.query(
            DatabaseHelper.MESSAGES_TABLE,
            arrayOf(MessageColumns.STATUS),
            "${BaseColumns._ID} = ?",
            arrayOf(messageId.orEmpty()),
            null,
            null,
            null,
        ).use { cursor ->
            when {
                cursor.moveToFirst() -> cursor.getInt(0)
                else -> null
            }
        }
    }

    private data class PushedNotification(
        val sender: String,
        val subId: Int,
        val transactionId: String,
        val contentLocation: String,
    )

    private data class StoredMms(
        val uri: Uri,
        val sender: String,
        val threadId: Long,
        val subId: Int,
        val messageType: Int,
        val timestampInSeconds: Long,
        val transactionId: String,
        val contentLocation: String,
        val text: String?,
    ) {
        fun toMmsMessage(): DatabaseMessages.MmsMessage {
            val rowId = ContentUris.parseId(uri)
            val message = MatrixCursor(DatabaseMessages.MmsMessage.getProjection()).use { cursor ->
                cursor.addRow(
                    arrayOf<Any?>(
                        rowId,
                        Mms.MESSAGE_BOX_INBOX,
                        null,
                        0,
                        MESSAGE_SIZE_BYTES,
                        timestampInSeconds,
                        timestampInSeconds,
                        threadId,
                        PduHeaders.PRIORITY_NORMAL,
                        0,
                        0,
                        0,
                        contentLocation,
                        transactionId,
                        messageType,
                        timestampInSeconds + EXPIRY_SECONDS,
                        0,
                        retrieveStatus(),
                        subId,
                    ),
                )
                cursor.moveToFirst()
                DatabaseMessages.MmsMessage.get(cursor)
            }
            message.mSubId = subId
            text?.let { body -> message.addPart(textPart(messageRowId = rowId, text = body)) }
            return message
        }

        private fun retrieveStatus(): Int {
            return when (messageType) {
                PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF -> PduHeaders.RETRIEVE_STATUS_OK
                else -> 0
            }
        }

        private fun textPart(messageRowId: Long, text: String): DatabaseMessages.MmsPart {
            return MatrixCursor(DatabaseMessages.MmsPart.PROJECTION).use { cursor ->
                cursor.addRow(
                    arrayOf<Any?>(
                        messageRowId * PART_ROW_ID_FACTOR,
                        messageRowId,
                        CharacterSets.UTF_8,
                        ContentType.TEXT_PLAIN,
                        text,
                    ),
                )
                cursor.moveToFirst()
                DatabaseMessages.MmsPart.get(cursor, false)
            }
        }
    }

    companion object {
        val RETRIEVE_CONF_RESPONSE: ByteArray = byteArrayOf(0x8C.toByte(), 0x84.toByte())
        private const val PUSH_DATA_PREFIX = "wap-push:"
        private const val TRANSACTION_ID_PREFIX = "tx-"
        private const val CONTENT_LOCATION_PREFIX = "http://mmsc.invalid/m/"
        private const val MILLIS_PER_SECOND = 1_000L
        private const val EXPIRY_SECONDS = 7 * 24 * 60 * 60L
        private const val MESSAGE_SIZE_BYTES = 2_048L
        private const val PART_ROW_ID_FACTOR = 10L
    }
}
