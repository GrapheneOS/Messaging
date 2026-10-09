@file:JvmName("SmsDestinations")

package com.android.messaging.sms

import com.android.messaging.util.PhoneUtils

internal fun canReplyToDestination(destination: String, subId: Int): Boolean {
    return when {
        isNumberOrEmailAddress(destination = destination) -> true
        else -> canSendToAlias(alias = destination, subId = subId)
    }
}

internal fun canReplyToDestinationFromAnySubscription(destination: String): Boolean {
    return when {
        isNumberOrEmailAddress(destination = destination) -> true
        else -> PhoneUtils.getDefault().activeSubscriptionInfoList.any { subscription ->
            canSendToAlias(alias = destination, subId = subscription.subscriptionId)
        }
    }
}

private fun isNumberOrEmailAddress(destination: String): Boolean {
    return MmsSmsUtils.isEmailAddress(destination) ||
        destination.none { character -> character.isLetter() }
}

private fun canSendToAlias(alias: String, subId: Int): Boolean {
    return !MmsConfig.get(subId).emailGateway.isNullOrEmpty() &&
        MmsSmsUtils.isAlias(alias, subId)
}
