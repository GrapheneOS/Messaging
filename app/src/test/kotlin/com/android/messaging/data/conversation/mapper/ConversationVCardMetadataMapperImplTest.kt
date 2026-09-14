package com.android.messaging.data.conversation.mapper

import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.data.vcard.mapper.VCardEntrySummarizer
import com.android.messaging.datamodel.media.CustomVCardEntry
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConversationVCardMetadataMapperImplTest {

    private val entrySummarizer = mockk<VCardEntrySummarizer>()
    private val mapper = ConversationVCardMetadataMapperImpl(
        entrySummarizer = entrySummarizer,
    )

    @Test
    fun map_emptyEntries_returnsFailedMetadata() {
        assertEquals(
            ConversationVCardAttachmentMetadata.Failed,
            mapper.map(entries = emptyList()),
        )
    }

    @Test
    fun map_singleContact_returnsSummarizedContactMetadata() {
        val entry = mockk<CustomVCardEntry>()
        every { entrySummarizer.isLocation(entry) } returns false
        every { entrySummarizer.avatarPhoto(entry) } returns null
        every { entrySummarizer.displayName(entry) } returns "Sam Rivera"
        every { entrySummarizer.normalizedDestination(entry) } returns "sam@example.com"
        every { entrySummarizer.firstPostalAddress(entry) } returns null

        assertEquals(
            ConversationVCardAttachmentMetadata.Loaded(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarPhoto = null,
                entryCount = 1,
                singleDisplayName = "Sam Rivera",
                normalizedDestination = "sam@example.com",
                locationAddress = null,
            ),
            mapper.map(entries = listOf(entry)),
        )
    }

    @Test
    fun map_singleLocation_returnsLocationMetadata() {
        val entry = mockk<CustomVCardEntry>()
        every { entrySummarizer.isLocation(entry) } returns true
        every { entrySummarizer.avatarPhoto(entry) } returns null
        every { entrySummarizer.displayName(entry) } returns null
        every { entrySummarizer.normalizedDestination(entry) } returns null
        every { entrySummarizer.firstPostalAddress(entry) } returns
            "25 11th Ave New York NY 10011 United States"

        assertEquals(
            ConversationVCardAttachmentMetadata.Loaded(
                type = ConversationVCardAttachmentType.LOCATION,
                avatarPhoto = null,
                entryCount = 1,
                singleDisplayName = null,
                normalizedDestination = null,
                locationAddress = "25 11th Ave New York NY 10011 United States",
            ),
            mapper.map(entries = listOf(entry)),
        )
    }

    @Test
    fun map_multipleEntries_returnsContactMetadataWithoutSingleEntrySummary() {
        val entries = listOf(mockk<CustomVCardEntry>(), mockk<CustomVCardEntry>())

        assertEquals(
            ConversationVCardAttachmentMetadata.Loaded(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarPhoto = null,
                entryCount = 2,
                singleDisplayName = null,
                normalizedDestination = null,
                locationAddress = null,
            ),
            mapper.map(entries = entries),
        )
    }
}
