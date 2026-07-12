package com.android.messaging.data.secondaryuser

import com.android.messaging.data.phone.formatter.PhoneNumberFormatter
import com.android.messaging.data.secondaryuser.model.SecondaryUserMessageInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class SecondaryUserMessageResolverTest {

    private val contactNameLookup = mockk<SmsContactNameLookup>()
    private val phoneNumberFormatter = mockk<PhoneNumberFormatter>()

    private val resolver = SecondaryUserMessageResolverImpl(
        contactNameLookup = contactNameLookup,
        phoneNumberFormatter = phoneNumberFormatter,
    )

    @Test
    fun resolve_contactFound_usesContactName() {
        every { contactNameLookup.lookup("+15551234") } returns "Alice"

        val info = resolver.resolve(address = "+15551234", body = "Hello there")
        assertEquals(
            SecondaryUserMessageInfo(sender = "Alice", body = "Hello there"),
            info,
        )
    }

    @Test
    fun resolve_noMatchingContact_fallsBackToFormattedNumber() {
        every { contactNameLookup.lookup("+15551234") } returns null
        every { phoneNumberFormatter.formatForDisplay("+15551234") } returns "+1 555-1234"

        val info = resolver.resolve(address = "+15551234", body = "Hello there")
        assertEquals(
            SecondaryUserMessageInfo(sender = "+1 555-1234", body = "Hello there"),
            info,
        )
    }

    @Test
    fun resolve_blankContactName_fallsBackToFormattedNumber() {
        every { contactNameLookup.lookup("+15551234") } returns ""
        every { phoneNumberFormatter.formatForDisplay("+15551234") } returns "+1 555-1234"

        val info = resolver.resolve(address = "+15551234", body = "Hello there")
        assertEquals("+1 555-1234", info?.sender)
    }

    @Test
    fun resolve_nullBody_returnsNull() {
        assertNull(resolver.resolve(address = "+15551234", body = null))
    }

    @Test
    fun resolve_emptyBody_returnsNull() {
        assertNull(resolver.resolve(address = "+15551234", body = ""))
    }

    @Test
    fun resolve_nullAddress_returnsNull() {
        assertNull(resolver.resolve(address = null, body = "Hello there"))
    }

    @Test
    fun resolve_emptyAddress_returnsNull() {
        assertNull(resolver.resolve(address = "", body = "Hello there"))
    }
}
