package com.android.messaging.data.vcarddetail.mapper

import com.android.messaging.data.vcard.mapper.VCardEntrySummarizerImpl
import com.android.messaging.data.vcard.model.VCardAvatarPhoto
import com.android.messaging.data.vcard.photo.VCardPhotoDownscaler
import com.android.messaging.data.vcarddetail.model.VCardFieldAction
import com.android.messaging.datamodel.media.CustomVCardEntry
import com.android.vcard.VCardConfig
import com.android.vcard.VCardProperty
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class VCardDetailMapperImplTest {

    private val photoDownscaler = mockk<VCardPhotoDownscaler>()

    private val mapper = VCardDetailMapperImpl(
        context = RuntimeEnvironment.getApplication().applicationContext,
        entrySummarizer = VCardEntrySummarizerImpl(
            photoDownscaler = photoDownscaler,
        ),
    )

    @Test
    fun map_noEntries_returnsEmptyList() {
        val result = mapper.map(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun map_displayNameFromFormattedName() {
        val contact = mapper.map(listOf(entryWith("FN" to "Ada Lovelace"))).single()

        assertEquals("Ada Lovelace", contact.displayName)
    }

    @Test
    fun map_missingDisplayName_isNull() {
        val contact = mapper.map(listOf(entryWith())).single()

        assertNull(contact.displayName)
    }

    @Test
    fun map_phone_isDialFieldWithTypeLabel() {
        val entry = entryWith("FN" to "Ada", "TEL" to "+15550001")
        val field = mapper.map(listOf(entry)).single().fields.single()

        assertEquals(VCardFieldAction.Dial(field.value), field.action)
        assertTrue(field.value.contains("555"))
    }

    @Test
    fun map_email_isEmailField() {
        val entry = entryWith("FN" to "Ada", "EMAIL" to "ada@example.com")
        val field = mapper.map(listOf(entry)).single().fields.single()

        assertEquals("ada@example.com", field.value)
        assertEquals(VCardFieldAction.Email("ada@example.com"), field.action)
    }

    @Test
    fun map_postalAddress_isOpenMapFieldWithFormattedAddress() {
        val entry = entryWith("FN" to "Ada")
        entry.addProperty(
            VCardProperty().apply {
                setName("ADR")
                setValues("", "", "1 Engine Way", "London", "", "NW1", "UK")
            },
        )

        val field = mapper.map(listOf(entry)).single().fields.single()

        assertTrue(field.value.contains("1 Engine Way"))
        assertTrue(field.value.contains("London"))
        assertEquals(VCardFieldAction.OpenMap(field.value), field.action)
    }

    @Test
    fun map_organization_isFormattedWithDepartmentAndTitle() {
        val entry = entryWith("FN" to "Ada")
        entry.addProperty(
            VCardProperty().apply {
                setName("ORG")
                setValues("Analytical Engines", "Research")
            },
        )
        entry.addProperty(
            VCardProperty().apply {
                setName("TITLE")
                setValues("Programmer")
            },
        )
        val field = mapper.map(listOf(entry)).single().fields.single()

        assertEquals("Analytical Engines, Research, Programmer", field.value)
        assertEquals(VCardFieldAction.None, field.action)
    }

    @Test
    fun map_websiteWithoutScheme_opensUrlWithHttpPrefix() {
        val entry = entryWith("FN" to "Ada", "URL" to "example.com")
        val field = mapper.map(listOf(entry)).single().fields.single()

        assertEquals("example.com", field.value)
        assertEquals(VCardFieldAction.OpenUrl("http://example.com"), field.action)
    }

    @Test
    fun map_websiteWithScheme_keepsUrl() {
        val entry = entryWith("FN" to "Ada", "URL" to "https://example.com")
        val field = mapper.map(listOf(entry)).single().fields.single()

        assertEquals(VCardFieldAction.OpenUrl("https://example.com"), field.action)
    }

    @Test
    fun map_birthdayAndNotes_arePlainFields() {
        val entry = entryWith("FN" to "Ada", "BDAY" to "1815-12-10", "NOTE" to "First programmer")
        val fields = mapper.map(listOf(entry)).single().fields

        assertEquals(listOf("1815-12-10", "First programmer"), fields.map { it.value })
        assertEquals("Birthday", fields[0].label)
        assertEquals("Notes", fields[1].label)
        assertTrue(fields.all { it.action == VCardFieldAction.None })
    }

    @Test
    fun map_nicknameAndAnniversary_arePlainFields() {
        val entry = entryWith(
            "FN" to "Ada",
            "NICKNAME" to "Countess",
            "ANNIVERSARY" to "1835-07-08",
        )
        val fields = mapper.map(listOf(entry)).single().fields

        assertEquals(listOf("Countess", "1835-07-08"), fields.map { it.value })
        assertEquals("Nickname", fields[0].label)
        assertEquals("Anniversary", fields[1].label)
    }

    @Test
    fun map_photo_isDownscaledIntoAvatar() {
        val photoBytes = byteArrayOf(1, 2, 3)
        val downscaledBytes = byteArrayOf(9)
        every { photoDownscaler.downscale(photoBytes) } returns downscaledBytes

        val entry = entryWith("FN" to "Ada")
        entry.addProperty(
            VCardProperty().apply {
                setName("PHOTO")
                byteValue = photoBytes
            },
        )
        val contact = mapper.map(listOf(entry)).single()

        assertEquals(VCardAvatarPhoto(downscaledBytes), contact.avatarPhoto)
    }

    @Test
    fun map_photoThatFailsDownscaling_hasNoAvatar() {
        every { photoDownscaler.downscale(any()) } returns null

        val entry = entryWith("FN" to "Ada")
        entry.addProperty(
            VCardProperty().apply {
                setName("PHOTO")
                byteValue = byteArrayOf(1)
            },
        )
        val contact = mapper.map(listOf(entry)).single()

        assertNull(contact.avatarPhoto)
    }

    @Test
    fun map_noPhoto_hasNoAvatarAndSkipsDownscaler() {
        val contact = mapper.map(listOf(entryWith("FN" to "Ada"))).single()

        assertNull(contact.avatarPhoto)
        verify(exactly = 0) { photoDownscaler.downscale(any()) }
    }

    @Test
    fun map_multipleEntries_arePreservedInOrder() {
        val result = mapper.map(
            listOf(
                entryWith("FN" to "First"),
                entryWith("FN" to "Second"),
            ),
        )

        assertEquals(listOf("First", "Second"), result.map { it.displayName })
    }

    private fun entryWith(vararg properties: Pair<String, String>): CustomVCardEntry {
        val entry = CustomVCardEntry(VCardConfig.VCARD_TYPE_V21_GENERIC, null)

        properties.forEach { (name, value) ->
            entry.addProperty(
                VCardProperty().apply {
                    setName(name)
                    setValues(value)
                    rawValue = value
                },
            )
        }

        return entry
    }
}
