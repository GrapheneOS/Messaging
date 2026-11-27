package com.android.messaging.datamodel.action

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.ParticipantRefresh
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.mmslib.pdu.SendConf
import com.android.messaging.receiver.SendStatusReceiver
import com.android.messaging.sms.MmsConfig
import com.android.messaging.sms.MmsSender
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.sms.MmsUtils
import com.android.messaging.sms.SmsSender
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSubscriptionManager

internal data class TelephonySmsInsert(
    val uri: Uri,
    val subId: Int,
    val address: String,
    val text: String,
    val timestamp: Long,
    val status: Int,
    val type: Int,
    val threadId: Long,
)

internal data class TelephonyStatusUpdate(
    val uri: Uri,
    val typeOrBox: Int,
    val timestamp: Long,
)

internal data class TelephonyMmsInsert(
    val uri: Uri,
    val recipients: List<String>,
    val subId: Int,
    val text: String?,
    val subject: String?,
    val timestamp: Long,
)

internal data class PlatformSmsSend(
    val destination: String,
    val parts: List<String>,
    val partIds: List<Int>,
    val subId: Int,
    val messageUri: Uri,
    val localStatusAtSend: Int?,
)

internal data class PlatformMmsSend(
    val subId: Int,
    val messageUri: Uri,
    val sentIntentExtras: Bundle,
    val localStatusAtSend: Int?,
)

internal data class ConversationNotificationUpdate(
    val conversationId: String?,
    val coverage: Int,
)

/**
 * Fakes the telephony provider and the radio below MmsUtils, SmsSender and MmsSender. SMS part
 * results are delivered through SmsSender.setResult, as SendStatusReceiver does; MMS results
 * arrive only when a test delivers them.
 */
internal class FakeTelephony {

    val smsInserts: MutableList<TelephonySmsInsert> = mutableListOf()
    val smsStatusUpdates: MutableList<TelephonyStatusUpdate> = mutableListOf()
    val mmsInserts: MutableList<TelephonyMmsInsert> = mutableListOf()
    val mmsStatusUpdates: MutableList<TelephonyStatusUpdate> = mutableListOf()
    val smsSends: MutableList<PlatformSmsSend> = mutableListOf()
    val mmsSends: MutableList<PlatformMmsSend> = mutableListOf()
    val emergencyNotifications: MutableList<String> = mutableListOf()
    val errorNotificationUpdates: MutableList<Int> = mutableListOf()
    val conversationNotificationUpdates: MutableList<ConversationNotificationUpdate> =
        mutableListOf()

    var isDefaultSmsApp: Boolean = true
    var defaultSmsSubscriptionId: Int = SUB_ID_SIM_1
    var activeSubscriptionIds: List<Int> = listOf(SUB_ID_SIM_1, SUB_ID_SIM_2)
    var failMmsInsert: Boolean = false
    var mmsSendConfStatus: Int = MmsUtils.MMS_REQUEST_SUCCEEDED

    val phoneUtils: PhoneUtils = mockk(relaxed = true)

    private val smsManager: SmsManager = mockk(relaxed = true)
    private val scriptedSmsResults = ArrayDeque<List<Int>>()
    private val scriptedMmsOutcomes = ArrayDeque<MmsUtils.StatusPlusUri>()
    private val threadIds = mutableMapOf<List<String>, Long>()
    private var nextRowId = FIRST_TELEPHONY_ROW_ID

    init {
        every { phoneUtils.getCanonicalBySimLocale(any()) } answers { firstArg() }
        every { phoneUtils.formatForDisplay(any()) } answers { firstArg() }
        every { phoneUtils.isDefaultSmsApp } answers { isDefaultSmsApp }
        every { phoneUtils.defaultSmsSubscriptionId } answers { defaultSmsSubscriptionId }
        every { phoneUtils.activeSubscriptionInfoList } answers { buildSubscriptionInfos() }
        every { phoneUtils.getEffectiveSubId(any()) } answers {
            when (val subId = firstArg<Int>()) {
                ParticipantData.DEFAULT_SELF_SUB_ID -> defaultSmsSubscriptionId
                else -> subId
            }
        }
        every { phoneUtils.smsManager } returns smsManager
        every { smsManager.divideMessage(any()) } answers {
            ArrayList(firstArg<String>().chunked(SMS_SEGMENT_LENGTH))
        }
        every {
            smsManager.sendMultipartTextMessage(
                any<String>(),
                any(),
                any<ArrayList<String>>(),
                any<ArrayList<PendingIntent>>(),
                any<ArrayList<PendingIntent>>(),
            )
        } answers {
            onSmsSent(
                destination = firstArg(),
                parts = thirdArg(),
                sentIntents = arg(3),
            )
        }
    }

