package com.android.messaging.data.conversation.mapper

import android.net.Uri
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.datamodel.data.VCardContactItemData
import com.android.messaging.datamodel.media.VCardResource
import com.android.messaging.datamodel.media.VCardResourceEntry
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationVCardMetadataMapperImplTest {

    private val mapper = ConversationVCardMetadataMapperImpl()

    @Test
    fun map_contactEntry_returnsContactMetadata() {
        val vCardContactItemData = mockk<VCardContactItemData> {
            every { getDisplayName() } returns "Sam Rivera"
            every { avatarUri } returns Uri.parse("content://avatar/sam")
            every { details } returns "sam@example.com"
            every { vCardResource } returns vCardResource(
                entry = vCardResourceEntry(
                    kind = null,
                    displayAddress = null,
                ),
            )
        }

        val metadata = mapper.map(
            vCardContactItemData = vCardContactItemData,
        )

        assertEquals(
            ConversationVCardAttachmentMetadata.Loaded(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = "content://avatar/sam",
                displayName = "Sam Rivera",
                details = "sam@example.com",
                locationAddress = null,
            ),
            metadata,
        )
    }

    @Test
    fun map_locationEntry_returnsLocationMetadata() {
        val vCardContactItemData = mockk<VCardContactItemData> {
            every { getDisplayName() } returns null
            every { avatarUri } returns null
            every { details } returns "New York"
            every { vCardResource } returns vCardResource(
                entry = vCardResourceEntry(
                    kind = "LoCaTiOn",
                    displayAddress = "25 11th Ave New York NY 10011 United States",
                ),
            )
        }

        val metadata = mapper.map(
            vCardContactItemData = vCardContactItemData,
        )

        assertEquals(
            ConversationVCardAttachmentMetadata.Loaded(
                type = ConversationVCardAttachmentType.LOCATION,
                avatarUri = null,
                displayName = null,
                details = "New York",
                locationAddress = "25 11th Ave New York NY 10011 United States",
            ),
            metadata,
        )
    }

    @Test
    fun map_blankStrings_returnsNullFields() {
        val vCardContactItemData = mockk<VCardContactItemData> {
            every { getDisplayName() } returns "   "
            every { avatarUri } returns Uri.parse(" ")
            every { details } returns ""
            every { vCardResource } returns vCardResource(
                entry = vCardResourceEntry(
                    kind = null,
                    displayAddress = " ",
                ),
            )
        }

        val metadata = mapper.map(
            vCardContactItemData = vCardContactItemData,
        ) as ConversationVCardAttachmentMetadata.Loaded

        assertEquals(ConversationVCardAttachmentType.CONTACT, metadata.type)
        assertNull(metadata.avatarUri)
        assertNull(metadata.displayName)
        assertNull(metadata.details)
        assertNull(metadata.locationAddress)
    }

    private fun vCardResource(
        entry: VCardResourceEntry,
    ): VCardResource {
        return mockk<VCardResource> {
            every { vCards } returns listOf(entry)
        }
    }

    private fun vCardResourceEntry(
        kind: String?,
        displayAddress: String?,
    ): VCardResourceEntry {
        return mockk<VCardResourceEntry> {
            every { getKind() } returns kind
            every { getDisplayAddress() } returns displayAddress
        }
    }
}
