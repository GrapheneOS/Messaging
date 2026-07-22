package com.android.messaging.ui.conversation.messages.mapper.conversationmessage

import android.net.Uri
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageUiModelMapperPartsTest :
    BaseConversationMessageUiModelMapperTest() {

    @Test
    fun map_textPart_mapsToTextPartWithPartText() {
        val uiModel = mapper.map(
            messageData(
                parts = listOf(messagePart(contentType = "text/plain", text = "Hi Ada")),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(ConversationMessagePartUiModel.Text(text = "Hi Ada")),
            uiModel.parts,
        )
    }

    @Test
    fun map_textPartWithNullText_mapsToTextPartWithEmptyText() {
        val uiModel = mapper.map(
            messageData(
                parts = listOf(messagePart(contentType = "text/html", text = null)),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(ConversationMessagePartUiModel.Text(text = "")),
            uiModel.parts,
        )
    }

    @Test
    fun map_audioPart_mapsToAudioAttachmentPreservingMediaFields() {
        val contentUri = Uri.parse("content://audio/1")

        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(
                        contentType = "audio/mpeg",
                        text = "voice note",
                        contentUri = contentUri,
                        width = 3,
                        height = 4,
                    ),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(
                ConversationMessagePartUiModel.Attachment.Audio(
                    text = "voice note",
                    contentType = "audio/mpeg",
                    contentUri = contentUri,
                    width = 3,
                    height = 4,
                ),
            ),
            uiModel.parts,
        )
    }

    @Test
    fun map_imagePart_mapsToImageAttachment() {
        val contentUri = Uri.parse("content://image/1")

        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(
                        contentType = "image/jpeg",
                        text = null,
                        contentUri = contentUri,
                        width = 640,
                        height = 480,
                    ),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(
                ConversationMessagePartUiModel.Attachment.Image(
                    text = null,
                    contentType = "image/jpeg",
                    contentUri = contentUri,
                    width = 640,
                    height = 480,
                ),
            ),
            uiModel.parts,
        )
    }

    @Test
    fun map_videoPart_mapsToVideoAttachment() {
        val contentUri = Uri.parse("content://video/1")

        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(
                        contentType = "video/mp4",
                        contentUri = contentUri,
                        width = 1920,
                        height = 1080,
                    ),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(
                ConversationMessagePartUiModel.Attachment.Video(
                    text = null,
                    contentType = "video/mp4",
                    contentUri = contentUri,
                    width = 1920,
                    height = 1080,
                ),
            ),
            uiModel.parts,
        )
    }

    @Test
    fun map_vCardPart_mapsToVCardAttachmentUsingVCardMapper() {
        val contentUri = Uri.parse("content://vcard/1")

        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(contentType = "text/x-vCard", contentUri = contentUri),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(
                ConversationMessagePartUiModel.Attachment.VCard(
                    text = null,
                    contentType = "text/x-vCard",
                    contentUri = contentUri,
                    width = 0,
                    height = 0,
                    vCardUiModel = vCardUiModel,
                ),
            ),
            uiModel.parts,
        )
        verify(exactly = 1) {
            conversationVCardAttachmentUiModelMapper.map(metadata = null)
        }
    }

    @Test
    fun map_unknownContentType_mapsToFileAttachment() {
        val contentUri = Uri.parse("content://file/1")

        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(contentType = "application/pdf", contentUri = contentUri),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(
                ConversationMessagePartUiModel.Attachment.File(
                    text = null,
                    contentType = "application/pdf",
                    contentUri = contentUri,
                    width = 0,
                    height = 0,
                ),
            ),
            uiModel.parts,
        )
    }

    @Test
    fun map_nullContentType_mapsToFileAttachmentWithEmptyContentType() {
        val uiModel = mapper.map(
            messageData(parts = listOf(messagePart(contentType = null))),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(
                ConversationMessagePartUiModel.Attachment.File(
                    text = null,
                    contentType = "",
                    contentUri = null,
                    width = 0,
                    height = 0,
                ),
            ),
            uiModel.parts,
        )
    }

    @Test
    fun map_multipleParts_preservesOrderAndTypes() {
        val imageUri = Uri.parse("content://image/2")

        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(contentType = "text/plain", text = "caption"),
                    messagePart(contentType = "image/png", contentUri = imageUri),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(
                ConversationMessagePartUiModel.Text(text = "caption"),
                ConversationMessagePartUiModel.Attachment.Image(
                    text = null,
                    contentType = "image/png",
                    contentUri = imageUri,
                    width = 0,
                    height = 0,
                ),
            ),
            uiModel.parts,
        )
    }

    @Test
    fun map_withMediaPartHavingContentUri_marksAttachmentsSaveable() {
        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(
                        contentType = "image/jpeg",
                        contentUri = Uri.parse("content://image/saveable"),
                    ),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertTrue(uiModel.canSaveAttachments)
    }

    @Test
    fun map_withTextPartsOnly_marksAttachmentsNotSaveable() {
        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(
                        contentType = "text/plain",
                        text = "Hi",
                        contentUri = Uri.parse("content://text/1"),
                    ),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertFalse(uiModel.canSaveAttachments)
    }

    @Test
    fun map_withMediaPartMissingContentUri_marksAttachmentsNotSaveable() {
        val uiModel = mapper.map(
            messageData(
                parts = listOf(messagePart(contentType = "image/jpeg", contentUri = null)),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertFalse(uiModel.canSaveAttachments)
    }

    @Test
    fun map_withBlankContentTypePart_marksAttachmentsNotSaveable() {
        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(
                        contentType = "   ",
                        contentUri = Uri.parse("content://blank/1"),
                    ),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertFalse(uiModel.canSaveAttachments)
    }

    @Test
    fun map_withTextCaptionBeforeSaveableMediaParts_marksAttachmentsSaveable() {
        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(contentType = "text/plain", text = "caption"),
                    messagePart(
                        contentType = "image/jpeg",
                        contentUri = Uri.parse("content://image/mixed"),
                    ),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertTrue(uiModel.canSaveAttachments)
    }

    @Test
    fun map_withSaveableMediaBeforeTextCaptionParts_marksAttachmentsSaveable() {
        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(
                        contentType = "image/jpeg",
                        contentUri = Uri.parse("content://image/mixed"),
                    ),
                    messagePart(contentType = "text/plain", text = "caption"),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertTrue(uiModel.canSaveAttachments)
    }

    @Test
    fun map_withVCardPartHavingContentUri_marksAttachmentsSaveable() {
        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(
                        contentType = "text/x-vCard",
                        contentUri = Uri.parse("content://vcard/saveable"),
                    ),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertTrue(uiModel.canSaveAttachments)
    }

    @Test
    fun map_oggAudioPart_mapsToAudioAttachment() {
        val contentUri = Uri.parse("content://audio/ogg")

        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(contentType = "application/ogg", contentUri = contentUri),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(
                ConversationMessagePartUiModel.Attachment.Audio(
                    text = null,
                    contentType = "application/ogg",
                    contentUri = contentUri,
                    width = 0,
                    height = 0,
                ),
            ),
            uiModel.parts,
        )
    }

    @Test
    fun map_vCardPartWithLowercaseContentType_mapsToVCardAttachment() {
        val contentUri = Uri.parse("content://vcard/lowercase")

        val uiModel = mapper.map(
            messageData(
                parts = listOf(
                    messagePart(contentType = "text/x-vcard", contentUri = contentUri),
                ),
            ),
            isYouTubePreviewEnabled = false,
        )

        assertEquals(
            persistentListOf(
                ConversationMessagePartUiModel.Attachment.VCard(
                    text = null,
                    contentType = "text/x-vcard",
                    contentUri = contentUri,
                    width = 0,
                    height = 0,
                    vCardUiModel = vCardUiModel,
                ),
            ),
            uiModel.parts,
        )
    }
}
