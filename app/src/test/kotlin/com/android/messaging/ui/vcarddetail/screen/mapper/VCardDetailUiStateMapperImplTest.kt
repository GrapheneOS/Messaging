package com.android.messaging.ui.vcarddetail.screen.mapper

import com.android.messaging.data.vcarddetail.model.VCardContact
import com.android.messaging.data.vcarddetail.model.VCardDetailResult
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
        val uiState = mapper.map(VCardDetailResult.Loaded(contacts))

        assertFalse(uiState.isLoading)
        assertEquals(contacts, uiState.contacts)
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
            avatarPhoto = null,
            fields = persistentListOf(),
        )
    }
}
