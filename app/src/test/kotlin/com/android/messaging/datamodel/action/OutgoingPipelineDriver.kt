package com.android.messaging.datamodel.action

import android.os.Bundle
import android.telephony.SmsManager
import com.android.messaging.datamodel.MmsFileProvider
import com.android.messaging.datamodel.data.MessageData

/**
 * What a user, the platform and the process lifecycle do to the outgoing pipeline. The cases in
 * the *ActionTest classes go through this interface only, so they can run against a replacement
 * of the legacy actions.
 */
internal interface OutgoingPipelineDriver {

    fun sendSmsDraft(
        conversationId: String,
        text: String,
        draftSelfSubId: Int? = null,
        requestedSubId: Int? = null,
    )

    fun sendMmsDraft(conversationId: String, text: String, draftSelfSubId: Int? = null)

    fun sendToRecipients(subId: Int, recipients: List<String>, text: String, subject: String?)

    fun resend(messageId: String)

    fun processPendingMessages()

    fun restartProcess()

    fun fireRetryAlarm(subId: Int)

    fun pendingRetryDelayMillis(subId: Int): Long?

    fun deliverMmsSentResult(
        resultCode: Int,
        httpStatus: Int = 0,
        withSendConfirmation: Boolean = true,
    )
}

internal class LegacyOutgoingPipelineDriver(
    private val world: OutgoingPipelineWorld,
) : OutgoingPipelineDriver {

    override fun sendSmsDraft(
        conversationId: String,
        text: String,
        draftSelfSubId: Int?,
        requestedSubId: Int?,
    ) {
        val draft = MessageData.createDraftSmsMessage(
            conversationId,
            draftSelfSubId?.let { subId -> world.selfIdFor(subId = subId) },
            text,
        )
        runAndSettle {
            when (requestedSubId) {
                null -> InsertNewMessageAction.insertNewMessage(draft)
                else -> InsertNewMessageAction.insertNewMessage(draft, requestedSubId)
            }
        }
    }

    override fun sendMmsDraft(conversationId: String, text: String, draftSelfSubId: Int?) {
        val draft = MessageData.createDraftMmsMessage(
            conversationId,
            draftSelfSubId?.let { subId -> world.selfIdFor(subId = subId) },
            text,
            null,
        )
        runAndSettle { InsertNewMessageAction.insertNewMessage(draft) }
    }

    override fun sendToRecipients(
        subId: Int,
        recipients: List<String>,
        text: String,
        subject: String?,
    ) {
        runAndSettle {
            InsertNewMessageAction.insertNewMessage(
                subId,
                recipients.joinToString(separator = ","),
                text,
                subject,
            )
        }
    }

    override fun resend(messageId: String) {
        runAndSettle { ResendMessageAction.resendMessage(messageId) }
    }

    override fun processPendingMessages() {
        runAndSettle { ProcessPendingMessagesAction.processFirstPendingMessage() }
    }

    override fun restartProcess() {
        runAndSettle {
            FixupMessageStatusOnStartupAction.fixupMessageStatus()
            ProcessPendingMessagesAction.processFirstPendingMessage()
        }
    }

    override fun fireRetryAlarm(subId: Int) {
        runAndSettle {
            world.actionService.fireAlarm(requestCode = retryRequestCode(subId = subId))
        }
    }

    override fun pendingRetryDelayMillis(subId: Int): Long? {
        return world.actionService.pendingAlarm(requestCode = retryRequestCode(subId = subId))
            ?.delayMillis
    }

    override fun deliverMmsSentResult(
        resultCode: Int,
        httpStatus: Int,
        withSendConfirmation: Boolean,
    ) {
        val send = requireNotNull(world.telephony.mmsSends.lastOrNull()) {
            "No MMS was handed to the platform"
        }
        runAndSettle {
            val extras = Bundle(send.sentIntentExtras).apply {
                putParcelable(SendMessageAction.EXTRA_CONTENT_URI, MmsFileProvider.buildRawMmsUri())
                putBoolean(SendMessageAction.EXTRA_RESPONSE_IMPORTANT, true)
                putInt(SmsManager.EXTRA_MMS_HTTP_STATUS, httpStatus)
                if (withSendConfirmation) {
                    putByteArray(
                        SmsManager.EXTRA_MMS_DATA,
                        FakeTelephony.SEND_CONF_RESPONSE.copyOf(),
                    )
                }
            }
            ProcessSentMessageAction.processMmsSent(resultCode, send.messageUri, extras)
        }
    }

    private fun runAndSettle(trigger: () -> Unit) {
        world.onWorker {
            trigger()
            world.runner.runUntilIdle()
        }
    }

    private fun retryRequestCode(subId: Int): Int {
        return RETRY_REQUEST_CODE_BASE + subId
    }

    private companion object {
        private const val RETRY_REQUEST_CODE_BASE = 103
    }
}
