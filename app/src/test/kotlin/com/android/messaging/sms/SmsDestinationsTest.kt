package com.android.messaging.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmsDestinationsTest {

    @Test
    fun anAlphanumericSenderId_cannotBeRepliedTo() {
        assertFalse(canReplyToDestination(destination = "AMAZON"))
        assertFalse(canReplyToDestination(destination = "Bank1234"))
        assertFalse(canReplyToDestination(destination = "My-Bank"))
    }

    @Test
    fun aPhoneNumber_canBeRepliedTo() {
        assertTrue(canReplyToDestination(destination = "54810027"))
        assertTrue(canReplyToDestination(destination = "+37254810027"))
        assertTrue(canReplyToDestination(destination = "(201) 555-0123"))
    }

    @Test
    fun aShortCode_canBeRepliedTo() {
        assertTrue(canReplyToDestination(destination = "13011"))
    }

    @Test
    fun anEmailAddress_canBeRepliedTo() {
        assertTrue(canReplyToDestination(destination = "someone@example.com"))
    }
}
