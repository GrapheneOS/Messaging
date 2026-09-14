package com.android.messaging.ui.conversation.mediapicker.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.PhotoPickerDraftAttachment
import com.android.messaging.data.media.model.PhotoPickerDraftAttachmentResult
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerDelegateRemovalTest :
    BaseConversationMediaPickerDelegateTest() {

    @Test
    fun onRemoveResolvedAttachment_removesDraftAndDeletesTemporaryAttachment() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val draftDelegate = createMediaPickerDraftDelegateMock()
            val attachment = ConversationDraftAttachment(
                contentType = "image/jpeg",
                contentUri = RESOLVED_CONTENT_URI,
            )
            val attachmentRepository = createAttachmentRepositoryMock(
                createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment = PhotoPickerDraftAttachment(
                            sourceContentUri = REMOTE_CONTENT_URI,
                            draftAttachment = attachment,
                        ),
                    ),
                ),
                deleteTemporaryAttachmentFlow = flowOf(Unit),
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

                delegate.onPhotoPickerMediaSelected(contentUris = listOf(REMOTE_CONTENT_URI))
                advanceUntilIdle()
                assertEquals(
                    REMOTE_CONTENT_URI,
                    delegate.photoPickerSourceContentUriByAttachmentContentUri.value[
                        RESOLVED_CONTENT_URI
                    ],
                )

                delegate.onRemoveResolvedAttachment(contentUri = RESOLVED_CONTENT_URI)
                advanceUntilIdle()

                verify(exactly = 1) {
                    draftDelegate.removeAttachment(contentUri = RESOLVED_CONTENT_URI)
                }
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    attachmentRepository.deleteTemporaryAttachment(
                        contentUri = RESOLVED_CONTENT_URI,
                    )
                }
                assertEquals(
                    emptyMap<String, String>(),
                    delegate.photoPickerSourceContentUriByAttachmentContentUri.value,
                )
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun onRemovePendingAttachment_removesPendingDraftAttachment() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val draftDelegate = createMediaPickerDraftDelegateMock()
            val delegate = createDelegate(
                draftDelegate = draftDelegate,
                attachmentMapper = createDraftAttachmentMapperMock(),
                attachmentRepository = createAttachmentRepositoryMock(),
                defaultDispatcher = mainDispatcherRule.testDispatcher,
            )

            delegate.onRemovePendingAttachment(pendingAttachmentId = PENDING_ATTACHMENT_ID)

            verify(exactly = 1) {
                draftDelegate.removePendingAttachment(pendingAttachmentId = PENDING_ATTACHMENT_ID)
            }
        }
    }

    private companion object {
        private const val PENDING_ATTACHMENT_ID = "pending-1"
        private const val REMOTE_CONTENT_URI = "content://remote/1"
        private const val RESOLVED_CONTENT_URI = "content://scratch/1"
    }
}
