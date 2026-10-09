package com.android.messaging.sms

import com.android.messaging.testutil.stubActiveSubscriptions
import com.android.messaging.testutil.stubMmsConfig
import com.android.messaging.util.PhoneUtils
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmsDestinationsTest {

    @Before
    fun setUp() {
        mockkStatic(MmsConfig::class, PhoneUtils::class)
        stubMmsConfig(subId = NO_ALIAS_SUB_ID, aliasEnabled = false)
        stubMmsConfig(subId = ALIAS_SUB_ID, aliasEnabled = true)
        stubMmsConfig(subId = NO_GATEWAY_SUB_ID, aliasEnabled = true, emailGatewayNumber = null)
    }

    @After
    fun tearDown() {
        unmockkStatic(MmsConfig::class, PhoneUtils::class)
    }

    @Test
    fun anAlphanumericSenderId_cannotBeRepliedTo() {
        assertFalse(canReplyToDestination(destination = "AMAZON", subId = NO_ALIAS_SUB_ID))
        assertFalse(canReplyToDestination(destination = "Bank1234", subId = NO_ALIAS_SUB_ID))
        assertFalse(canReplyToDestination(destination = "My-Bank", subId = NO_ALIAS_SUB_ID))
    }

    @Test
    fun aPhoneNumber_canBeRepliedTo() {
        assertTrue(canReplyToDestination(destination = "54810027", subId = NO_ALIAS_SUB_ID))
        assertTrue(canReplyToDestination(destination = "+37254810027", subId = NO_ALIAS_SUB_ID))
        assertTrue(canReplyToDestination(destination = "(201) 555-0123", subId = NO_ALIAS_SUB_ID))
    }

    @Test
    fun aShortCode_canBeRepliedTo() {
        assertTrue(canReplyToDestination(destination = "13011", subId = NO_ALIAS_SUB_ID))
    }

    @Test
    fun anEmailAddress_canBeRepliedTo() {
        assertTrue(
            canReplyToDestination(destination = "someone@example.com", subId = NO_ALIAS_SUB_ID),
        )
    }

    @Test
    fun aCarrierAlias_canBeRepliedToThroughTheEmailGateway() {
        assertTrue(canReplyToDestination(destination = "john.doe", subId = ALIAS_SUB_ID))
    }

    @Test
    fun aCarrierAlias_cannotBeRepliedToWithoutAnEmailGateway() {
        assertFalse(canReplyToDestination(destination = "john.doe", subId = NO_GATEWAY_SUB_ID))
    }

    @Test
    fun aDestinationThatIsNotAnAlias_cannotBeRepliedToThroughTheEmailGateway() {
        assertFalse(canReplyToDestination(destination = "My-Bank", subId = ALIAS_SUB_ID))
        assertFalse(canReplyToDestination(destination = "1bank", subId = ALIAS_SUB_ID))
    }

    @Test
    fun aCarrierAlias_canBeRepliedToWhenAnyActiveSubscriptionSupportsIt() {
        stubActiveSubscriptions(subIds = listOf(NO_ALIAS_SUB_ID, ALIAS_SUB_ID))

        assertTrue(canReplyToDestinationFromAnySubscription(destination = "john.doe"))
    }

    @Test
    fun aCarrierAlias_cannotBeRepliedToWhenNoActiveSubscriptionSupportsIt() {
        stubActiveSubscriptions(subIds = listOf(NO_ALIAS_SUB_ID, NO_GATEWAY_SUB_ID))

        assertFalse(canReplyToDestinationFromAnySubscription(destination = "john.doe"))
    }

    @Test
    fun aPhoneNumber_canBeRepliedToWithoutAnActiveSubscription() {
        stubActiveSubscriptions(subIds = emptyList())

        assertTrue(canReplyToDestinationFromAnySubscription(destination = "+37254810027"))
    }

    private companion object {
        const val NO_ALIAS_SUB_ID = 1
        const val ALIAS_SUB_ID = 2
        const val NO_GATEWAY_SUB_ID = 3
    }
}
