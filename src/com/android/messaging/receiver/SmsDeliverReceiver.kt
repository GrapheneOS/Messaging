package com.android.messaging.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import com.android.messaging.di.receiver.IncomingSmsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
            val delivery = launch {
                entryPoint.incomingSmsDeliverer().deliverFromIntent(appContext, intent)
            }
            try {
                // The import keeps running in the application scope either way; stop holding the
                // broadcast open for it once the window is nearly spent, or the system kills us
                // with an ANR and the import does not finish at all.
                withTimeoutOrNull(BROADCAST_BUDGET) { delivery.join() }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {

        private val BROADCAST_BUDGET = 8.seconds

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
