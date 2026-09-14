package com.android.messaging.data.onboarding.store

import android.Manifest
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BuglePrefsKeys
import com.android.messaging.util.OsUtil
import javax.inject.Inject

internal interface SelfPhoneNumberPermissionStore {
    fun isGranted(): Boolean
    fun isRequested(): Boolean
    fun markRequested()
}

internal class SelfPhoneNumberPermissionStoreImpl @Inject constructor() :
    SelfPhoneNumberPermissionStore {

    override fun isGranted(): Boolean {
        return OsUtil.hasPermission(Manifest.permission.READ_PHONE_NUMBERS)
    }

    override fun isRequested(): Boolean {
        return BuglePrefs.getApplicationPrefs().getBoolean(
            BuglePrefsKeys.SELF_PHONE_NUMBER_PERMISSION_REQUESTED,
            BuglePrefsKeys.SELF_PHONE_NUMBER_PERMISSION_REQUESTED_DEFAULT,
        )
    }

    override fun markRequested() {
        BuglePrefs.getApplicationPrefs().putBoolean(
            BuglePrefsKeys.SELF_PHONE_NUMBER_PERMISSION_REQUESTED,
            true,
        )
    }
}
