package com.android.messaging.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SubscriptionManager
import com.android.messaging.di.receiver.IncomingSmsEntryPoint
import com.android.messaging.sms.MmsUtils
import com.android.messaging.util.LogUtil
import com.android.messaging.util.OsUtil
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        LogUtil.v(LogUtil.BUGLE_TAG, "SmsReceiver.onReceive $intent")

        if (!PhoneUtils.getDefault().isSmsEnabled || !OsUtil.isSecondaryUser()) {
            return
        }

        if (intent.action == Sms.Intents.SMS_RECEIVED_ACTION) {
            handleSecondaryUserSmsReceived(context, intent, entryPoint(context))
        }
    }

    private fun handleSecondaryUserSmsReceived(
        context: Context,
        intent: Intent,
        entryPoint: IncomingSmsEntryPoint,
    ) {
        // The contact and conversation lookups perform synchronous queries, so resolve off the
        // main thread.
        val pendingResult = goAsync()
        entryPoint.applicationScope().launch(entryPoint.ioDispatcher()) {
            try {
                notifyIncomingMessage(context, intent, entryPoint)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun notifyIncomingMessage(
        context: Context,
        intent: Intent,
        entryPoint: IncomingSmsEntryPoint,
    ) {
        val values = entryPoint.incomingSmsParser().parse(intent)?.let { messages ->
            MmsUtils.parseReceivedSmsMessage(
                context,
                messages,
                SendStatusReceiver.NO_ERROR_CODE,
            )
        }
        val address = values?.getAsString(Sms.ADDRESS)
        val subscriptionId = PhoneUtils.getDefault().getEffectiveIncomingSubIdFromSystem(
            intent,
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
        )

        entryPoint.secondaryUserNotifier().notifyIncomingMessage(
            info = entryPoint.secondaryUserMessageResolver().resolve(
                address = address,
                body = values?.getAsString(Sms.BODY),
            ),
            target = entryPoint.secondaryUserNotificationTargetResolver().resolve(
                address = address,
                subscriptionId = subscriptionId,
            ),
        )
    }

    private fun entryPoint(context: Context): IncomingSmsEntryPoint {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            IncomingSmsEntryPoint::class.java,
        )
    }
}
