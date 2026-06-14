package com.android.messaging.domain.shareintent.usecase

import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.shareintent.repository.SharedAttachmentRepository
import com.android.messaging.util.ContentType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private val grantingCaller = mockk<ComponentCaller> {
        every { checkContentUriPermission(any(), any()) } returns PackageManager.PERMISSION_GRANTED
    }
    private val denyingCaller = mockk<ComponentCaller> {
        every { checkContentUriPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED
    }

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

        val draft = buildSharedConversationDraft(intent, grantingCaller)

        assertEquals("Subject", draft?.subjectText)
    }

    @Test
    fun invoke_subjectMissing_fallsBackToTitle() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        val intent = textIntent().apply {
            putExtra(Intent.EXTRA_TITLE, "Title")
        }

        val draft = buildSharedConversationDraft(intent, grantingCaller)

        assertEquals("Title", draft?.subjectText)
    }

    @Test
    fun invoke_subjectAndTitleMissing_usesEmptySubject() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        val intent = textIntent()

        val draft = buildSharedConversationDraft(intent, grantingCaller)

        assertEquals("", draft?.subjectText)
    }

    @Test
    fun invoke_callerHasReadPermission_persistsAttachment() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.IMAGE_JPEG
        coEvery { sharedAttachmentRepository.persistToScratchSpace(IMAGE_URI, any()) } returns
            ATTACHMENT

        val draft = buildSharedConversationDraft(imageIntent(), grantingCaller)

        assertEquals(listOf(ATTACHMENT), draft?.attachments?.toList())
    }

    @Test
    fun invoke_callerLacksReadPermission_dropsAttachment() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.IMAGE_JPEG

        val draft = buildSharedConversationDraft(imageIntent(), denyingCaller)

        assertTrue(draft?.attachments.orEmpty().isEmpty())
        assertEquals("shared text", draft?.messageText)
        coVerify(exactly = 0) { sharedAttachmentRepository.persistToScratchSpace(any(), any()) }
    }

    private fun textIntent(): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = ContentType.TEXT_PLAIN
            putExtra(Intent.EXTRA_TEXT, "shared text")
        }
    }

    private fun imageIntent(): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = ContentType.IMAGE_JPEG
            putExtra(Intent.EXTRA_TEXT, "shared text")
            putExtra(Intent.EXTRA_STREAM, IMAGE_URI)
        }
    }

    private companion object {
        private val IMAGE_URI: Uri = Uri.parse("content://media/external/images/1")
        private val ATTACHMENT = ConversationDraftAttachment(
            contentType = ContentType.IMAGE_JPEG,
            contentUri = "content://scratch/1",
        )
    }
}
