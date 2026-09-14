package com.android.messaging.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import com.android.messaging.data.secondaryuser.model.SecondaryUserMessageInfo
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
        // The contact lookup performs a synchronous query, so resolve off the main thread.
        val pendingResult = goAsync()
        entryPoint.applicationScope().launch(entryPoint.ioDispatcher()) {
            try {
                val info = resolveMessage(context, intent, entryPoint)
                entryPoint.secondaryUserNotifier().notifyIncomingMessage(info)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun resolveMessage(
        context: Context,
        intent: Intent,
        entryPoint: IncomingSmsEntryPoint,
    ): SecondaryUserMessageInfo? {
        val messages = entryPoint.incomingSmsParser().parse(intent) ?: return null
        val values = MmsUtils.parseReceivedSmsMessage(
            context,
            messages,
            SendStatusReceiver.NO_ERROR_CODE,
        )

        return entryPoint.secondaryUserMessageResolver().resolve(
            address = values.getAsString(Sms.ADDRESS),
            body = values.getAsString(Sms.BODY),
        )
    }

    private fun entryPoint(context: Context): IncomingSmsEntryPoint {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            IncomingSmsEntryPoint::class.java,
        )
    }
}
