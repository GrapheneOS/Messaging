package com.android.messaging.ui.conversation.screen.viewmodel

import app.cash.turbine.test
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationViewModelAttachmentPreviewTest : BaseConversationViewModelTest() {

    @Test
    fun onOpenStartupAttachment_emitsAttachmentPreviewForConversationImages() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onOpenStartupAttachment(
                    conversationId = CONVERSATION_ID,
                    startupAttachment = ConversationEntryStartupAttachment(
                        contentType = "image/jpeg",
                        contentUri = "content://media/image/1",
                    ),
                )
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.OpenAttachmentPreview(
                        contentType = "image/jpeg",
                        contentUri = "content://media/image/1",
                        imageCollectionUri = MessagingContentProvider
                            .buildConversationImagesUri(CONVERSATION_ID)
                            .toString(),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun attachmentPreviewEvents_useDraftAndConversationImageCollections() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onConversationIdChanged(conversationId = CONVERSATION_ID)

            viewModel.effects.test {
                viewModel.onAttachmentClicked(
                    attachment = ComposerAttachmentUiModel.Resolved.VisualMedia.Image(
                        key = "attachment-1",
                        contentType = "image/jpeg",
                        contentUri = "content://media/image/1",
                        captionText = "",
                        width = 640,
                        height = 480,
                    ),
                )
                advanceUntilIdle()
                assertEquals(
                    ConversationScreenEffect.OpenAttachmentPreview(
                        contentType = "image/jpeg",
                        contentUri = "content://media/image/1",
                        imageCollectionUri = MessagingContentProvider
                            .buildDraftImagesUri(CONVERSATION_ID)
                            .toString(),
                    ),
                    awaitItem(),
                )

                viewModel.onMessageAttachmentClicked(
                    contentType = "image/png",
                    contentUri = "content://media/image/2",
                )
                advanceUntilIdle()
                assertEquals(
                    ConversationScreenEffect.OpenAttachmentPreview(
                        contentType = "image/png",
                        contentUri = "content://media/image/2",
                        imageCollectionUri = MessagingContentProvider
                            .buildConversationImagesUri(CONVERSATION_ID)
                            .toString(),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
