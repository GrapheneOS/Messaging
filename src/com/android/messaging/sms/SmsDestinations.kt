@file:JvmName("SmsDestinations")

package com.android.messaging.sms

internal fun canReplyToDestination(destination: String): Boolean {
    return when {
        MmsSmsUtils.isEmailAddress(destination) -> true
        else -> destination.none { character -> character.isLetter() }
    }
}
