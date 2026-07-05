package com.android.messaging.data.vcard.mapper

import com.android.messaging.data.vcard.model.VCardAvatarPhoto
import com.android.messaging.data.vcard.photo.VCardPhotoDownscaler
import com.android.messaging.datamodel.media.CustomVCardEntry
import com.android.vcard.VCardConfig
import com.android.vcard.VCardProperty
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VCardEntrySummarizerImplTest {

    private val photoDownscaler = mockk<VCardPhotoDownscaler>()

    private val summarizer = VCardEntrySummarizerImpl(
        photoDownscaler = photoDownscaler,
    )

    @Test
    fun displayName_fromFormattedName() {
        assertEquals("Ada Lovelace", summarizer.displayName(entryWith("FN" to "Ada Lovelace")))
    }

    @Test
    fun displayName_missing_isNull() {
        assertNull(summarizer.displayName(entryWith()))
    }

    @Test
    fun avatarPhoto_withPhoto_returnsDownscaledBytes() {
        val photoBytes = byteArrayOf(1, 2, 3)
        val downscaled = byteArrayOf(9)
        every { photoDownscaler.downscale(photoBytes) } returns downscaled

        val entry = entryWith("FN" to "Ada")
        entry.addProperty(
            VCardProperty().apply {
                setName("PHOTO")
                setByteValue(photoBytes)
            },
        )

        assertEquals(VCardAvatarPhoto(downscaled), summarizer.avatarPhoto(entry))
    }

    @Test
    fun avatarPhoto_withoutPhoto_isNullAndSkipsDownscaler() {
        assertNull(summarizer.avatarPhoto(entryWith("FN" to "Ada")))
        verify(exactly = 0) { photoDownscaler.downscale(any()) }
    }

    @Test
    fun isLocation_matchesKindPropertyIgnoringCase() {
        assertTrue(summarizer.isLocation(entryWith("KIND" to "Location")))
        assertFalse(summarizer.isLocation(entryWith("KIND" to "individual")))
        assertFalse(summarizer.isLocation(entryWith()))
    }

    @Test
    fun firstPostalAddress_returnsFormattedAddress() {
        val entry = entryWith("FN" to "Ada")
        entry.addProperty(
            VCardProperty().apply {
                setName("ADR")
                setValues("", "", "1 Engine Way", "London", "", "NW1", "UK")
            },
        )
        val address = summarizer.firstPostalAddress(entry)

        assertTrue(requireNotNull(address).contains("1 Engine Way"))
    }

    @Test
    fun firstPostalAddress_withoutPostal_isNull() {
        assertNull(summarizer.firstPostalAddress(entryWith("FN" to "Ada")))
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
