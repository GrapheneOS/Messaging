package com.android.messaging.data.onboarding.store

import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BuglePrefsKeys
import com.android.messaging.util.core.AppVersionProvider
import javax.inject.Inject

internal interface SmsWarningStore {
    fun isAcknowledged(): Boolean
    fun acknowledge()
}

internal class SmsWarningStoreImpl @Inject constructor(
    private val appVersionProvider: AppVersionProvider,
) : SmsWarningStore {

    override fun isAcknowledged(): Boolean {
        val acknowledgedVersion = BuglePrefs.getApplicationPrefs().getInt(
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED_VERSION,
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED_VERSION_DEFAULT,
        )
        return acknowledgedVersion == appVersionProvider.versionCode()
    }

    override fun acknowledge() {
        BuglePrefs.getApplicationPrefs().putInt(
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED_VERSION,
            appVersionProvider.versionCode(),
        )
    }
}
