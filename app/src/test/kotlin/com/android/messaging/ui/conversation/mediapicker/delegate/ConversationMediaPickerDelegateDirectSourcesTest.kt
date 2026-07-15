package com.android.messaging.ui.conversation.mediapicker.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.testutil.TEST_CONTACT_URI
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerDelegateDirectSourcesTest :
    BaseConversationMediaPickerDelegateTest() {

    @Test
    fun onCapturedMediaReady_addsSingleDraftAttachment() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val draftDelegate = createMediaPickerDraftDelegateMock()
            val attachmentMapper = createDraftAttachmentMapperMock()
            val delegate = createDelegate(
                draftDelegate = draftDelegate,
                attachmentMapper = attachmentMapper,
                attachmentRepository = createAttachmentRepositoryMock(),
                defaultDispatcher = mainDispatcherRule.testDispatcher,
            )
            val capturedMedia = ConversationCapturedMedia(
                contentUri = "content://scratch/1",
                contentType = "image/jpeg",
                width = 800,
                height = 600,
            )
            val attachment = ConversationDraftAttachment(
                contentType = "image/jpeg",
                contentUri = "content://scratch/1",
                width = 800,
                height = 600,
            )

            every {
                attachmentMapper.map(capturedMedia = capturedMedia)
            } returns attachment

            delegate.onCapturedMediaReady(capturedMedia = capturedMedia)

            verify(exactly = 1) {
                attachmentMapper.map(capturedMedia = capturedMedia)
            }
            verify(exactly = 1) {
                draftDelegate.addAttachments(attachments = listOf(attachment))
            }
        }
    }

    @Test
    fun onContactCardPicked_addsResolvedContactAttachment() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val draftDelegate = createMediaPickerDraftDelegateMock()
            val attachment = ConversationDraftAttachment(
                contentType = "text/x-vCard",
                contentUri = "content://contacts/as_vcard/1",
            )
            val attachmentRepository = createAttachmentRepositoryMock(
                createDraftAttachmentFromContactFlow = flowOf(attachment),
            )
            val delegate = createDelegate(
                draftDelegate = draftDelegate,
                attachmentMapper = createDraftAttachmentMapperMock(),
                attachmentRepository = attachmentRepository,
                defaultDispatcher = dispatcher,
            )
            val boundScope = CoroutineScope(dispatcher + SupervisorJob())

            try {
                delegate.bind(
                    scope = boundScope,
                    conversationIdFlow = MutableStateFlow(value = null),
                )

                delegate.onContactCardPicked(contactUri = TEST_CONTACT_URI)
                advanceUntilIdle()

                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    attachmentRepository.createDraftAttachmentFromContact(
                        contactUri = TEST_CONTACT_URI,
                    )
                }
                verify(exactly = 1) {
                    draftDelegate.addAttachments(attachments = listOf(attachment))
                }
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun onContactCardPicked_ignoresBlankUris() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val attachmentRepository = createAttachmentRepositoryMock()
            val delegate = createDelegate(
                draftDelegate = createMediaPickerDraftDelegateMock(),
                attachmentMapper = createDraftAttachmentMapperMock(),
                attachmentRepository = attachmentRepository,
                defaultDispatcher = dispatcher,
            )
            val boundScope = CoroutineScope(dispatcher + SupervisorJob())

            try {
                delegate.bind(
                    scope = boundScope,
                    conversationIdFlow = MutableStateFlow(value = null),
                )

                delegate.onContactCardPicked(contactUri = "   ")
                advanceUntilIdle()

                verify(exactly = 0) {
                    @Suppress("UnusedFlow")
                    attachmentRepository.createDraftAttachmentFromContact(any())
                }
            } finally {
                boundScope.cancel()
            }
        }
    }
}
