package com.android.messaging.domain.shareintent.usecase

import android.content.Intent
import android.net.Uri
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.shareintent.model.SharedTextContentResult
import com.android.messaging.data.shareintent.repository.SharedAttachmentRepository
import com.android.messaging.util.ContentType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class BuildSharedConversationDraftImplTest {

    private val resolveSharedContentType = mockk<ResolveSharedContentType>()
    private val sharedAttachmentRepository = mockk<SharedAttachmentRepository>()
    private val testDispatcher = StandardTestDispatcher()

    private val buildSharedConversationDraft = BuildSharedConversationDraftImpl(
        resolveSharedContentType = resolveSharedContentType,
        sharedAttachmentRepository = sharedAttachmentRepository,
        ioDispatcher = testDispatcher,
    )

    @Test
    fun invoke_subjectPresent_usesSubject() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        val intent = textIntent().apply {
            putExtra(Intent.EXTRA_SUBJECT, "Subject")
            putExtra(Intent.EXTRA_TITLE, "Title")
        }

        val result = buildSharedConversationDraft(intent)

        assertEquals("Subject", result.draft?.subjectText)
    }

    @Test
    fun invoke_subjectMissing_fallsBackToTitle() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        val intent = textIntent().apply {
            putExtra(Intent.EXTRA_TITLE, "Title")
        }

        val result = buildSharedConversationDraft(intent)

        assertEquals("Title", result.draft?.subjectText)
    }

    @Test
    fun invoke_subjectAndTitleMissing_usesEmptySubject() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        val intent = textIntent()

        val result = buildSharedConversationDraft(intent)

        assertEquals("", result.draft?.subjectText)
    }

    @Test
    fun invoke_imageContentUri_persistsAttachment() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.IMAGE_JPEG
        coEvery { sharedAttachmentRepository.persistToScratchSpace(IMAGE_URI, any()) } returns
            ATTACHMENT

        val result = buildSharedConversationDraft(imageIntent())

        assertEquals(listOf(ATTACHMENT), result.draft?.attachments?.toList())
        assertFalse(result.hasDroppedContent)
    }

    @Test
    fun invoke_fileUriAttachment_isDroppedAndMarkedDroppedContent() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.IMAGE_JPEG

        val result = buildSharedConversationDraft(imageIntent(uri = FILE_URI))

        assertTrue(result.draft?.attachments.orEmpty().isEmpty())
        assertEquals("shared text", result.draft?.messageText)
        assertTrue(result.hasDroppedContent)
        verify(exactly = 0) { resolveSharedContentType(FILE_URI, any()) }
        coVerify(exactly = 0) { sharedAttachmentRepository.persistToScratchSpace(any(), any()) }
    }

    @Test
    fun invoke_textWithoutExtraText_readsFromContentUri() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        coEvery { sharedAttachmentRepository.readTextContent(TEXT_URI) } returns
            SharedTextContentResult.Read("file text")

        val result = buildSharedConversationDraft(textContentIntent())

        assertEquals("file text", result.draft?.messageText)
        assertFalse(result.hasDroppedContent)
    }

    @Test
    fun invoke_textReadFails_marksDroppedContent() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        coEvery { sharedAttachmentRepository.readTextContent(TEXT_URI) } returns
            SharedTextContentResult.Failed

        val intent = textContentIntent().apply {
            putExtra(Intent.EXTRA_SUBJECT, "Subject")
        }
        val result = buildSharedConversationDraft(intent)

        assertEquals("Subject", result.draft?.subjectText)
        assertTrue(result.hasDroppedContent)
    }

    @Test
    fun invoke_blankTextContent_doesNotMarkDroppedContent() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        coEvery { sharedAttachmentRepository.readTextContent(TEXT_URI) } returns
            SharedTextContentResult.Empty

        val intent = textContentIntent().apply {
            putExtra(Intent.EXTRA_SUBJECT, "Subject")
        }
        val result = buildSharedConversationDraft(intent)

        assertEquals("Subject", result.draft?.subjectText)
        assertFalse(result.hasDroppedContent)
    }

    @Test
    fun invoke_fileUriText_isDroppedAndMarkedDroppedContent() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN

        val result = buildSharedConversationDraft(textContentIntent(uri = FILE_URI))

        assertNull(result.draft)
        assertTrue(result.hasDroppedContent)
        verify(exactly = 0) { resolveSharedContentType(FILE_URI, any()) }
        coVerify(exactly = 0) { sharedAttachmentRepository.readTextContent(any()) }
    }

    @Test
    fun invoke_textWithExtraText_skipsContentUri() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        val intent = textContentIntent().apply { putExtra(Intent.EXTRA_TEXT, "inline") }

        val result = buildSharedConversationDraft(intent)

        assertEquals("inline", result.draft?.messageText)
        coVerify(exactly = 0) { sharedAttachmentRepository.readTextContent(any()) }
    }

    @Test
    fun invoke_sendMultipleWithNonImageTopLevelType_persistsMediaUris() = runTest(testDispatcher) {
        every { resolveSharedContentType(IMAGE_URI, any()) } returns ContentType.IMAGE_JPEG
        every { resolveSharedContentType(VIDEO_URI, any()) } returns "video/mp4"
        coEvery { sharedAttachmentRepository.persistToScratchSpace(IMAGE_URI, any()) } returns
            ATTACHMENT
        coEvery { sharedAttachmentRepository.persistToScratchSpace(VIDEO_URI, any()) } returns
            VIDEO_ATTACHMENT

        val result = buildSharedConversationDraft(
            multipleIntent(
                type = "*/*",
                uris = arrayListOf(IMAGE_URI, VIDEO_URI),
            ),
        )

        assertEquals(listOf(ATTACHMENT, VIDEO_ATTACHMENT), result.draft?.attachments?.toList())
    }

    @Test
    fun invoke_sendMultiple_readsTextUrisAndPersistsMediaUris() = runTest(testDispatcher) {
        every { resolveSharedContentType(IMAGE_URI, any()) } returns ContentType.IMAGE_JPEG
        every { resolveSharedContentType(TEXT_URI, any()) } returns ContentType.TEXT_PLAIN
        coEvery { sharedAttachmentRepository.persistToScratchSpace(IMAGE_URI, any()) } returns
            ATTACHMENT
        coEvery { sharedAttachmentRepository.readTextContent(TEXT_URI) } returns
            SharedTextContentResult.Read("file text")

        val result = buildSharedConversationDraft(
            multipleIntent(
                type = "*/*",
                uris = arrayListOf(IMAGE_URI, TEXT_URI),
            ),
        )

        assertEquals(listOf(ATTACHMENT), result.draft?.attachments?.toList())
        assertEquals("file text", result.draft?.messageText)
        coVerify(exactly = 0) { sharedAttachmentRepository.persistToScratchSpace(TEXT_URI, any()) }
    }

    @Test
    fun invoke_sendMultiple_combinesExtraTextAndTextUris() = runTest(testDispatcher) {
        every { resolveSharedContentType(TEXT_URI, any()) } returns ContentType.TEXT_PLAIN
        coEvery { sharedAttachmentRepository.readTextContent(TEXT_URI) } returns
            SharedTextContentResult.Read("file text")

        val intent = multipleIntent(
            type = ContentType.TEXT_PLAIN,
            uris = arrayListOf(TEXT_URI),
        ).apply { putExtra(Intent.EXTRA_TEXT, "inline") }

        val result = buildSharedConversationDraft(intent)

        assertEquals("inline\nfile text", result.draft?.messageText)
    }

    @Test
    fun invoke_sendMultipleFileUris_areDroppedAndMarkedDroppedContent() = runTest(testDispatcher) {
        every { resolveSharedContentType(IMAGE_URI, any()) } returns ContentType.IMAGE_JPEG
        coEvery { sharedAttachmentRepository.persistToScratchSpace(IMAGE_URI, any()) } returns
            ATTACHMENT

        val result = buildSharedConversationDraft(
            multipleIntent(
                type = "*/*",
                uris = arrayListOf(IMAGE_URI, FILE_URI),
            ),
        )

        assertEquals(listOf(ATTACHMENT), result.draft?.attachments?.toList())
        assertTrue(result.hasDroppedContent)
        verify(exactly = 0) { resolveSharedContentType(FILE_URI, any()) }
    }

    @Test
    fun invoke_mediaPersistFails_marksDroppedContent() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.IMAGE_JPEG
        coEvery { sharedAttachmentRepository.persistToScratchSpace(IMAGE_URI, any()) } returns null

        val result = buildSharedConversationDraft(imageIntent())

        assertTrue(result.draft?.attachments.orEmpty().isEmpty())
        assertEquals("shared text", result.draft?.messageText)
        assertTrue(result.hasDroppedContent)
    }

    @Test
    fun invoke_unsupportedContentType_marksDroppedContent() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns "application/pdf"

        val result = buildSharedConversationDraft(imageIntent())

        assertTrue(result.draft?.attachments.orEmpty().isEmpty())
        assertEquals("shared text", result.draft?.messageText)
        assertTrue(result.hasDroppedContent)
    }

    @Test
    fun invoke_sendMultipleUnsupportedType_marksDroppedContent() = runTest(testDispatcher) {
        every { resolveSharedContentType(IMAGE_URI, any()) } returns ContentType.IMAGE_JPEG
        every { resolveSharedContentType(TEXT_URI, any()) } returns "application/pdf"
        coEvery { sharedAttachmentRepository.persistToScratchSpace(IMAGE_URI, any()) } returns
            ATTACHMENT

        val result = buildSharedConversationDraft(
            multipleIntent(
                type = "*/*",
                uris = arrayListOf(IMAGE_URI, TEXT_URI),
            ),
        )

        assertEquals(listOf(ATTACHMENT), result.draft?.attachments?.toList())
        assertTrue(result.hasDroppedContent)
    }

    @Test
    fun invoke_sendMultipleTextReadFails_marksDroppedContent() = runTest(testDispatcher) {
        every { resolveSharedContentType(IMAGE_URI, any()) } returns ContentType.IMAGE_JPEG
        every { resolveSharedContentType(TEXT_URI, any()) } returns ContentType.TEXT_PLAIN
        coEvery { sharedAttachmentRepository.persistToScratchSpace(IMAGE_URI, any()) } returns
            ATTACHMENT
        coEvery { sharedAttachmentRepository.readTextContent(TEXT_URI) } returns
            SharedTextContentResult.Failed

        val result = buildSharedConversationDraft(
            multipleIntent(
                type = "*/*",
                uris = arrayListOf(IMAGE_URI, TEXT_URI),
            ),
        )

        assertEquals(listOf(ATTACHMENT), result.draft?.attachments?.toList())
        assertTrue(result.hasDroppedContent)
    }

    @Test
    fun invoke_sendMultipleBlankText_doesNotMarkDroppedContent() = runTest(testDispatcher) {
        every { resolveSharedContentType(TEXT_URI, any()) } returns ContentType.TEXT_PLAIN
        coEvery { sharedAttachmentRepository.readTextContent(TEXT_URI) } returns
            SharedTextContentResult.Empty

        val intent = multipleIntent(
            type = ContentType.TEXT_PLAIN,
            uris = arrayListOf(TEXT_URI),
        ).apply {
            putExtra(Intent.EXTRA_TEXT, "inline")
        }
        val result = buildSharedConversationDraft(intent)

        assertEquals("inline", result.draft?.messageText)
        assertFalse(result.hasDroppedContent)
    }

    private fun multipleIntent(
        type: String,
        uris: ArrayList<Uri>,
    ): Intent {
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            this.type = type
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }

    private fun textIntent(): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = ContentType.TEXT_PLAIN
            putExtra(Intent.EXTRA_TEXT, "shared text")
        }
    }

    private fun textContentIntent(uri: Uri = TEXT_URI): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = ContentType.TEXT_PLAIN
            putExtra(Intent.EXTRA_STREAM, uri)
        }
    }

    private fun imageIntent(uri: Uri = IMAGE_URI): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = ContentType.IMAGE_JPEG
            putExtra(Intent.EXTRA_TEXT, "shared text")
            putExtra(Intent.EXTRA_STREAM, uri)
        }
    }

    private companion object {
        private val IMAGE_URI: Uri = Uri.parse("content://media/external/images/1")
        private val VIDEO_URI: Uri = Uri.parse("content://media/external/video/1")
        private val TEXT_URI: Uri = Uri.parse("content://media/external/text/1")
        private val FILE_URI: Uri =
            Uri.parse("file:///data/data/com.android.messaging/databases/secret")
        private val ATTACHMENT = ConversationDraftAttachment(
            contentType = ContentType.IMAGE_JPEG,
            contentUri = "content://scratch/1",
        )
        private val VIDEO_ATTACHMENT = ConversationDraftAttachment(
            contentType = "video/mp4",
            contentUri = "content://scratch/2",
        )
    }
}
