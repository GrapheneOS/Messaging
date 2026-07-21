package com.android.messaging.data.contact.formatter

import com.android.messaging.data.phone.formatter.PhoneNumberFormatter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ContactDestinationFormatterImplTest {

    private val phoneNumberFormatter = mockk<PhoneNumberFormatter>()
    private val formatter = ContactDestinationFormatterImpl(
        phoneNumberFormatter = phoneNumberFormatter,
    )

    @Test
    fun canonicalize_whenValueIsBlank_returnsTrimmedBlank() {
        assertEquals("", formatter.canonicalize("   "))
        verify(exactly = 0) {
            phoneNumberFormatter.getCanonicalForEnteredNumber(any())
        }
    }

    @Test
    fun canonicalize_whenValueIsEmail_trimsAndLowercasesEmail() {
        assertEquals("alice@example.com", formatter.canonicalize("  Alice@Example.COM  "))
        verify(exactly = 0) {
            phoneNumberFormatter.getCanonicalForEnteredNumber(any())
        }
    }

    @Test
    fun canonicalize_whenValueIsPhone_trimsCanonicalizesAndStripsSeparators() {
        every {
            phoneNumberFormatter.getCanonicalForEnteredNumber("+1 (555) 123-4567")
        } returns "+1 (555) 123-4567"

        assertEquals("+15551234567", formatter.canonicalize("  +1 (555) 123-4567  "))
    }

    @Test
    fun canonicalize_withCountryCandidates_passesCandidatesToPhoneNumberFormatter() {
        val countryCandidates = listOf("US", "CA")
        every {
            phoneNumberFormatter.getCanonicalForEnteredNumber("555.123/4567", countryCandidates)
        } returns "555.123/4567"

        assertEquals(
            "5551234567",
            formatter.canonicalize("555.123/4567", countryCandidates),
        )
    }

    @Test
    fun formatPhoneForDisplay_delegatesToPhoneNumberFormatter() {
        every { phoneNumberFormatter.formatForDisplay("+15551234") } returns "+1 555-1234"

        assertEquals("+1 555-1234", formatter.formatPhoneForDisplay("+15551234"))
    }

    @Test
    fun countryCandidates_delegatesToPhoneNumberFormatter() {
        val countryCandidates = listOf("US", "CA")
        every { phoneNumberFormatter.countryCandidates() } returns countryCandidates

        assertEquals(countryCandidates, formatter.countryCandidates())
    }
}
