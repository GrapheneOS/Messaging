package com.android.messaging.data.vcard.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.provider.ContactsContract.Contacts
import app.cash.turbine.test
import com.android.messaging.data.vcard.parser.VCardParser
import com.android.messaging.datamodel.media.CustomVCardEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VCardEntryRepositoryImplTest {

    private val parser = mockk<VCardParser>()
    private val contentResolver = mockk<ContentResolver>(relaxed = true)
    private val repository = VCardEntryRepositoryImpl(
        parser = parser,
        contentResolver = contentResolver,
    )

    @Test
    fun getEntries_staticUri_reusesCachedEntries() = runTest {
        val entries = listOf(mockk<CustomVCardEntry>())
        coEvery { parser.parse(STATIC_VCARD_URI) } returns entries

        assertEquals(entries, repository.getEntries(STATIC_VCARD_URI))
        assertEquals(entries, repository.getEntries(STATIC_VCARD_URI))
        coVerify(exactly = 1) { parser.parse(STATIC_VCARD_URI) }
    }

    @Test
    fun getEntries_contactVCardUri_doesNotReuseCachedEntries() = runTest {
        val firstEntries = listOf(mockk<CustomVCardEntry>())
        val secondEntries = listOf(mockk<CustomVCardEntry>())
        coEvery { parser.parse(CONTACT_VCARD_URI) } returnsMany listOf(
            firstEntries,
            secondEntries,
        )

        assertEquals(firstEntries, repository.getEntries(CONTACT_VCARD_URI))
        assertEquals(secondEntries, repository.getEntries(CONTACT_VCARD_URI))
        coVerify(exactly = 2) { parser.parse(CONTACT_VCARD_URI) }
    }

    @Test
    fun observeEntries_contactVCardUri_reloadsWhenContactsChange() = runTest {
        val observerSlot = slot<ContentObserver>()
        every {
            contentResolver.registerContentObserver(
                Contacts.CONTENT_URI,
                true,
                capture(observerSlot),
            )
        } just runs
        every { contentResolver.unregisterContentObserver(any()) } just runs

        val firstEntries = listOf(mockk<CustomVCardEntry>())
        val secondEntries = listOf(mockk<CustomVCardEntry>())
        coEvery { parser.parse(CONTACT_VCARD_URI) } returnsMany listOf(
            firstEntries,
            secondEntries,
        )

        repository.observeEntries(CONTACT_VCARD_URI).test {
            assertEquals(firstEntries, awaitItem())

            observerSlot.captured.onChange(false)

            assertEquals(secondEntries, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 2) { parser.parse(CONTACT_VCARD_URI) }
        verify { contentResolver.unregisterContentObserver(observerSlot.captured) }
    }

    private companion object {
        private const val STATIC_VCARD_URI = "content://messages/attachments/contact.vcf"
        private val CONTACT_VCARD_URI = Contacts.CONTENT_VCARD_URI
            .buildUpon()
            .appendPath("lookup-key")
            .build()
            .toString()
    }
}
