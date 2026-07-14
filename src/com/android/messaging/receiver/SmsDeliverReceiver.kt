package com.android.messaging.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import com.android.messaging.di.receiver.IncomingSmsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

class SmsDeliverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        // Import within the broadcast window, not via the job queue that JobScheduler can defer
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val entryPoint = entryPoint(appContext)
        entryPoint.applicationScope().launch(entryPoint.ioDispatcher()) {
            try {
                entryPoint.incomingSmsDeliverer().deliverFromIntent(appContext, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {

        @JvmStatic
        fun deliverSmsMessages(
            context: Context,
            subId: Int,
            errorCode: Int,
            messages: Array<SmsMessage>,
        ) {
            entryPoint(context).incomingSmsDeliverer().deliver(
                context = context,
                subId = subId,
                errorCode = errorCode,
                messages = messages,
            )
        }

        private fun entryPoint(context: Context): IncomingSmsEntryPoint {
            return EntryPointAccessors.fromApplication(
                context.applicationContext,
                IncomingSmsEntryPoint::class.java,
            )
        }
    }
}
