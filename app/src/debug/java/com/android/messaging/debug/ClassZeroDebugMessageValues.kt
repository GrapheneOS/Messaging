package com.android.messaging.debug

import android.content.ContentValues
import android.content.Intent
import android.provider.Telephony.Sms
import com.android.messaging.datamodel.data.ParticipantData

internal const val ACTION_SHOW_CLASS_ZERO = "com.android.messaging.debug.SHOW_CLASS_ZERO"
internal const val EXTRA_ADDRESS = "address"
internal const val EXTRA_BODY = "body"

private const val DEFAULT_ADDRESS = "+15551234567"
private const val DEFAULT_BODY = "Fake Class 0 message"

internal fun buildClassZeroMessageValues(intent: Intent): ContentValues {
    val messageBody = intent.getStringExtra(EXTRA_BODY)
        ?.takeIf { it.isNotBlank() }
        ?: DEFAULT_BODY
    val senderAddress = intent.getStringExtra(EXTRA_ADDRESS)
        ?.takeIf { it.isNotBlank() }
        ?: DEFAULT_ADDRESS
    val timestampMillis = System.currentTimeMillis()

    return ContentValues().apply {
        put(Sms.ADDRESS, senderAddress)
        put(Sms.BODY, messageBody)
        put(Sms.DATE, timestampMillis)
        put(Sms.DATE_SENT, timestampMillis)
        put(Sms.Inbox.READ, 0)
        put(Sms.Inbox.SEEN, 0)
        put(Sms.STATUS, Sms.STATUS_NONE)
        put(Sms.SUBSCRIPTION_ID, ParticipantData.DEFAULT_SELF_SUB_ID)
        put(Sms.TYPE, Sms.MESSAGE_TYPE_INBOX)
    }
}
