package com.android.messaging.domain.shareintent.usecase

import android.content.Intent
import com.android.messaging.data.shareintent.repository.SharedAttachmentRepository
import com.android.messaging.util.ContentType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

        val draft = buildSharedConversationDraft(intent)

        assertEquals("Subject", draft?.subjectText)
    }

    @Test
    fun invoke_subjectMissing_fallsBackToTitle() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        val intent = textIntent().apply {
            putExtra(Intent.EXTRA_TITLE, "Title")
        }

        val draft = buildSharedConversationDraft(intent)

        assertEquals("Title", draft?.subjectText)
    }

    @Test
    fun invoke_subjectAndTitleMissing_usesEmptySubject() = runTest(testDispatcher) {
        every { resolveSharedContentType(any(), any()) } returns ContentType.TEXT_PLAIN
        val intent = textIntent()

        val draft = buildSharedConversationDraft(intent)

        assertEquals("", draft?.subjectText)
    }

    private fun textIntent(): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = ContentType.TEXT_PLAIN
            putExtra(Intent.EXTRA_TEXT, "shared text")
        }
    }
}
