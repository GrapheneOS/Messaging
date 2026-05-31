package com.android.messaging.domain.conversation.usecase.telephony

import android.telephony.TelephonyManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IsEmergencyPhoneNumberImplTest {

    private val telephonyManager = mockk<TelephonyManager>()
    private val isEmergencyPhoneNumber = IsEmergencyPhoneNumberImpl(
        telephonyManager = telephonyManager,
    )

    @Test
    fun invoke_stripsSeparatorsBeforeCheckingTelephonyManager() {
        every { telephonyManager.isEmergencyNumber(EMERGENCY_NUMBER) } returns true

        val result = isEmergencyPhoneNumber("9-1-1")

        assertTrue(result)
        verify(exactly = 1) {
            telephonyManager.isEmergencyNumber(EMERGENCY_NUMBER)
        }
    }

    @Test
    fun invoke_returnsFalseWhenTelephonyManagerRejectsNumber() {
        every { telephonyManager.isEmergencyNumber(NON_EMERGENCY_NUMBER) } returns false

        val result = isEmergencyPhoneNumber(NON_EMERGENCY_NUMBER)

        assertFalse(result)
    }

    @Test
    fun invoke_fallsBackWhenTelephonyManagerStateIsUnavailable() {
        every {
            telephonyManager.isEmergencyNumber(EMERGENCY_NUMBER)
        } throws IllegalStateException("not ready")

        val result = isEmergencyPhoneNumber(EMERGENCY_NUMBER)

        assertTrue(result)
    }

    @Test
    fun invoke_fallsBackWhenTelephonyManagerDoesNotSupportEmergencyChecks() {
        every {
            telephonyManager.isEmergencyNumber(NON_EMERGENCY_NUMBER)
        } throws UnsupportedOperationException("unsupported")

        val result = isEmergencyPhoneNumber(NON_EMERGENCY_NUMBER)

        assertFalse(result)
    }

    private companion object {
        private const val EMERGENCY_NUMBER = "911"
        private const val NON_EMERGENCY_NUMBER = "5551212"
    }
}
