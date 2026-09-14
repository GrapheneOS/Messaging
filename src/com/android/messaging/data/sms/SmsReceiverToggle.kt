package com.android.messaging.data.sms

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.android.messaging.util.LogUtil
import com.android.messaging.util.OsUtil
import javax.inject.Inject

internal interface SmsReceiverToggle {
    fun update(context: Context)
}

internal class SmsReceiverToggleImpl @Inject constructor() : SmsReceiverToggle {

    override fun update(context: Context) {
        // When we're running as the secondary user, we don't get the new SMS_DELIVER intent,
        // only the primary user receives that. As secondary, we need to go old-school and
        // listen for the SMS_RECEIVED intent. For the secondary user, use this SmsReceiver
        // for both sms and mms notification. For the primary user we don't use the SmsReceiver.
        val enabled = OsUtil.isSecondaryUser()
        LogUtil.v(
            LogUtil.BUGLE_TAG,
            when {
                enabled -> "Enabling SMS message receiving"
                else -> "Disabling SMS message receiving"
            },
        )

        val state = when {
            enabled -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context.packageName, SMS_RECEIVER_CLASS),
            state,
            PackageManager.DONT_KILL_APP,
        )
    }

    private companion object {
        private const val SMS_RECEIVER_CLASS = "com.android.messaging.receiver.SmsReceiver"
    }
}
