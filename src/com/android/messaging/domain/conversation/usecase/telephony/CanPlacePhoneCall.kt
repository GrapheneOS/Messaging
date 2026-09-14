package com.android.messaging.domain.conversation.usecase.telephony

import android.telephony.PhoneNumberUtils
import javax.inject.Inject

internal fun interface CanPlacePhoneCall {
    operator fun invoke(destination: String?): Boolean
}

internal class CanPlacePhoneCallImpl @Inject constructor(
    private val isDeviceVoiceCapable: IsDeviceVoiceCapable,
    private val isEmergencyPhoneNumber: IsEmergencyPhoneNumber,
) : CanPlacePhoneCall {

    override operator fun invoke(destination: String?): Boolean {
        val phoneNumber = destination?.takeIf(String::isNotBlank) ?: return false

        return isDeviceVoiceCapable() &&
            PhoneNumberUtils.isWellFormedSmsAddress(phoneNumber) &&
            !isEmergencyPhoneNumber(phoneNumber)
    }
}
