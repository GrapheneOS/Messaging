package com.android.messaging.ui.conversation.messages.delegate.selection

import android.net.Uri
import app.cash.turbine.test
import com.android.messaging.data.media.model.AttachmentToSave
import com.android.messaging.data.media.model.SaveAttachmentsResult
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.every
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageSelectionDelegateSaveAttachmentTest :
    BaseConversationMessageSelectionDelegateTest() {

    @Test
    fun onMessageLongClick_exposesSaveAttachmentActionWhenCanSaveAttachments() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = null,
                        canSaveAttachments = true,
                        parts = persistentListOf(
                            createAttachmentPart(),
                        ),
                    ),
                )
                advanceUntilIdle()

                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf(
                        ConversationMessageSelectionAction.Delete,
                        ConversationMessageSelectionAction.SaveAttachment,
                        ConversationMessageSelectionAction.Details,
                    ),
                    harness.delegate.state.value.availableActions,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun saveAttachmentAction_emitsResultEffectAndClearsSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()
            val attachments = listOf(
                AttachmentToSave(
                    contentType = IMAGE_ATTACHMENT_CONTENT_TYPE,
                    contentUri = IMAGE_ATTACHMENT_CONTENT_URI,
                ),
            )
            every {
                harness.conversationAttachmentsRepository.saveAttachmentsToMediaStore(
                    attachments = attachments,
                )
            } returns flowOf(
                SaveAttachmentsResult(
                    imageCount = 1,
                    videoCount = 0,
                    otherCount = 0,
                    failCount = 0,
                ),
            )

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = null,
                        canSaveAttachments = true,
                        parts = persistentListOf(
                            createAttachmentPart(),
                        ),
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.SaveAttachment,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShowSaveAttachmentsResult(
                            imageCount = 1,
                            videoCount = 0,
                            otherCount = 0,
                            failCount = 0,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    harness.conversationAttachmentsRepository.saveAttachmentsToMediaStore(
                        attachments = attachments,
                    )
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun saveAttachmentAction_skipsAttachmentsWithBlankContentTypeOrNullUri() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()
            val attachments = listOf(
                AttachmentToSave(
                    contentType = IMAGE_ATTACHMENT_CONTENT_TYPE,
                    contentUri = IMAGE_ATTACHMENT_CONTENT_URI,
                ),
            )
            every {
                harness.conversationAttachmentsRepository.saveAttachmentsToMediaStore(
                    attachments = attachments,
                )
            } returns flowOf(
                SaveAttachmentsResult(
                    imageCount = 1,
                    videoCount = 0,
                    otherCount = 0,
                    failCount = 0,
                ),
            )

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = null,
                        canSaveAttachments = true,
                        parts = persistentListOf(
                            createAttachmentPart(),
                            ConversationMessagePartUiModel.Attachment.File(
                                text = null,
                                contentType = "",
                                contentUri = Uri.parse("content://media/blank"),
                                width = 0,
                                height = 0,
                            ),
                            ConversationMessagePartUiModel.Attachment.File(
                                text = null,
                                contentType = "application/pdf",
                                contentUri = null,
                                width = 0,
                                height = 0,
                            ),
                        ),
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.SaveAttachment,
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    harness.conversationAttachmentsRepository.saveAttachmentsToMediaStore(
                        attachments = attachments,
                    )
                }
            } finally {
                harness.cancel()
            }
        }
    }
}
