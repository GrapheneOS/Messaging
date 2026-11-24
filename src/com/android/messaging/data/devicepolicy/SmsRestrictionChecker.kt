package com.android.messaging.data.devicepolicy

import android.os.UserManager
import javax.inject.Inject

internal interface SmsRestrictionChecker {
    fun isSmsDisallowed(): Boolean
}

internal class SmsRestrictionCheckerImpl @Inject constructor(
    private val userManager: UserManager,
) : SmsRestrictionChecker {

    override fun isSmsDisallowed(): Boolean {
        return userManager.hasUserRestriction(UserManager.DISALLOW_SMS)
    }
}
