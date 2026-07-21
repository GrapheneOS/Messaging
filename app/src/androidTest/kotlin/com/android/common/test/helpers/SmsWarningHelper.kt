package com.android.common.test.helpers

import com.android.messaging.BuildConfig
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BuglePrefsKeys

object SmsWarningHelper {

    fun acknowledgeSmsWarning(): Int {
        val previousAcknowledgedVersion = BuglePrefs.getApplicationPrefs().getInt(
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED_VERSION,
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED_VERSION_DEFAULT,
        )
        BuglePrefs.getApplicationPrefs().putInt(
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED_VERSION,
            BuildConfig.VERSION_CODE,
        )

        return previousAcknowledgedVersion
    }

    fun restoreSmsWarning(previousAcknowledgedVersion: Int) {
        BuglePrefs.getApplicationPrefs().putInt(
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED_VERSION,
            previousAcknowledgedVersion,
        )
    }
}
