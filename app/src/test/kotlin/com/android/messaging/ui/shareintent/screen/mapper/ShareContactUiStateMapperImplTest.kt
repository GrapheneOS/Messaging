package com.android.messaging.ui.shareintent.screen.mapper

import com.android.messaging.data.contact.model.Contact
import com.android.messaging.data.contact.model.ContactDestination
import com.android.messaging.ui.shareintent.screen.formatter.ShareTargetTextFormatter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ShareContactUiStateMapperImplTest {

    private val textFormatter = mockk<ShareTargetTextFormatter> {
        every { wrap(any()) } answers { "wrapped:${firstArg<String>()}" }
        every { detailsOrNull(any(), any()) } answers {
            secondArg<String?>()?.let { "details:$it" }
        }
    }

    private val mapper = ShareContactUiStateMapperImpl(textFormatter = textFormatter)

    @Test
    fun map_mapsContactWithSingleDestinationIntoUiState() {
        val contact = contact(
            id = 10L,
            displayName = "Name",
            photoUri = "content://photo/10",
            destinations = listOf(
                destination(
                    value = "+1 555 0100",
                    normalizedValue = "+15550100",
                    displayValue = "+1 555 0100",
                ),
            ),
        )

        val result = mapper.map(persistentListOf(contact)).single()

        verify { textFormatter.detailsOrNull(name = "Name", value = "+1 555 0100") }

        assertEquals(10L, result.contactId)
        assertEquals("+1 555 0100", result.destination)
        assertEquals("+15550100", result.normalizedDestination)
        assertEquals("wrapped:Name", result.displayName)
        assertEquals("details:+1 555 0100", result.details)
        assertEquals("content://photo/10", result.avatarUri)
    }

    @Test
    fun map_usesFirstDestinationWhenContactHasMultiple() {
        val contact = contact(
            id = 11L,
            displayName = "Name",
            destinations = listOf(
                destination(
                    value = "+15550001",
                    normalizedValue = "+15550001",
                ),
                destination(
                    value = "+15550002",
                    normalizedValue = "+15550002",
                ),
            ),
        )

        val result = mapper.map(persistentListOf(contact)).single()

        assertEquals("+15550001", result.destination)
        assertEquals("+15550001", result.normalizedDestination)
    }

    @Test
    fun map_fallsBackToDestinationDisplayValueWhenDisplayNameBlank() {
        val contact = contact(
            id = 13L,
            displayName = "   ",
            destinations = listOf(
                destination(
                    value = "+15550003",
                    displayValue = "+1 555 0003",
                ),
            ),
        )

        val result = mapper.map(persistentListOf(contact)).single()

        assertEquals("wrapped:+1 555 0003", result.displayName)
    }

    @Test
    fun map_setsNullDetailsWhenFormatterReturnsNull() {
        every { textFormatter.detailsOrNull(any(), any()) } returns null

        val contact = contact(
            id = 14L,
            displayName = "Name",
            destinations = listOf(destination(value = "+15550004")),
        )

        val result = mapper.map(persistentListOf(contact)).single()

        assertEquals(null, result.details)
    }

    @Test
    fun map_dropsOnlyContactsWithoutDestinationsAndKeepsTheRest() {
        val withDestination = contact(
            id = 15L,
            displayName = "Name",
            destinations = listOf(destination(value = "+15550005")),
        )
        val withoutDestination = contact(
            id = 16L,
            displayName = "Name",
            destinations = emptyList(),
        )

        val result = mapper.map(persistentListOf(withDestination, withoutDestination))

        assertEquals(listOf(15L), result.map { it.contactId })
    }

    @Test
    fun map_returnsEmptyListForEmptyInput() {
        val result = mapper.map(persistentListOf())

        assertTrue(result.isEmpty())
    }

    private fun contact(
        id: Long,
        displayName: String,
        destinations: List<ContactDestination>,
        photoUri: String? = null,
    ): Contact {
        return Contact(
            id = id,
            lookupKey = "lookup_$id",
            displayName = displayName,
            photoUri = photoUri,
            destinations = destinations.toImmutableList(),
        )
    }

    private fun destination(
        value: String,
        normalizedValue: String = value,
        displayValue: String = value,
    ): ContactDestination {
        return ContactDestination(
            dataId = value.hashCode().toLong(),
            contactId = 0L,
            value = value,
            normalizedValue = normalizedValue,
            displayValue = displayValue,
            kind = ContactDestination.Kind.PHONE,
            type = 1,
            customLabel = null,
            isPrimary = false,
            isSuperPrimary = false,
        )
    }
}
