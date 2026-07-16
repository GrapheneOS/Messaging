package com.android.common.test.helpers

import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BuglePrefsKeys

object SmsWarningHelper {

    fun acknowledgeSmsWarning(): Boolean {
        val wasAcknowledged = BuglePrefs.getApplicationPrefs().getBoolean(
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED,
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED_DEFAULT,
        )
        BuglePrefs.getApplicationPrefs().putBoolean(
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED,
            true,
        )

        return wasAcknowledged
    }

    fun restoreSmsWarning(wasAcknowledged: Boolean) {
        BuglePrefs.getApplicationPrefs().putBoolean(
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED,
            wasAcknowledged,
        )
    }
}
