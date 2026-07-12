package com.android.messaging.data.sms

import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import com.android.messaging.Factory
import com.android.messaging.datamodel.action.ReceiveSmsMessageAction
import com.android.messaging.sms.MmsUtils
import com.android.messaging.util.DebugUtils
import com.android.messaging.util.LogUtil
import com.android.messaging.util.PhoneUtils
import javax.inject.Inject

internal interface IncomingSmsDeliverer {
    fun deliverFromIntent(context: Context, intent: Intent)
    fun deliver(context: Context, subId: Int, errorCode: Int, messages: Array<SmsMessage>)
}

internal class IncomingSmsDelivererImpl @Inject constructor(
    private val parser: IncomingSmsParser,
) : IncomingSmsDeliverer {

    override fun deliverFromIntent(
        context: Context,
        intent: Intent,
    ) {
        val messages = parser.parse(intent)
        if (messages.isNullOrEmpty()) {
            LogUtil.e(LogUtil.BUGLE_TAG, "processReceivedSms: null or zero or ignored message")
            return
        }

        val errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, NO_ERROR_CODE)
        val subId = PhoneUtils.getDefault()
            .getEffectiveIncomingSubIdFromSystem(intent, EXTRA_SUB_ID)
        deliver(
            context = context,
            subId = subId,
            errorCode = errorCode,
            messages = messages,
        )

        if (MmsUtils.isDumpSmsEnabled()) {
            val format = intent.getStringExtra(EXTRA_FORMAT)
            DebugUtils.dumpSms(messages.first().timestampMillis, messages, format)
        }
    }

    override fun deliver(
        context: Context,
        subId: Int,
        errorCode: Int,
        messages: Array<SmsMessage>,
    ) {
        val firstMessage = messages.first()
        val messageValues = MmsUtils.parseReceivedSmsMessage(context, messages, errorCode)
        val receivedTimestampMs = MmsUtils.getMessageDate(firstMessage, System.currentTimeMillis())

        // Default to unread and unseen for us but ReceiveSmsMessageAction will override
        // seen for the telephony db.
        messageValues.put(Sms.Inbox.READ, 0)
        messageValues.put(Sms.Inbox.SEEN, 0)
        messageValues.put(Sms.Inbox.DATE, receivedTimestampMs)
        messageValues.put(Sms.SUBSCRIPTION_ID, subId)

        val isClassZero = firstMessage.messageClass == SmsMessage.MessageClass.CLASS_0 ||
            DebugUtils.debugClassZeroSmsEnabled()

        when {
            isClassZero -> Factory.get().getUIIntents()
                .launchClassZeroActivity(context, messageValues)

            else -> ReceiveSmsMessageAction(messageValues).start()
        }
    }

    private companion object {
        private const val EXTRA_ERROR_CODE = "errorCode"
        private const val EXTRA_SUB_ID = "subscription"
        private const val EXTRA_FORMAT = "format"
        private const val NO_ERROR_CODE = -1
    }
}
