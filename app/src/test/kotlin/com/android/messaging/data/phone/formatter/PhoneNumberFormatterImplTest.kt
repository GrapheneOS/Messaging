package com.android.messaging.data.phone.formatter

import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

internal class PhoneNumberFormatterImplTest {

    private val phoneUtils = mockk<PhoneUtils>()
    private val formatter = PhoneNumberFormatterImpl(phoneUtils = phoneUtils)

    @Test
    fun formatForDisplay_delegatesToPhoneUtils() {
        every { phoneUtils.formatForDisplay(PHONE_NUMBER) } returns FORMATTED_PHONE_NUMBER

        assertEquals(FORMATTED_PHONE_NUMBER, formatter.formatForDisplay(PHONE_NUMBER))
    }

    @Test
    fun formatForDisplayUsingSimCountry_whenPhoneUtilsReturnsNull_returnsEmptyString() {
        every { phoneUtils.formatForDisplayUsingSimCountry(PHONE_NUMBER) } returns null

        assertEquals("", formatter.formatForDisplayUsingSimCountry(PHONE_NUMBER))
    }

    @Test
    fun formatNormalizedUsingSimCountry_whenPhoneUtilsReturnsNull_returnsEmptyString() {
        every { phoneUtils.formatNormalizedDestinationUsingSimCountry(PHONE_NUMBER) } returns null

        assertEquals("", formatter.formatNormalizedUsingSimCountry(PHONE_NUMBER))
    }

    @Test
    fun getCanonicalForEnteredNumber_delegatesToPhoneUtils() {
        every { phoneUtils.getCanonicalForEnteredPhoneNumber(PHONE_NUMBER) } returns
            CANONICAL_NUMBER

        assertEquals(CANONICAL_NUMBER, formatter.getCanonicalForEnteredNumber(PHONE_NUMBER))
    }

    @Test
    fun getCanonicalForEnteredNumber_withCountryCandidates_delegatesToPhoneUtils() {
        val countryCandidates = listOf("US", "CA")
        every {
            phoneUtils.getCanonicalForEnteredPhoneNumber(PHONE_NUMBER, countryCandidates)
        } returns CANONICAL_NUMBER

        assertEquals(
            CANONICAL_NUMBER,
            formatter.getCanonicalForEnteredNumber(PHONE_NUMBER, countryCandidates),
        )
    }

    @Test
    fun countryCandidates_warmsUpPhoneUtilsBeforeReadingCandidates() {
        val countryCandidates = listOf("US", "CA")
        every { phoneUtils.warmUp() } returns Unit
        every { phoneUtils.countryCandidatesForEnteredPhoneNumber } returns countryCandidates

        assertEquals(countryCandidates, formatter.countryCandidates())
        verify {
            phoneUtils.warmUp()
            phoneUtils.countryCandidatesForEnteredPhoneNumber
        }
    }

    private companion object {
        private const val PHONE_NUMBER = "+15551234"
        private const val FORMATTED_PHONE_NUMBER = "+1 555-1234"
        private const val CANONICAL_NUMBER = "+15551234"
    }
}
