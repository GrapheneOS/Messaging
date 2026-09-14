package com.android.messaging.ui.conversation.messages.ui.attachment

import android.net.Uri
import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentOpenAction
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationAttachmentSectionsBuilderTest {

    @Test
    fun mixedAttachments_splitsGalleryVisualsAndTrailingItems() {
        val imageAttachment = ConversationMessageAttachment.Media(
            key = "image",
            part = imagePart(),
        )
        val videoAttachment = ConversationMessageAttachment.Media(
            key = "video",
            part = videoPart(),
        )
        val youTubeAttachment = ConversationMessageAttachment.YouTubePreview(
            key = "youtube",
            sourceUrl = "https://www.youtube.com/watch?v=abc",
            thumbnailUrl = "https://img.youtube.com/vi/abc/0.jpg",
        )
        val unsupportedAttachment = ConversationMessageAttachment.Unsupported(
            key = "unsupported",
            part = filePart(
                contentUri = Uri.parse("content://mms/part/file-1"),
                contentType = "application/pdf",
            ),
        )

        val sections = buildConversationAttachmentSections(
            attachments = listOf(
                imageAttachment,
                videoAttachment,
                youTubeAttachment,
                unsupportedAttachment,
            ).toImmutableList(),
        )

        assertEquals(
            listOf(imageAttachment, youTubeAttachment),
            sections.galleryVisualAttachments,
        )
        assertEquals(2, sections.trailingItems.size)
        assertEquals(
            videoAttachment,
            (sections.trailingItems[0] as ConversationAttachmentItem.StandaloneVisual)
                .attachment,
        )

        val fileAttachment = (sections.trailingItems[1] as ConversationAttachmentItem.Inline)
            .attachment as ConversationInlineAttachment.File
        assertEquals("unsupported", fileAttachment.key)
        assertEquals("application/pdf", fileAttachment.titleText)
    }

    @Test
    fun audioAttachment_mapsToInlineAudioAttachment() {
        val sections = buildConversationAttachmentSections(
            attachments = listOf(
                ConversationMessageAttachment.Media(
                    key = "attachment-1",
                    part = ConversationMessagePartUiModel.Attachment.Audio(
                        text = null,
                        contentType = "audio/x-wav",
                        contentUri = Uri.parse("content://mms/part/audio-1"),
                        width = 0,
                        height = 0,
                    ),
                ),
            ).toImmutableList(),
        )

        val inlineAttachment =
            (sections.trailingItems.single() as ConversationAttachmentItem.Inline)
                .attachment as ConversationInlineAttachment.Audio

        assertEquals("content://mms/part/audio-1", inlineAttachment.contentUri)
        assertEquals(R.string.audio_attachment_content_description, inlineAttachment.titleTextResId)
        assertNull(inlineAttachment.titleText)
    }

    @Test
    fun vcardAttachment_mapsToInlineVCardAttachment_andPreservesUiModel() {
        val vCardUiModel = ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.LOCATION,
            titleText = "Pier 57",
            subtitleText = "25 11th Ave New York NY 10011 United States",
        )

        val sections = buildConversationAttachmentSections(
            attachments = listOf(
                ConversationMessageAttachment.Media(
                    key = "attachment-1",
                    part = ConversationMessagePartUiModel.Attachment.VCard(
                        text = null,
                        contentType = "text/x-vCard",
                        contentUri = Uri.parse("content://mms/part/vcard-1"),
                        width = 0,
                        height = 0,
                        vCardUiModel = vCardUiModel,
                    ),
                ),
            ).toImmutableList(),
        )

        val inlineAttachment =
            (sections.trailingItems.single() as ConversationAttachmentItem.Inline)
                .attachment as ConversationInlineAttachment.VCard

        assertEquals("content://mms/part/vcard-1", inlineAttachment.contentUri)
        assertEquals(ConversationVCardAttachmentType.LOCATION, inlineAttachment.type)
        assertEquals("Pier 57", inlineAttachment.titleText)
        assertEquals("25 11th Ave New York NY 10011 United States", inlineAttachment.subtitleText)
    }

    @Test
    fun vcardAttachment_usesSubtitleOverrideWhenProvided() {
        val vCardUiModel = ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            titleText = "Sam Rivera",
            subtitleText = "sam@example.com",
        )

        val sections = buildConversationAttachmentSections(
            attachments = listOf(
                ConversationMessageAttachment.Media(
                    key = "attachment-1",
                    part = ConversationMessagePartUiModel.Attachment.VCard(
                        text = null,
                        contentType = "text/x-vCard",
                        contentUri = Uri.parse("content://mms/part/vcard-1"),
                        width = 0,
                        height = 0,
                        vCardUiModel = vCardUiModel,
                    ),
                ),
            ).toImmutableList(),
            vCardSubtitleTextResIdOverride = R.string.copy_to_clipboard,
        )

        val inlineAttachment =
            (sections.trailingItems.single() as ConversationAttachmentItem.Inline)
                .attachment as ConversationInlineAttachment.VCard

        assertNull(inlineAttachment.subtitleText)
        assertEquals(R.string.copy_to_clipboard, inlineAttachment.subtitleTextResId)
    }

    @Test
    fun unsupportedAttachmentWithoutContentUri_hasNoOpenAction() {
        val sections = buildConversationAttachmentSections(
            attachments = listOf(
                ConversationMessageAttachment.Unsupported(
                    key = "unsupported",
                    part = filePart(
                        contentUri = null,
                        contentType = "",
                    ),
                ),
            ).toImmutableList(),
        )

        val inlineAttachment =
            (sections.trailingItems.single() as ConversationAttachmentItem.Inline)
                .attachment as ConversationInlineAttachment.File

        assertNull(inlineAttachment.openAction)
        assertNull(inlineAttachment.titleText)
        assertTrue(sections.galleryVisualAttachments.isEmpty())
    }

    @Test
    fun mediaFileAttachment_mapsToInlineFileAttachmentWithOpenAction() {
        val sections = buildConversationAttachmentSections(
            attachments = listOf(
                ConversationMessageAttachment.Media(
                    key = "file",
                    part = filePart(
                        contentUri = Uri.parse("content://mms/part/file-1"),
                        contentType = "application/pdf",
                    ),
                ),
            ).toImmutableList(),
        )

        val inlineAttachment =
            (sections.trailingItems.single() as ConversationAttachmentItem.Inline)
                .attachment as ConversationInlineAttachment.File

        assertEquals("file", inlineAttachment.key)
        assertEquals("application/pdf", inlineAttachment.titleText)
        assertEquals(
            "content://mms/part/file-1",
            (inlineAttachment.openAction as ConversationAttachmentOpenAction.OpenContent)
                .contentUri,
        )
    }

    private fun imagePart(): ConversationMessagePartUiModel.Attachment.Image {
        return ConversationMessagePartUiModel.Attachment.Image(
            text = null,
            contentType = "image/jpeg",
            contentUri = Uri.parse("content://mms/part/image-1"),
            width = 640,
            height = 480,
        )
    }

    private fun videoPart(): ConversationMessagePartUiModel.Attachment.Video {
        return ConversationMessagePartUiModel.Attachment.Video(
            text = null,
            contentType = "video/mp4",
            contentUri = Uri.parse("content://mms/part/video-1"),
            width = 640,
            height = 480,
        )
    }

    private fun filePart(
        contentUri: Uri?,
        contentType: String,
    ): ConversationMessagePartUiModel.Attachment.File {
        return ConversationMessagePartUiModel.Attachment.File(
            text = null,
            contentType = contentType,
            contentUri = contentUri,
            width = 0,
            height = 0,
        )
    }
}
