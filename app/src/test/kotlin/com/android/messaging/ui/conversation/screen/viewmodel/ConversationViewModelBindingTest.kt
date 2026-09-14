package com.android.messaging.ui.conversation.screen.viewmodel

import androidx.lifecycle.ViewModelStore
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationViewModelBindingTest : BaseConversationViewModelTest() {

    @Test
    fun init_bindsAllDelegates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val audioRecordingDelegate = createAudioRecordingDelegateMock()
            val composerAttachmentsDelegate = createComposerAttachmentsDelegateMock()
            val messagesDelegate = createMessagesDelegateMock()
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val metadataDelegate = createMetadataDelegateMock()
            val focusDelegate = createFocusDelegateMock()
            val viewModel = createViewModel(
                audioRecordingDelegate = audioRecordingDelegate.mock,
                composerAttachmentsDelegate = composerAttachmentsDelegate.mock,
                draftDelegate = draftDelegate.mock,
                messagesDelegate = messagesDelegate.mock,
                messageSelectionDelegate = messageSelectionDelegate.mock,
                mediaPickerDelegate = mediaPickerDelegate.mock,
                metadataDelegate = metadataDelegate.mock,
                focusDelegate = focusDelegate.mock,
            )

            advanceUntilIdle()

            assertEquals(1, draftDelegate.bindCalls.size)
            assertEquals(1, audioRecordingDelegate.bindCalls.size)
            assertEquals(1, composerAttachmentsDelegate.bindCalls.size)
            assertEquals(1, messagesDelegate.bindCalls.size)
            assertEquals(1, messageSelectionDelegate.bindCalls.size)
            assertEquals(1, mediaPickerDelegate.bindCalls.size)
            assertEquals(1, metadataDelegate.bindCalls.size)
            assertEquals(1, focusDelegate.bindCalls.size)
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                focusDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.stateFlow,
                composerAttachmentsDelegate.bindCalls.single().draftStateFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                audioRecordingDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                messagesDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                messageSelectionDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                mediaPickerDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                metadataDelegate.bindCalls.single().conversationIdFlow,
            )
            assertEquals(null, draftDelegate.bindCalls.single().conversationIdFlow.value)

            viewModel.onConversationIdChanged(conversationId = CONVERSATION_ID)

            assertEquals(
                CONVERSATION_ID,
                draftDelegate.bindCalls.single().conversationIdFlow.value,
            )
            verify(exactly = 1) {
                messageSelectionDelegate.mock.dismissMessageSelection()
            }
        }
    }

    @Test
    fun onCleared_flushesDraftDelegateAndMediaPickerDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val audioRecordingDelegate = createAudioRecordingDelegateMock()
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val focusDelegate = createFocusDelegateMock()
            val viewModelStore = ViewModelStore()
            createViewModelInStore(
                viewModelStore = viewModelStore,
                viewModelFactory = {
                    createViewModel(
                        audioRecordingDelegate = audioRecordingDelegate.mock,
                        draftDelegate = draftDelegate.mock,
                        mediaPickerDelegate = mediaPickerDelegate.mock,
                        focusDelegate = focusDelegate.mock,
                    )
                },
            )

            viewModelStore.clear()

            verify(exactly = 1) {
                audioRecordingDelegate.mock.onScreenCleared()
            }
            verify(exactly = 1) {
                draftDelegate.mock.flushDraft()
            }
            verify(exactly = 1) {
                mediaPickerDelegate.mock.onScreenCleared()
            }
            verify(exactly = 1) {
                focusDelegate.mock.setScreenFocused(focused = false)
            }
        }
    }
}