    fun install() {
        mockkStatic(MmsUtils::class)
        mockkStatic(MmsConfig::class)
        mockkStatic(MmsSender::class)
        mockkStatic(PhoneNumberUtils::class)
        mockkStatic(BugleNotifications::class)
        mockkStatic(BugleActionToasts::class)
        mockkStatic(ParticipantRefresh::class)
        mockkStatic(MmsSmsUtils.Threads::class)
        installMmsUtils()
        installSupportingStatics()
    }

    fun scriptSmsPartResults(vararg resultCodes: Int) {
        synchronized(this) { scriptedSmsResults.addLast(resultCodes.toList()) }
    }

    fun scriptMmsSendOutcome(outcome: MmsUtils.StatusPlusUri) {
        synchronized(this) { scriptedMmsOutcomes.addLast(outcome) }
    }

    fun threadIdFor(recipients: List<String>): Long {
        return synchronized(this) {
            threadIds.getOrPut(recipients.sorted()) { FIRST_THREAD_ID + threadIds.size }
        }
    }

    fun recipientsForThread(threadId: Long): List<String> {
        return synchronized(this) {
            threadIds.entries.firstOrNull { (_, id) -> id == threadId }?.key.orEmpty()
        }
    }

    fun allocateSmsUri(): Uri {
        return nextUri(authority = "sms")
    }

    fun allocateMmsUri(): Uri {
        return nextUri(authority = "mms")
    }

