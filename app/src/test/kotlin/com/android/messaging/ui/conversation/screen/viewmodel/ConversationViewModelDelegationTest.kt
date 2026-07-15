package com.android.messaging.ui.conversation.screen.viewmodel

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationViewModelDelegationTest : BaseConversationViewModelTest() {

    @Test
    fun onSeedDraft_forwardsToDraftDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
            )
            val draft = ConversationDraft(
                messageText = "Hello",
                selfParticipantId = "self-1",
            )

            viewModel.onSeedDraft(
                conversationId = CONVERSATION_ID,
                draft = draft,
            )

            verify(exactly = 1) {
                draftDelegate.mock.seedDraft(
                    conversationId = CONVERSATION_ID,
                    draft = draft,
                )
            }
        }
    }

    @Test
    fun eventMethods_forwardToDelegates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val audioRecordingDelegate = createAudioRecordingDelegateMock()
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            draftDelegate.stateFlow.value = draftDelegate.stateFlow.value.copy(
                draft = ConversationDraft(
                    selfParticipantId = AUDIO_RECORDING_SELF_PARTICIPANT_ID,
                ),
            )
            val viewModel = createViewModel(
                audioRecordingDelegate = audioRecordingDelegate.mock,
                draftDelegate = draftDelegate.mock,
                messageSelectionDelegate = messageSelectionDelegate.mock,
                mediaPickerDelegate = mediaPickerDelegate.mock,
            )

            viewModel.onMessageSelectionActionClick(
                action = ConversationMessageSelectionAction.Delete,
            )
            viewModel.onMessageTextChanged(text = "Hello")
            viewModel.onAudioRecordingStart(isLocked = false)
            viewModel.onAudioRecordingStart(isLocked = true)
            viewModel.onAudioRecordingFinish()
            viewModel.onAudioRecordingCancel()
            viewModel.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
            viewModel.onPhotoPickerMediaDeselected(contentUris = listOf("content://picker/2"))
            viewModel.onSendClick()
            viewModel.dismissDeleteMessageConfirmation()
            viewModel.dismissMessageSelection()
            viewModel.confirmDeleteSelectedMessages()
            viewModel.persistDraft()

            verify(exactly = 1) {
                messageSelectionDelegate.mock.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Delete,
                )
            }
            verify(exactly = 1) {
                draftDelegate.mock.onMessageTextChanged(messageText = "Hello")
            }
            verify(exactly = 1) {
                audioRecordingDelegate.mock.startRecording(
                    selfParticipantId = AUDIO_RECORDING_SELF_PARTICIPANT_ID,
                )
            }
            verify(exactly = 1) {
                audioRecordingDelegate.mock.startLockedRecording(
                    selfParticipantId = AUDIO_RECORDING_SELF_PARTICIPANT_ID,
                )
            }
            verify(exactly = 1) {
                audioRecordingDelegate.mock.finishRecording()
            }
            verify(exactly = 1) {
                audioRecordingDelegate.mock.cancelRecording()
            }
            verify(exactly = 1) {
                mediaPickerDelegate.mock.onPhotoPickerMediaSelected(
                    contentUris = listOf("content://picker/1"),
                )
            }
            verify(exactly = 1) {
                mediaPickerDelegate.mock.onPhotoPickerMediaDeselected(
                    contentUris = listOf("content://picker/2"),
                )
            }
            verify(exactly = 1) {
                draftDelegate.mock.onSendClick()
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.dismissDeleteMessageConfirmation()
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.dismissMessageSelection()
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.confirmDeleteSelectedMessages()
            }
            verify(exactly = 1) {
                draftDelegate.mock.persistDraft()
            }
        }
    }

    @Test
    fun conversationActionMethods_forwardToMetadataDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(metadataDelegate = metadataDelegate.mock)

            viewModel.onArchiveConversationClick()
            viewModel.onUnarchiveConversationClick()
            viewModel.onAddContactClick()
            viewModel.onDeleteConversationClick()
            viewModel.confirmDeleteConversation()
            viewModel.dismissDeleteConversationConfirmation()

            verify(exactly = 1) { metadataDelegate.mock.onArchiveConversationClick() }
            verify(exactly = 1) { metadataDelegate.mock.onUnarchiveConversationClick() }
            verify(exactly = 1) { metadataDelegate.mock.onAddContactClick() }
            verify(exactly = 1) { metadataDelegate.mock.onDeleteConversationClick() }
            verify(exactly = 1) { metadataDelegate.mock.confirmDeleteConversation() }
            verify(exactly = 1) { metadataDelegate.mock.dismissDeleteConversationConfirmation() }
        }
    }

    @Test
    fun onScreenForegrounded_forwardsCancelNotificationFlagToFocusDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val focusDelegate = createFocusDelegateMock()
            val viewModel = createViewModel(focusDelegate = focusDelegate.mock)

            viewModel.onScreenForegrounded(cancelNotification = true)
            viewModel.onScreenForegrounded(cancelNotification = false)

            verify(exactly = 1) {
                focusDelegate.mock.setScreenFocused(
                    focused = true,
                    cancelNotification = true,
                )
            }
            verify(exactly = 1) {
                focusDelegate.mock.setScreenFocused(
                    focused = true,
                    cancelNotification = false,
                )
            }
        }
    }

    @Test
    fun onScreenBackgrounded_unsetsFocusOnDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val focusDelegate = createFocusDelegateMock()
            val viewModel = createViewModel(focusDelegate = focusDelegate.mock)

            viewModel.onScreenBackgrounded()

            verify(exactly = 1) {
                focusDelegate.mock.setScreenFocused(focused = false)
            }
        }
    }

    private companion object {
        private const val AUDIO_RECORDING_SELF_PARTICIPANT_ID = "self-recording"
    }
}
