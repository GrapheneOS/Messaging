package com.android.messaging.ui.shareintent.screen.model

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.ui.common.components.attachment.VCardAttachmentKind
import com.android.messaging.util.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ShareAttachmentUiModelTest {

    @Test
    fun toShareAttachmentUiModel_mapsVideoToMediaMarkedAsVideo() {
        val model = attachment(contentType = "video/mp4").toShareAttachmentUiModel()

        val media = model as ShareAttachmentUiModel.Media
        assertEquals("content://uri", media.id)
        assertEquals("video/mp4", media.contentType)
        assertTrue(media.isVideo)
    }

    @Test
    fun toShareAttachmentUiModel_mapsImageToMediaNotMarkedAsVideo() {
        val model = attachment(contentType = "image/jpeg").toShareAttachmentUiModel()

        val media = model as ShareAttachmentUiModel.Media
        assertEquals("image/jpeg", media.contentType)
        assertFalse(media.isVideo)
    }

    @Test
    fun toShareAttachmentUiModel_mapsAudioWithDuration() {
        val model = attachment(
            contentType = "audio/mpeg",
            durationMillis = 4200L,
        ).toShareAttachmentUiModel()

        val audio = model as ShareAttachmentUiModel.Audio
        assertEquals("content://uri", audio.id)
        assertEquals(4200L, audio.durationMillis)
    }

    @Test
    fun toShareAttachmentUiModel_mapsAudioWithoutDurationToZero() {
        val model = attachment(
            contentType = "audio/mpeg",
            durationMillis = null,
        ).toShareAttachmentUiModel()

        val audio = model as ShareAttachmentUiModel.Audio
        assertEquals(0L, audio.durationMillis)
    }

    @Test
    fun toShareAttachmentUiModel_mapsVCardStrippingFileExtensionFromTitle() {
        val model = attachment(
            contentType = ContentType.TEXT_VCARD,
            displayName = "Jane.vcf",
        ).toShareAttachmentUiModel()

        val vcard = model as ShareAttachmentUiModel.VCard
        assertEquals("Jane", vcard.title)
        assertEquals(VCardAttachmentKind.Contact, vcard.kind)
    }

    @Test
    fun toShareAttachmentUiModel_mapsVCardWithNullDisplayNameToNullTitle() {
        val model = attachment(
            contentType = ContentType.TEXT_VCARD,
            displayName = null,
        ).toShareAttachmentUiModel()

        assertEquals(null, (model as ShareAttachmentUiModel.VCard).title)
    }

    private fun attachment(
        contentType: String,
        contentUri: String = "content://uri",
        durationMillis: Long? = null,
        displayName: String? = null,
    ): ConversationDraftAttachment {
        return ConversationDraftAttachment(
            contentType = contentType,
            contentUri = contentUri,
            durationMillis = durationMillis,
            displayName = displayName,
        )
    }
}
