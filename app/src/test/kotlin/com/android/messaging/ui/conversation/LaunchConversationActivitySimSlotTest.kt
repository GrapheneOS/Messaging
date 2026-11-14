package com.android.messaging.ui.conversation

import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.android.messaging.datamodel.data.ParticipantData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LaunchConversationActivitySimSlotTest {

    private val subscriptionManager = mockk<SubscriptionManager>()
    private val telephonyManager = mockk<TelephonyManager>()

    @Before
    fun setUp() {
        every { telephonyManager.isEmergencyNumber(any()) } returns false
        every { telephonyManager.isEmergencyNumber(EMERGENCY_NUMBER) } returns true

        val info = mockk<SubscriptionInfo>()
        every { info.subscriptionId } returns SATELLITE_SUB_ID
        every { subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1) } returns info
    }

    @Test
    fun getSelfSubId_sendsAnEmergencyConversationFromTheSubscriptionInTheSlot() {
        assertEquals(SATELLITE_SUB_ID, getSelfSubId(arrayOf(EMERGENCY_NUMBER), slotIndex = 1))
    }

    @Test
    fun getSelfSubId_ignoresTheSlotForAConversationWithAnyoneElse() {
        assertEquals(
            ParticipantData.DEFAULT_SELF_SUB_ID,
            getSelfSubId(arrayOf(EMERGENCY_NUMBER, "+15551230000"), slotIndex = 1),
        )
        verify(exactly = 0) { subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(any()) }
    }

    @Test
    fun getSelfSubId_usesTheDefaultWithoutASlot() {
        assertEquals(
            ParticipantData.DEFAULT_SELF_SUB_ID,
            getSelfSubId(
                arrayOf(EMERGENCY_NUMBER),
                slotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX,
            ),
        )
    }

    @Test
    fun getSelfSubId_usesTheDefaultForAnEmptySlot() {
        every { subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0) } returns null

        assertEquals(
            ParticipantData.DEFAULT_SELF_SUB_ID,
            getSelfSubId(arrayOf(EMERGENCY_NUMBER), slotIndex = 0),
        )
    }

    @Test
    fun getSelfSubId_usesTheDefaultWhenSubscriptionsAreUnreadable() {
        every {
            subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1)
        } throws SecurityException("READ_PHONE_STATE revoked")

        assertEquals(
            ParticipantData.DEFAULT_SELF_SUB_ID,
            getSelfSubId(arrayOf(EMERGENCY_NUMBER), slotIndex = 1),
        )
    }

    private fun getSelfSubId(recipients: Array<String>, slotIndex: Int): Int {
        return LaunchConversationActivity.getSelfSubId(
            subscriptionManager,
            telephonyManager,
            recipients,
            slotIndex,
        )
    }

    private companion object {
        private const val EMERGENCY_NUMBER = "911"
        private const val SATELLITE_SUB_ID = 7
    }
}
