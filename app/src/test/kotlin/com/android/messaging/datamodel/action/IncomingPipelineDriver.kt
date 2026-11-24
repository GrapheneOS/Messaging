package com.android.messaging.datamodel.action

import android.app.Activity
import android.content.ContentValues
import android.os.Bundle
import android.provider.Telephony.Sms
import android.telephony.SmsManager
import androidx.core.content.contentValuesOf
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.MmsFileProvider
import com.android.messaging.datamodel.data.ParticipantData

/**
 * What the platform and the user do to the incoming pipeline, and the conversation lookup it
 * relies on. The incoming test classes go through this interface only, so they can run against a
 * replacement of the legacy actions.
 */
internal interface IncomingPipelineDriver {

    fun receiveSms(
        sender: String,
        text: String,
        subId: Int,
        serviceCenter: String? = null,
        replyPathPresent: Boolean = false,
    )

    fun receiveMmsNotification(sender: String, subId: Int)

    fun redownloadMms(messageId: String)

    fun completeMmsDownload(content: DownloadedMms)

    fun failMmsDownload(resultCode: Int, httpStatus: Int = 0)

    fun conversationForRecipient(
        threadId: Long,
        recipient: String,
        senderBlocked: Boolean,
        subId: Int,
    ): String

    fun conversationForThread(threadId: Long, senderBlocked: Boolean, subId: Int): String
}

internal class LegacyIncomingPipelineDriver(
    private val world: OutgoingPipelineWorld,
) : IncomingPipelineDriver {

    override fun receiveSms(
        sender: String,
        text: String,
        subId: Int,
        serviceCenter: String?,
        replyPathPresent: Boolean,
    ) {
        val sentTimestamp = System.currentTimeMillis()
        val values = contentValuesOf(
            Sms.ADDRESS to sender,
            Sms.BODY to text,
            Sms.DATE to sentTimestamp,
            Sms.DATE_SENT to sentTimestamp,
            Sms.REPLY_PATH_PRESENT to replyPathFlag(replyPathPresent = replyPathPresent),
            Sms.SERVICE_CENTER to serviceCenter,
            Sms.SUBSCRIPTION_ID to subId,
            Sms.Inbox.READ to 0,
            Sms.Inbox.SEEN to 0,
        )
        runAndSettle { ReceiveSmsMessageAction(ContentValues(values)).start() }
    }

    override fun receiveMmsNotification(sender: String, subId: Int) {
        val pushData = world.incomingMms.pushNotification(sender = sender, subId = subId)
        runAndSettle { ReceiveMmsMessageAction(subId, pushData).start() }
    }

    override fun redownloadMms(messageId: String) {
        runAndSettle { RedownloadMmsAction.redownloadMessage(messageId) }
    }

    override fun completeMmsDownload(content: DownloadedMms) {
        world.incomingMms.stageDownload(content = content)
        val contentUri = MmsFileProvider.buildRawMmsUri()
        MmsFileProvider.getFile(contentUri).apply {
            parentFile?.mkdirs()
            writeBytes(FakeIncomingMms.RETRIEVE_CONF_RESPONSE)
        }
        deliverDownloadResult(resultCode = Activity.RESULT_OK, httpStatus = 0) { extras ->
            extras.putParcelable(DownloadMmsAction.EXTRA_CONTENT_URI, contentUri)
        }
    }

    override fun failMmsDownload(resultCode: Int, httpStatus: Int) {
        deliverDownloadResult(resultCode = resultCode, httpStatus = httpStatus) { extras ->
            extras.putParcelable(
                DownloadMmsAction.EXTRA_CONTENT_URI,
                MmsFileProvider.buildRawMmsUri(),
            )
        }
    }

    override fun conversationForRecipient(
        threadId: Long,
        recipient: String,
        senderBlocked: Boolean,
        subId: Int,
    ): String {
        return world.withDatabase { db ->
            BugleDatabaseOperations.getOrCreateConversationFromRecipient(
                db,
                threadId,
                senderBlocked,
                ParticipantData.getFromRawPhoneBySimLocale(recipient, subId),
            )
        }
    }

    override fun conversationForThread(threadId: Long, senderBlocked: Boolean, subId: Int): String {
        return world.withDatabase { db ->
            BugleDatabaseOperations.getOrCreateConversationFromThreadId(
                db,
                threadId,
                senderBlocked,
                subId,
            )
        }
    }

    private fun deliverDownloadResult(
        resultCode: Int,
        httpStatus: Int,
        addContent: (Bundle) -> Unit,
    ) {
        val download = requireNotNull(world.incomingMms.downloads.lastOrNull()) {
            "No MMS download was handed to the platform"
        }
        val extras = Bundle(download.extras).apply {
            putInt(SmsManager.EXTRA_MMS_HTTP_STATUS, httpStatus)
            addContent(this)
        }
        runAndSettle { ProcessDownloadedMmsAction.processMessageDownloaded(resultCode, extras) }
    }

    private fun replyPathFlag(replyPathPresent: Boolean): Int {
        return when {
            replyPathPresent -> 1
            else -> 0
        }
    }

    private fun runAndSettle(trigger: () -> Unit) {
        world.onWorker {
            trigger()
            world.runner.runUntilIdle()
        }
    }
}
