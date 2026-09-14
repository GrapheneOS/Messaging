package com.android.messaging.ui.vcarddetail.screen.mapper

import com.android.messaging.data.vcarddetail.model.VCardContact
import com.android.messaging.data.vcarddetail.model.VCardDetailResult
import com.android.messaging.ui.vcarddetail.screen.model.VCardContactUiModel
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class VCardDetailUiStateMapperImplTest {

    private val mapper = VCardDetailUiStateMapperImpl()

    @Test
    fun map_loading_isLoadingWithoutContacts() {
        val uiState = mapper.map(VCardDetailResult.Loading)

        assertTrue(uiState.isLoading)
        assertTrue(uiState.contacts.isEmpty())
        assertFalse(uiState.canAddToContacts)
    }

    @Test
    fun map_failed_isNotLoadingWithoutContacts() {
        val uiState = mapper.map(VCardDetailResult.Failed)

        assertFalse(uiState.isLoading)
        assertTrue(uiState.contacts.isEmpty())
        assertFalse(uiState.canAddToContacts)
    }

    @Test
    fun map_loadedWithContacts_exposesContactsAndAllowsAddToContacts() {
        val contacts = persistentListOf(contact("Ada Lovelace"))
        val expectedContacts = persistentListOf(
            VCardContactUiModel(
                displayName = "Ada Lovelace",
                normalizedDestination = "+15550001",
                avatarPhoto = null,
                fields = persistentListOf(),
            ),
        )
        val uiState = mapper.map(VCardDetailResult.Loaded(contacts))

        assertFalse(uiState.isLoading)
        assertEquals(expectedContacts, uiState.contacts)
        assertTrue(uiState.canAddToContacts)
    }

    @Test
    fun map_loadedWithoutContacts_disallowsAddToContacts() {
        val uiState = mapper.map(VCardDetailResult.Loaded(persistentListOf()))

        assertFalse(uiState.isLoading)
        assertTrue(uiState.contacts.isEmpty())
        assertFalse(uiState.canAddToContacts)
    }

    private fun contact(displayName: String): VCardContact {
        return VCardContact(
            displayName = displayName,
            normalizedDestination = "+15550001",
            avatarPhoto = null,
            fields = persistentListOf(),
        )
    }
}
