package com.android.messaging.data.vcarddetail.repository

import app.cash.turbine.test
import com.android.messaging.data.vcard.repository.VCardEntryRepository
import com.android.messaging.data.vcarddetail.mapper.VCardDetailMapper
import com.android.messaging.data.vcarddetail.model.VCardContact
import com.android.messaging.data.vcarddetail.model.VCardDetailResult
import com.android.messaging.datamodel.media.CustomVCardEntry
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class VCardDetailRepositoryImplTest {

    private val vCardEntryRepository = mockk<VCardEntryRepository>()
    private val mapper = mockk<VCardDetailMapper>()
    private val entries = listOf(mockk<CustomVCardEntry>())

    private val repository = VCardDetailRepositoryImpl(
        vCardEntryRepository = vCardEntryRepository,
        vCardDetailMapper = mapper,
    )

    @Test
    fun observeVCard_blankUri_emitsFailedAndCompletes() = runTest {
        repository.observeVCard("   ").test {
            assertEquals(VCardDetailResult.Failed, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun observeVCard_withContacts_emitsLoadingThenLoadedContacts() = runTest {
        givenParsedContacts(contact("Ada Lovelace"))

        repository.observeVCard(VCARD_URI).test {
            assertEquals(VCardDetailResult.Loading, awaitItem())

            val loaded = awaitItem() as VCardDetailResult.Loaded
            assertEquals(1, loaded.contacts.size)
            awaitComplete()
        }
    }

    @Test
    fun observeVCard_noContacts_emitsFailed() = runTest {
        givenParsedContacts()

        repository.observeVCard(VCARD_URI).test {
            assertEquals(VCardDetailResult.Loading, awaitItem())
            assertEquals(VCardDetailResult.Failed, awaitItem())
            awaitComplete()
        }
    }

    private fun givenParsedContacts(vararg contacts: VCardContact) {
        every {
            vCardEntryRepository.observeEntries(
                vCardUri = VCARD_URI,
                refreshes = any(),
            )
        } returns flowOf(entries)
        every { mapper.map(entries) } returns persistentListOf(*contacts)
    }

    private fun contact(displayName: String): VCardContact {
        return VCardContact(
            displayName = displayName,
            normalizedDestination = "+15550001",
            avatarPhoto = null,
            fields = persistentListOf(),
        )
    }

    private companion object {
        private const val VCARD_URI = "content://vcard"
    }
}
