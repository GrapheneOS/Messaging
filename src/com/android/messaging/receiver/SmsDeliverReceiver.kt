package com.android.messaging.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import com.android.messaging.data.sms.IncomingSmsDeliverer
import com.android.messaging.di.receiver.IncomingSmsEntryPoint
import dagger.hilt.android.EntryPointAccessors

class SmsDeliverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Sms.Intents.SMS_DELIVER_ACTION) {
            delivererFrom(context).deliverFromIntent(context, intent)
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
            delivererFrom(context).deliver(
                context = context,
                subId = subId,
                errorCode = errorCode,
                messages = messages
            )
        }

        private fun delivererFrom(context: Context): IncomingSmsDeliverer {
            return EntryPointAccessors.fromApplication(
                context.applicationContext,
                IncomingSmsEntryPoint::class.java,
            ).incomingSmsDeliverer()
        }
    }
}
