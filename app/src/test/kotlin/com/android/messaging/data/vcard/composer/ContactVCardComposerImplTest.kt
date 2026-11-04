package com.android.messaging.data.vcard.composer

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.Website
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContactsEntity
import androidx.test.core.app.ApplicationProvider
import com.android.messaging.datamodel.MediaScratchFileProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.util.Base64
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ContactVCardComposerImplTest {

    private val contentResolver = mockk<ContentResolver>()
    private val scratchOutput = ByteArrayOutputStream()

    private val composer = ContactVCardComposerImpl(
        context = ApplicationProvider.getApplicationContext<Context>(),
        contentResolver = contentResolver,
    )

    @Before
    fun setUp() {
        mockkStatic(MediaScratchFileProvider::class)
        every { MediaScratchFileProvider.buildMediaScratchSpaceUri("vcf") } returns SCRATCH_URI
        every { contentResolver.openOutputStream(SCRATCH_URI) } returns scratchOutput
        every { contentResolver.delete(SCRATCH_URI, null, null) } returns 1
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun composeToScratchSpace_linkedRawContacts_writesOneVCard() {
        givenContact(contactUri = FIRST_CONTACT_URI, contactId = 1L, nameRawContactId = 10L)
        givenRawContacts(
            contactId = 1L,
            rows = listOf(
                nameRow(rawContactId = 10L, dataId = 100L, displayName = "Ada Lovelace"),
                websiteRow(rawContactId = 10L, dataId = 101L, url = "https://example.com/ada"),
                nameRow(rawContactId = 20L, dataId = 200L, displayName = "Ada Lovelace"),
                websiteRow(rawContactId = 20L, dataId = 201L, url = "https://example.com/ada"),
                signalRow(rawContactId = 20L, dataId = 202L),
            ),
        )

        val result = composer.composeToScratchSpace(
            contactUris = listOf(FIRST_CONTACT_URI),
            includePhotos = true,
        )

        assertEquals(SCRATCH_URI, result)
        val vCardLines = scratchLines()
        assertEquals(1, vCardLines.count { it == "BEGIN:VCARD" })
        assertEquals(1, vCardLines.count { it.startsWith("FN") })
        assertEquals(1, vCardLines.count { it.startsWith("URL") })
    }

    @Test
    fun composeToScratchSpace_sameNumberInDifferentFormats_keepsOneNumber() {
        givenContact(contactUri = FIRST_CONTACT_URI, contactId = 1L, nameRawContactId = 10L)
        givenRawContacts(
            contactId = 1L,
            rows = listOf(
                phoneRow(rawContactId = 10L, dataId = 100L, number = "(555) 000-1234"),
                phoneRow(rawContactId = 10L, dataId = 101L, number = "555 000 9876"),
                phoneRow(rawContactId = 20L, dataId = 200L, number = "+15550001234"),
                phoneRow(rawContactId = 20L, dataId = 201L, number = "555-000-9876"),
            ),
        )

        composer.composeToScratchSpace(
            contactUris = listOf(FIRST_CONTACT_URI),
            includePhotos = true,
        )

        val phoneLines = scratchLines().filter { it.startsWith("TEL") }
        assertEquals(2, phoneLines.size)
        assertTrue(phoneLines.none { it.contains("+15550001234") })
    }

    @Test
    fun composeToScratchSpace_differentNormalizedNumbers_keepsBothNumbers() {
        givenContact(contactUri = FIRST_CONTACT_URI, contactId = 1L, nameRawContactId = 10L)
        givenRawContacts(
            contactId = 1L,
            rows = listOf(
                phoneRow(
                    rawContactId = 10L,
                    dataId = 100L,
                    number = "5550001234",
                    normalizedNumber = "+15550001234",
                ),
                phoneRow(
                    rawContactId = 20L,
                    dataId = 200L,
                    number = "+445550001234",
                    normalizedNumber = "+445550001234",
                ),
            ),
        )

        composer.composeToScratchSpace(
            contactUris = listOf(FIRST_CONTACT_URI),
            includePhotos = true,
        )

        assertEquals(2, scratchLines().count { it.startsWith("TEL") })
    }

    @Test
    fun composeToScratchSpace_sameNumberWithDifferentPostDial_keepsBothNumbers() {
        givenContact(contactUri = FIRST_CONTACT_URI, contactId = 1L, nameRawContactId = 10L)
        givenRawContacts(
            contactId = 1L,
            rows = listOf(
                phoneRow(
                    rawContactId = 10L,
                    dataId = 100L,
                    number = "555 000 1234",
                    normalizedNumber = "+15550001234",
                ),
                phoneRow(
                    rawContactId = 20L,
                    dataId = 200L,
                    number = "555 000 1234,9",
                    normalizedNumber = "+15550001234",
                ),
            ),
        )

        composer.composeToScratchSpace(
            contactUris = listOf(FIRST_CONTACT_URI),
            includePhotos = true,
        )

        assertEquals(2, scratchLines().count { it.startsWith("TEL") })
    }

    @Test
    fun composeToScratchSpace_namesDiffer_usesNameOfDisplayedRawContact() {
        givenContact(contactUri = FIRST_CONTACT_URI, contactId = 1L, nameRawContactId = 20L)
        givenRawContacts(
            contactId = 1L,
            rows = listOf(
                nameRow(
                    rawContactId = 10L,
                    dataId = 100L,
                    displayName = "Ada (Signal)",
                    isPrimary = true,
                ),
                nameRow(rawContactId = 20L, dataId = 200L, displayName = "Ada Lovelace"),
            ),
        )

        composer.composeToScratchSpace(
            contactUris = listOf(FIRST_CONTACT_URI),
            includePhotos = true,
        )

        assertEquals(
            listOf("FN:Ada Lovelace"),
            scratchLines().filter { it.startsWith("FN") },
        )
    }

    @Test
    fun composeToScratchSpace_photoPerRawContact_keepsContactPhoto() {
        givenContact(
            contactUri = FIRST_CONTACT_URI,
            contactId = 1L,
            nameRawContactId = 10L,
            photoId = 200L,
        )
        givenRawContacts(
            contactId = 1L,
            rows = listOf(
                photoRow(rawContactId = 10L, dataId = 100L, photo = firstPhotoBytes),
                photoRow(rawContactId = 20L, dataId = 200L, photo = secondPhotoBytes),
            ),
        )

        composer.composeToScratchSpace(
            contactUris = listOf(FIRST_CONTACT_URI),
            includePhotos = true,
        )

        val vCardText = scratchOutput.toString(Charsets.UTF_8.name())
        assertEquals(1, scratchLines().count { it.startsWith("PHOTO") })
        assertTrue(vCardText.contains(secondPhotoBytes.toBase64()))
        assertFalse(vCardText.contains(firstPhotoBytes.toBase64()))
    }

    @Test
    fun composeToScratchSpace_photosExcluded_writesNoPhoto() {
        givenContact(
            contactUri = FIRST_CONTACT_URI,
            contactId = 1L,
            nameRawContactId = 10L,
            photoId = 101L,
        )
        givenRawContacts(
            contactId = 1L,
            rows = listOf(
                nameRow(rawContactId = 10L, dataId = 100L, displayName = "Ada"),
                photoRow(rawContactId = 10L, dataId = 101L, photo = firstPhotoBytes),
            ),
        )

        composer.composeToScratchSpace(
            contactUris = listOf(FIRST_CONTACT_URI),
            includePhotos = false,
        )

        val vCardLines = scratchLines()
        assertEquals(listOf("FN:Ada"), vCardLines.filter { it.startsWith("FN") })
        assertTrue(vCardLines.none { it.startsWith("PHOTO") })
    }

    @Test
    fun composeToScratchSpace_severalContacts_writesVCardPerContact() {
        givenContact(contactUri = FIRST_CONTACT_URI, contactId = 1L, nameRawContactId = 10L)
        givenRawContacts(
            contactId = 1L,
            rows = listOf(nameRow(rawContactId = 10L, dataId = 100L, displayName = "Ada")),
        )
        givenContact(contactUri = SECOND_CONTACT_URI, contactId = 2L, nameRawContactId = 30L)
        givenRawContacts(
            contactId = 2L,
            rows = listOf(nameRow(rawContactId = 30L, dataId = 300L, displayName = "Grace")),
        )

        composer.composeToScratchSpace(
            contactUris = listOf(FIRST_CONTACT_URI, SECOND_CONTACT_URI),
            includePhotos = true,
        )

        assertEquals(
            listOf("FN:Ada", "FN:Grace"),
            scratchLines().filter { it.startsWith("FN") },
        )
    }

    @Test
    fun composeToScratchSpace_contactMissing_returnsNullWithoutScratchFile() {
        every {
            contentResolver.query(FIRST_CONTACT_URI, any(), any(), any(), any())
        } returns MatrixCursor(CONTACT_COLUMNS)

        val result = composer.composeToScratchSpace(
            contactUris = listOf(FIRST_CONTACT_URI),
            includePhotos = true,
        )

        assertNull(result)
        verify(exactly = 0) { MediaScratchFileProvider.buildMediaScratchSpaceUri(any()) }
    }

    @Test
    fun composeToScratchSpace_writeFails_deletesScratchFileAndReturnsNull() {
        givenContact(contactUri = FIRST_CONTACT_URI, contactId = 1L, nameRawContactId = 10L)
        givenRawContacts(
            contactId = 1L,
            rows = listOf(nameRow(rawContactId = 10L, dataId = 100L, displayName = "Ada")),
        )
        every { contentResolver.openOutputStream(SCRATCH_URI) } throws FileNotFoundException()

        val result = composer.composeToScratchSpace(
            contactUris = listOf(FIRST_CONTACT_URI),
            includePhotos = true,
        )

        assertNull(result)
        verify { contentResolver.delete(SCRATCH_URI, null, null) }
    }

    private fun givenContact(
        contactUri: Uri,
        contactId: Long,
        nameRawContactId: Long,
        photoId: Long? = null,
    ) {
        every {
            contentResolver.query(contactUri, any(), any(), any(), any())
        } returns MatrixCursor(CONTACT_COLUMNS).apply {
            addRow(arrayOf<Any?>(contactId, nameRawContactId, photoId))
        }
    }

    private fun givenRawContacts(contactId: Long, rows: List<Map<String, Any?>>) {
        every {
            contentResolver.query(
                RawContactsEntity.CONTENT_URI,
                any(),
                any(),
                match { selectionArgs -> selectionArgs.contentEquals(arrayOf("$contactId")) },
                any(),
            )
        } answers {
            MatrixCursor(RAW_CONTACT_ENTITY_COLUMNS).apply {
                rows.forEach { row ->
                    addRow(RAW_CONTACT_ENTITY_COLUMNS.map(row::get))
                }
            }
        }
    }

    private fun nameRow(
        rawContactId: Long,
        dataId: Long,
        displayName: String,
        isPrimary: Boolean = false,
    ): Map<String, Any?> {
        return dataRow(
            rawContactId = rawContactId,
            dataId = dataId,
            mimeType = StructuredName.CONTENT_ITEM_TYPE,
            isPrimary = isPrimary,
            StructuredName.DISPLAY_NAME to displayName,
            StructuredName.GIVEN_NAME to displayName,
        )
    }

    private fun phoneRow(
        rawContactId: Long,
        dataId: Long,
        number: String,
        normalizedNumber: String? = null,
    ): Map<String, Any?> {
        return dataRow(
            rawContactId = rawContactId,
            dataId = dataId,
            mimeType = Phone.CONTENT_ITEM_TYPE,
            isPrimary = false,
            Phone.NUMBER to number,
            Phone.NORMALIZED_NUMBER to normalizedNumber,
            Phone.TYPE to Phone.TYPE_MOBILE,
        )
    }

    private fun websiteRow(rawContactId: Long, dataId: Long, url: String): Map<String, Any?> {
        return dataRow(
            rawContactId = rawContactId,
            dataId = dataId,
            mimeType = Website.CONTENT_ITEM_TYPE,
            isPrimary = false,
            Website.URL to url,
            Website.TYPE to Website.TYPE_HOMEPAGE,
        )
    }

    private fun photoRow(rawContactId: Long, dataId: Long, photo: ByteArray): Map<String, Any?> {
        return dataRow(
            rawContactId = rawContactId,
            dataId = dataId,
            mimeType = Photo.CONTENT_ITEM_TYPE,
            isPrimary = false,
            Photo.PHOTO to photo,
        )
    }

    private fun signalRow(rawContactId: Long, dataId: Long): Map<String, Any?> {
        return dataRow(
            rawContactId = rawContactId,
            dataId = dataId,
            mimeType = SIGNAL_MIME_TYPE,
            isPrimary = false,
            Data.DATA1 to "+15550001234",
        )
    }

    private fun dataRow(
        rawContactId: Long,
        dataId: Long,
        mimeType: String,
        isPrimary: Boolean,
        vararg dataColumns: Pair<String, Any?>,
    ): Map<String, Any?> {
        return mapOf(
            RawContactsEntity._ID to rawContactId,
            RawContactsEntity.DATA_ID to dataId,
            Data.MIMETYPE to mimeType,
            Data.IS_PRIMARY to when {
                isPrimary -> 1
                else -> 0
            },
            Data.IS_SUPER_PRIMARY to 0,
        ) + dataColumns
    }

    private fun scratchLines(): List<String> {
        return scratchOutput.toString(Charsets.UTF_8.name()).lines()
    }

    private fun ByteArray.toBase64(): String {
        return Base64.getEncoder().encodeToString(this)
    }

    private companion object {
        private val FIRST_CONTACT_URI: Uri =
            Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath("lookup-key-1").build()
        private val SECOND_CONTACT_URI: Uri =
            Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath("lookup-key-2").build()
        private val SCRATCH_URI: Uri =
            Uri.parse("content://${MediaScratchFileProvider.AUTHORITY}/contact.vcf")

        private const val SIGNAL_MIME_TYPE =
            "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact"

        private val CONTACT_COLUMNS = arrayOf(
            Contacts._ID,
            Contacts.NAME_RAW_CONTACT_ID,
            Contacts.PHOTO_ID,
        )

        private val RAW_CONTACT_ENTITY_COLUMNS = arrayOf(
            RawContactsEntity._ID,
            RawContactsEntity.DATA_ID,
            Data.MIMETYPE,
            Data.IS_PRIMARY,
            Data.IS_SUPER_PRIMARY,
        ) + (1..15).map { index -> "data$index" } + (1..4).map { index -> "data_sync$index" }

        private val firstPhotoBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x01, 0x02)
        private val secondPhotoBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x03, 0x04)
    }
}
