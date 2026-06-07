package com.android.messaging.ui.shareintent.screen.mapper

import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ShareContactSectionMapperImplTest {

    private val mapper = ShareContactSectionMapperImpl()

    @Test
    fun map_groupsContactsByUppercaseFirstLetterPreservingOrder() {
        val alex = contact(
            contactId = 1L,
            displayName = "Alex",
        )
        val brian = contact(
            contactId = 2L,
            displayName = "Brian",
        )

        val result = mapper.map(persistentListOf(alex, brian))

        assertEquals(listOf("A", "B"), result.map { it.label })
        assertEquals(listOf(alex), result[0].targets)
        assertEquals(listOf(brian), result[1].targets)
    }

    @Test
    fun map_putsDifferentCaseOfSameLetterIntoOneSection() {
        val lower = contact(
            contactId = 1L,
            displayName = "alex",
        )
        val upper = contact(
            contactId = 2L,
            displayName = "Amelia",
        )

        val result = mapper.map(persistentListOf(lower, upper))

        assertEquals(1, result.size)
        assertEquals("A", result.single().label)
        assertEquals(listOf(lower, upper), result.single().targets)
    }

    @Test
    fun map_putsNonLetterFirstCharacterIntoHashSection() {
        val numeric = contact(
            contactId = 1L,
            displayName = "+1 555 0100",
        )
        val symbol = contact(
            contactId = 2L,
            displayName = "#tag",
        )

        val result = mapper.map(persistentListOf(numeric, symbol))

        assertEquals(1, result.size)
        assertEquals("#", result.single().label)
        assertEquals(listOf(numeric, symbol), result.single().targets)
    }

    @Test
    fun map_ignoresLeadingWhitespaceWhenChoosingSection() {
        val padded = contact(
            contactId = 1L,
            displayName = "   Zoe",
        )

        val result = mapper.map(persistentListOf(padded))

        assertEquals("Z", result.single().label)
    }

    @Test
    fun map_returnsEmptyListForEmptyInput() {
        val result = mapper.map(persistentListOf())

        assertTrue(result.isEmpty())
    }

    @Test
    fun map_usesHashSectionForBlankDisplayName() {
        val blank = contact(
            contactId = 1L,
            displayName = "   ",
        )

        val result = mapper.map(persistentListOf(blank))

        assertEquals("#", result.single().label)
    }

    private fun contact(
        contactId: Long,
        displayName: String,
    ): ShareTargetUiState.Contact {
        return ShareTargetUiState.Contact(
            contactId = contactId,
            destination = "+1000$contactId",
            normalizedDestination = "+1000$contactId",
            displayName = displayName,
            details = null,
            avatarUri = null,
        )
    }
}
