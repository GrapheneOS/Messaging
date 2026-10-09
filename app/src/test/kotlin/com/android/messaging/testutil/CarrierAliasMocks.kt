package com.android.messaging.testutil

import android.telephony.SubscriptionInfo
import androidx.appcompat.mms.CarrierConfigValuesLoader
import com.android.messaging.sms.MmsConfig
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk

internal const val VERIZON_EMAIL_GATEWAY_NUMBER = "6245"

internal fun stubMmsConfig(
    subId: Int,
    aliasEnabled: Boolean,
    emailGatewayNumber: String? = VERIZON_EMAIL_GATEWAY_NUMBER,
) {
    every { MmsConfig.get(subId) } returns mockk {
        every { isAliasEnabled } returns aliasEnabled
        every { aliasMinChars } returns CarrierConfigValuesLoader.CONFIG_ALIAS_MIN_CHARS_DEFAULT
        every { aliasMaxChars } returns CarrierConfigValuesLoader.CONFIG_ALIAS_MAX_CHARS_DEFAULT
        every { emailGateway } returns emailGatewayNumber
    }
}

internal fun stubActiveSubscriptions(subIds: List<Int>) {
    val subscriptions = subIds.map { subId ->
        mockk<SubscriptionInfo> {
            every { subscriptionId } returns subId
        }
    }
    every { PhoneUtils.getDefault() } returns mockk {
        every { activeSubscriptionInfoList } returns subscriptions
    }
}
