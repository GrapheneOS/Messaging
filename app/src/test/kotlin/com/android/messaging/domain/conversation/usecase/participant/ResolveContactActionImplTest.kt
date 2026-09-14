package com.android.messaging.domain.conversation.usecase.participant

import com.android.messaging.domain.conversation.usecase.participant.model.ResolveContactActionResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ResolveContactActionImplTest {

    private val isContactSaved = mockk<IsContactSaved>()
    private val canAddContact = mockk<CanAddContact>()

    private val resolveContactAction = ResolveContactActionImpl(
        isContactSaved = isContactSaved,
        canAddContact = canAddContact,
    )

    @Test
    fun invoke_whenContactIsSaved_returnsShowContactCard() {
        every { isContactSaved(any(), any()) } returns true
        every { canAddContact(any(), any(), any()) } returns false

        val result = resolveContactAction(
            contactId = CONTACT_ID,
            lookupKey = LOOKUP_KEY,
            destination = DESTINATION,
        )

        assertEquals(
            ResolveContactActionResult.ShowContactCard(
                contactId = CONTACT_ID,
                lookupKey = LOOKUP_KEY,
            ),
            result,
        )
    }

    @Test
    fun invoke_whenContactIsSavedWithoutLookupKey_returnsUnavailable() {
        every { isContactSaved(any(), any()) } returns true
        every { canAddContact(any(), any(), any()) } returns false

        val result = resolveContactAction(
            contactId = CONTACT_ID,
            lookupKey = null,
            destination = DESTINATION,
        )

        assertEquals(ResolveContactActionResult.Unavailable, result)
    }

    @Test
    fun invoke_whenContactIsNotSavedAndDestinationIsAddable_returnsAddContact() {
        every { isContactSaved(any(), any()) } returns false
        every { canAddContact(any(), any(), any()) } returns true

        val result = resolveContactAction(
            contactId = CONTACT_ID,
            lookupKey = null,
            destination = DESTINATION,
        )

        assertEquals(
            ResolveContactActionResult.AddContact(destination = DESTINATION),
            result,
        )
    }

    @Test
    fun invoke_whenContactIsNotSavedAndDestinationIsMissing_returnsUnavailable() {
        every { isContactSaved(any(), any()) } returns false
        every { canAddContact(any(), any(), any()) } returns true

        val result = resolveContactAction(
            contactId = CONTACT_ID,
            lookupKey = null,
            destination = null,
        )

        assertEquals(ResolveContactActionResult.Unavailable, result)
    }

    @Test
    fun invoke_whenContactIsNotSavedAndDestinationIsNotAddable_returnsUnavailable() {
        every { isContactSaved(any(), any()) } returns false
        every { canAddContact(any(), any(), any()) } returns false

        val result = resolveContactAction(
            contactId = CONTACT_ID,
            lookupKey = null,
            destination = DESTINATION,
        )

        assertEquals(ResolveContactActionResult.Unavailable, result)
    }

    @Test
    fun invoke_resolvesAddContactWithoutGroupSemantics() {
        every { isContactSaved(any(), any()) } returns false
        every {
            canAddContact(
                isGroup = false,
                lookupKey = null,
                destination = DESTINATION,
            )
        } returns true

        val result = resolveContactAction(
            contactId = CONTACT_ID,
            lookupKey = null,
            destination = DESTINATION,
        )

        assertEquals(
            ResolveContactActionResult.AddContact(destination = DESTINATION),
            result,
        )
    }

    private companion object {
        private const val CONTACT_ID = 42L
        private const val LOOKUP_KEY = "lookup-key"
        private const val DESTINATION = "+15551234567"
    }
}
