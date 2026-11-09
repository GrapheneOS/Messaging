package com.android.messaging.datamodel.action

import com.android.messaging.sms.DatabaseMessages.LocalDatabaseMessage
import com.android.messaging.sms.DatabaseMessages.MmsMessage
import com.android.messaging.sms.DatabaseMessages.SmsMessage

internal data class SyncBatchChanges(
    val actionKey: String,
    val smsToAdd: List<SmsMessage>,
    val mmsToAdd: List<MmsMessage>,
    val messagesToDelete: MutableList<LocalDatabaseMessage>,
)
