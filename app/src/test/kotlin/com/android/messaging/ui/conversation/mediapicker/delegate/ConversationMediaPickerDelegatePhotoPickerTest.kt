package com.android.messaging.ui.conversation.mediapicker.delegate

import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.PhotoPickerDraftAttachment
import com.android.messaging.data.media.model.PhotoPickerDraftAttachmentResult
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerDelegatePhotoPickerTest :
    BaseConversationMediaPickerDelegateTest() {

    @Test
    fun onPhotoPickerMediaSelected_addsResolvedDraftAttachments() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val draftDelegate = createMediaPickerDraftDelegateMock()
            val attachments = listOf(
                ConversationDraftAttachment(
                    contentType = "image/jpeg",
                    contentUri = "content://picker/1",
                    width = 640,
                    height = 480,
                ),
                ConversationDraftAttachment(
                    contentType = "video/mp4",
                    contentUri = "content://picker/2",
                    width = 1280,
                    height = 720,
                ),
            )
            val attachmentRepository = createAttachmentRepositoryMock(
                createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment =
                            PhotoPickerDraftAttachment(
                                sourceContentUri = "content://picker/source/1",
                                draftAttachment = attachments[0],
                            ),
                    ),
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment =
                            PhotoPickerDraftAttachment(
                                sourceContentUri = "content://picker/source/2",
                                draftAttachment = attachments[1],
                            ),
                    ),
                ),
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

                delegate.onPhotoPickerMediaSelected(
                    contentUris = listOf(
                        "content://picker/source/1",
                        "content://picker/source/2",
                    ),
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    attachmentRepository.createDraftAttachmentsFromPhotoPicker(
                        contentUris = listOf(
                            "content://picker/source/1",
                            "content://picker/source/2",
                        ),
                    )
                }
                verify(exactly = 1) {
                    draftDelegate.addAttachments(attachments = listOf(attachments[0]))
                }
                verify(exactly = 1) {
                    draftDelegate.addAttachments(attachments = listOf(attachments[1]))
                }
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun onPhotoPickerMediaSelected_ignoresEmptyLists() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val attachmentRepository = createAttachmentRepositoryMock()
            val draftDelegate = createMediaPickerDraftDelegateMock()
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

                delegate.onPhotoPickerMediaSelected(contentUris = emptyList())
                advanceUntilIdle()

                verify(exactly = 0) {
                    @Suppress("UnusedFlow")
                    attachmentRepository.createDraftAttachmentsFromPhotoPicker(any())
                }
                verify(exactly = 0) {
                    draftDelegate.addAttachments(any())
                }
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun onPhotoPickerMediaSelected_ignoresAlreadySelectedUris() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val attachmentRepository = createAttachmentRepositoryMock(
                createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment =
                            PhotoPickerDraftAttachment(
                                sourceContentUri = "content://picker/1",
                                draftAttachment = ConversationDraftAttachment(
                                    contentType = "image/jpeg",
                                    contentUri = "content://scratch/1",
                                ),
                            ),
                    ),
                ),
            )
            val draftDelegate = createMediaPickerDraftDelegateMock()
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

                delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
                advanceUntilIdle()
                delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
                advanceUntilIdle()

                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    attachmentRepository.createDraftAttachmentsFromPhotoPicker(
                        contentUris = listOf("content://picker/1"),
                    )
                }
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun onPhotoPickerMediaDeselected_removesDraftAttachmentsAndDeletesTemporaryAttachment() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val draftDelegate = createMediaPickerDraftDelegateMock()
            val attachmentRepository = createAttachmentRepositoryMock(
                createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment =
                            PhotoPickerDraftAttachment(
                                sourceContentUri = "content://picker/1",
                                draftAttachment = ConversationDraftAttachment(
                                    contentType = "image/jpeg",
                                    contentUri = "content://scratch/1",
                                ),
                            ),
                    ),
                ),
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

                delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
                advanceUntilIdle()
                delegate.onPhotoPickerMediaDeselected(
                    contentUris = listOf(
                        "content://picker/1",
                        " ",
                    ),
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    draftDelegate.removeAttachment(contentUri = "content://scratch/1")
                }
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    attachmentRepository.deleteTemporaryAttachment(
                        contentUri = "content://scratch/1",
                    )
                }
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun onPhotoPickerMediaDeselected_beforeResolutionDoesNotAddResolvedAttachment() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val draftDelegate = createMediaPickerDraftDelegateMock()
            val resolutionStarted = CompletableDeferred<Unit>()
            val releaseResolution = CompletableDeferred<Unit>()
            val attachmentRepository = createAttachmentRepositoryMock(
                createDraftAttachmentsFromPhotoPickerFlow = flow {
                    resolutionStarted.complete(Unit)
                    releaseResolution.await()
                    emit(
                        PhotoPickerDraftAttachmentResult.Resolved(
                            photoPickerDraftAttachment =
                                PhotoPickerDraftAttachment(
                                    sourceContentUri = "content://picker/1",
                                    draftAttachment = ConversationDraftAttachment(
                                        contentType = "image/jpeg",
                                        contentUri = "content://scratch/1",
                                    ),
                                ),
                        ),
                    )
                },
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

                delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
                runCurrent()
                resolutionStarted.await()

                delegate.onPhotoPickerMediaDeselected(contentUris = listOf("content://picker/1"))
                releaseResolution.complete(Unit)
                advanceUntilIdle()

                verify(exactly = 0) {
                    draftDelegate.addAttachments(any())
                }
                verify(exactly = 1) {
                    draftDelegate.removeAttachment(contentUri = "content://picker/1")
                }
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    attachmentRepository.deleteTemporaryAttachment(
                        contentUri = "content://picker/1",
                    )
                }
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    attachmentRepository.deleteTemporaryAttachment(
                        contentUri = "content://scratch/1",
                    )
                }
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun onPhotoPickerMediaSelected_whenResolutionFailsEmitsMessageEffect() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val draftDelegate = createMediaPickerDraftDelegateMock()
            val attachmentRepository = createAttachmentRepositoryMock(
                createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                    PhotoPickerDraftAttachmentResult.Failed(
                        sourceContentUri = "content://picker/1",
                    ),
                ),
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

                delegate.effects.test {
                    delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShowMessage(
                            messageResId = R.string.fail_to_load_attachment,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }

                verify(exactly = 0) {
                    draftDelegate.addAttachments(any())
                }
            } finally {
                boundScope.cancel()
            }
        }
    }

    @Test
    fun photoPickerSourceContentUriByAttachmentContentUri_returnsPickerUriForResolvedAttachment() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val dispatcher = mainDispatcherRule.testDispatcher
            val draftDelegate = createMediaPickerDraftDelegateMock()
            val attachmentRepository = createAttachmentRepositoryMock(
                createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment =
                            PhotoPickerDraftAttachment(
                                sourceContentUri = "content://picker/1",
                                draftAttachment = ConversationDraftAttachment(
                                    contentType = "image/jpeg",
                                    contentUri = "content://scratch/1",
                                ),
                            ),
                    ),
                ),
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

                delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
                advanceUntilIdle()

                assertEquals(
                    "content://picker/1",
                    delegate.photoPickerSourceContentUriByAttachmentContentUri.value[
                        "content://scratch/1",
                    ],
                )
            } finally {
                boundScope.cancel()
            }
        }
    }
}