    private fun installMmsUtils() {
        every { MmsUtils.getOrCreateThreadId(any<Context>(), any<List<String>>()) } answers {
            threadIdFor(recipients = secondArg())
        }
        every { MmsUtils.getOrCreateSmsThreadId(any(), any()) } answers {
            threadIdFor(recipients = listOf(secondArg()))
        }
        every { MmsSmsUtils.Threads.getOrCreateThreadId(any(), any<String>()) } answers {
            threadIdFor(recipients = listOf(secondArg()))
        }
        every { MmsUtils.getRecipientsByThread(any()) } answers {
            recipientsForThread(threadId = firstArg())
        }
        every {
            MmsUtils.insertSmsMessage(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers { recordSmsInsert(arguments = args) }
        every { MmsUtils.updateSmsMessageSendingStatus(any(), any(), any(), any()) } answers {
            smsStatusUpdates += TelephonyStatusUpdate(
                uri = secondArg(),
                typeOrBox = thirdArg(),
                timestamp = arg(3),
            )
            true
        }
        every { MmsUtils.updateMmsMessageSendingStatus(any(), any(), any(), any()) } answers {
            mmsStatusUpdates += TelephonyStatusUpdate(
                uri = secondArg(),
                typeOrBox = thirdArg(),
                timestamp = arg(3),
            )
            true
        }
        every { MmsUtils.isDeliveryReportRequired(any()) } returns false
        every {
            MmsUtils.insertSendingMmsMessage(any(), any(), any(), any(), any(), any())
        } answers { recordMmsInsert(arguments = args) }
        every { MmsUtils.sendMmsMessage(any(), any(), any(), any()) } answers {
            recordMmsSend(subId = secondArg(), messageUri = thirdArg(), extras = arg(3))
        }
        every { MmsUtils.readSendingMmsMessage(any(), any(), any(), any()) } answers {
            readSendingMms(
                messageUri = firstArg(),
                conversationId = secondArg(),
                participantId = thirdArg(),
                selfId = arg(3),
            )
        }
        every { MmsUtils.updateSentMmsMessageStatus(any(), any(), any()) } answers {
            MmsUtils.StatusPlusUri(mmsSendConfStatus, MMS_RESPONSE_STATUS_OK, secondArg())
        }
    }

    private fun installSupportingStatics() {
        val mmsConfig = mockk<MmsConfig>(relaxed = true)
        every { mmsConfig.emailGateway } returns ""
        every { mmsConfig.sendMultipartSmsAsSeparateMessages } returns false
        every { mmsConfig.maxMessageSize } returns MAX_MMS_SIZE_BYTES
        every { MmsConfig.get(any()) } returns mmsConfig
        val sendConf = mockk<SendConf>(relaxed = true)
        every { MmsSender.parseSendConf(any(), any()) } answers {
            when {
                firstArg<ByteArray?>() contentEquals SEND_CONF_RESPONSE -> sendConf
                else -> null
            }
        }
        every { PhoneNumberUtils.isEmergencyNumber(any()) } answers {
            firstArg<String>() == EMERGENCY_NUMBER
        }
        every { BugleNotifications.notifyEmergencySmsFailed(any(), any()) } answers {
            emergencyNotifications += firstArg<String>()
        }
        every { BugleNotifications.update(any<Int>()) } answers {
            errorNotificationUpdates += firstArg<Int>()
        }
        every { BugleNotifications.update(any<String>(), any()) } answers {
            conversationNotificationUpdates += ConversationNotificationUpdate(
                conversationId = firstArg(),
                coverage = secondArg(),
            )
        }
        every {
            BugleActionToasts.onSendMessageOrManualDownloadActionCompleted(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns Unit
        every { ParticipantRefresh.refreshParticipant(any(), any()) } returns false
    }

    private fun recordSmsInsert(arguments: List<Any?>): Uri {
        val uri = nextUri(authority = "sms")
        smsInserts += TelephonySmsInsert(
            uri = uri,
            subId = arguments[2] as Int,
            address = arguments[3] as String,
            text = arguments[4] as String,
            timestamp = arguments[5] as Long,
            status = arguments[6] as Int,
            type = arguments[7] as Int,
            threadId = arguments[8] as Long,
        )
        return uri
    }

    private fun recordMmsInsert(arguments: List<Any?>): Uri? {
        val message = arguments[2] as MessageData
        val uri = when {
            failMmsInsert -> null
            else -> nextUri(authority = "mms")
        }
        uri?.let { insertedUri ->
            @Suppress("UNCHECKED_CAST")
            mmsInserts += TelephonyMmsInsert(
                uri = insertedUri,
                recipients = (arguments[1] as List<String>).toList(),
                subId = arguments[3] as Int,
                text = message.messageText,
                subject = message.mmsSubject,
                timestamp = arguments[5] as Long,
            )
        }
        return uri
    }

    private fun recordMmsSend(subId: Int, messageUri: Uri, extras: Bundle): MmsUtils.StatusPlusUri {
        val outcome = synchronized(this) { scriptedMmsOutcomes.removeFirstOrNull() }
        val sentIntentExtras = Bundle(extras).apply {
            putInt(SendMessageAction.KEY_SUB_ID, subId)
        }
        mmsSends += PlatformMmsSend(
            subId = subId,
            messageUri = messageUri,
            sentIntentExtras = sentIntentExtras,
            localStatusAtSend = localStatusOf(messageUri = messageUri),
        )
        return outcome ?: MmsUtils.STATUS_PENDING
    }

    private fun readSendingMms(
        messageUri: Uri,
        conversationId: String,
        participantId: String?,
        selfId: String,
    ): MessageData? {
        val inserted = mmsInserts.lastOrNull { insert -> insert.uri == messageUri }
        return inserted?.let { insert ->
            MessageData.createDraftMmsMessage(conversationId, selfId, insert.text, insert.subject)
                .apply {
                    bindParticipantId(participantId)
                    updateSendingMessage(conversationId, messageUri, insert.timestamp)
                }
        }
    }

    private fun onSmsSent(
        destination: String,
        parts: ArrayList<String>,
        sentIntents: ArrayList<PendingIntent>,
    ) {
        val sentBroadcasts = sentIntents.map { pendingIntent ->
            shadowOf(pendingIntent).savedIntent
        }
        val messageUri = requireNotNull(sentBroadcasts.first().data)
        val subId = sentBroadcasts.first().getIntExtra(SendStatusReceiver.EXTRA_SUB_ID, -1)
        val partIds = sentBroadcasts.map { intent ->
            intent.getIntExtra(SendStatusReceiver.EXTRA_PART_ID, -1)
        }
        smsSends += PlatformSmsSend(
            destination = destination,
            parts = parts.toList(),
            partIds = partIds,
            subId = subId,
            messageUri = messageUri,
            localStatusAtSend = localStatusOf(messageUri = messageUri),
        )
        val resultCodes = synchronized(this) { scriptedSmsResults.removeFirstOrNull() }
            ?: List(parts.size) { Activity.RESULT_OK }
        partIds.forEachIndexed { index, partId ->
            SmsSender.setResult(
                messageUri,
                resultCodes[index],
                SendStatusReceiver.NO_ERROR_CODE,
                partId,
                subId,
            )
        }
    }

    private fun localStatusOf(messageUri: Uri): Int? {
        return DataModel.get().database.query(
            DatabaseHelper.MESSAGES_TABLE,
            arrayOf(MessageColumns.STATUS),
            "${MessageColumns.SMS_MESSAGE_URI} = ?",
            arrayOf(messageUri.toString()),
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

    private fun buildSubscriptionInfos(): List<SubscriptionInfo> {
        return activeSubscriptionIds.mapIndexed { slot, subId ->
            ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder()
                .setId(subId)
                .setSimSlotIndex(slot)
                .buildSubscriptionInfo()
        }
    }

    private fun nextUri(authority: String): Uri {
        val rowId = synchronized(this) { nextRowId++ }
        return Uri.parse("content://$authority/$rowId")
    }

    companion object {
        const val SUB_ID_SIM_1 = 1
        const val SUB_ID_SIM_2 = 2
        const val EMERGENCY_NUMBER = "911"
        const val SMS_SEGMENT_LENGTH = 160
        const val MAX_MMS_SIZE_BYTES = 300 * 1024
        val SEND_CONF_RESPONSE: ByteArray = byteArrayOf(0x8C.toByte(), 0x81.toByte())
        private const val MMS_RESPONSE_STATUS_OK = 0x80
        private const val FIRST_THREAD_ID = 500L
        private const val FIRST_TELEPHONY_ROW_ID = 9_000L
    }
}
