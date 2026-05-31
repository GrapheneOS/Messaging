package com.android.messaging.ui.conversation.screen.viewmodel

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.domain.conversation.usecase.participant.CanAddMoreConversationParticipants
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationViewModelUiStateTest : BaseConversationViewModelTest() {

    @Test
    fun scaffoldUiState_combinesDelegateStatesUsingComposerMapper() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val composerUiState = ConversationComposerUiState(
                messageText = "Mapped text",
                isSendEnabled = true,
            )
            val draftDelegate = createDraftDelegateMock()
            val messagesDelegate = createMessagesDelegateMock()
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                messagesDelegate = messagesDelegate.mock,
                messageSelectionDelegate = messageSelectionDelegate.mock,
                metadataDelegate = metadataDelegate.mock,
                composerUiStateMapper = createComposerUiStateMapperMock(
                    mappedUiState = composerUiState,
                ),
            )

            val metadataState = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Single(
                    photoUri = null,
                ),
                participantCount = 2,
                otherParticipantDisplayDestination = null,
                otherParticipantPhoneNumber = null,
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.Editable,
            )
            val messagesState = ConversationMessagesUiState.Present(
                messages = listOf(
                    createMessageUiModel(),
                ).toPersistentList(),
            )
            val selectionState = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf(MESSAGE_ID),
            )
            metadataDelegate.stateFlow.value = metadataState
            messagesDelegate.stateFlow.value = messagesState
            messageSelectionDelegate.stateFlow.value = selectionState
            draftDelegate.stateFlow.value = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Draft text",
                ),
            )

            viewModel.scaffoldUiState.test {
                assertEquals(
                    ConversationScreenScaffoldUiState(
                        composer = composerUiState,
                    ),
                    awaitItem(),
                )
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenScaffoldUiState(
                        canAddPeople = false,
                        canArchive = true,
                        canDeleteConversation = true,
                        canEditSubject = true,
                        metadata = metadataState,
                        messages = messagesState,
                        composer = composerUiState,
                        selection = selectionState,
                    ),
                    expectMostRecentItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun mediaPickerOverlayUiState_combinesComposerMetadataAndPhotoPickerSourceUris() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val photoPickerSourceUris = persistentMapOf(
                "content://scratch/1" to "content://picker/1",
            )
            val composerUiState = ConversationComposerUiState(
                isSendEnabled = true,
            )
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(
                mediaPickerDelegate = mediaPickerDelegate.mock,
                metadataDelegate = metadataDelegate.mock,
                composerUiStateMapper = createComposerUiStateMapperMock(
                    mappedUiState = composerUiState,
                ),
            )

            viewModel.mediaPickerOverlayUiState.test {
                awaitItem()

                mediaPickerDelegate.photoPickerSourceContentUriByAttachmentContentUriFlow.value =
                    photoPickerSourceUris
                awaitItem()

                metadataDelegate.stateFlow.value = ConversationMetadataUiState.Present(
                    title = "Weekend plan",
                    selfParticipantId = "self-1",
                    avatar = ConversationMetadataUiState.Avatar.Single(
                        photoUri = null,
                    ),
                    participantCount = 2,
                    otherParticipantDisplayDestination = null,
                    otherParticipantPhoneNumber = null,
                    otherParticipantContactLookupKey = null,
                    isArchived = false,
                    composerAvailability = ConversationComposerAvailability.Editable,
                )

                val overlayState = awaitItem()

                assertEquals("Weekend plan", overlayState.conversationTitle)
                assertEquals(
                    photoPickerSourceUris,
                    overlayState.photoPickerSourceContentUriByAttachmentContentUri,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun scaffoldUiState_enablesAddPeopleWhenConversationIsBelowRecipientLimit() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val canAddMoreConversationParticipants = mockk<CanAddMoreConversationParticipants>()
            every {
                canAddMoreConversationParticipants.invoke(participantCount = 2)
            } returns true
            val viewModel = createViewModel(
                metadataDelegate = metadataDelegate.mock,
                canAddMoreConversationParticipants = canAddMoreConversationParticipants,
            )

            metadataDelegate.stateFlow.value = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Group,
                participantCount = 2,
                otherParticipantDisplayDestination = null,
                otherParticipantPhoneNumber = null,
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.Editable,
            )
            viewModel.scaffoldUiState.test {
                assertEquals(false, awaitItem().canAddPeople)
                advanceUntilIdle()
                assertEquals(true, awaitItem().canAddPeople)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun scaffoldUiState_disablesAddPeopleWhenConversationReachedRecipientLimit() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val canAddMoreConversationParticipants = mockk<CanAddMoreConversationParticipants>()
            every {
                canAddMoreConversationParticipants.invoke(participantCount = 10)
            } returns false
            val viewModel = createViewModel(
                metadataDelegate = metadataDelegate.mock,
                canAddMoreConversationParticipants = canAddMoreConversationParticipants,
            )

            metadataDelegate.stateFlow.value = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Group,
                participantCount = 10,
                otherParticipantDisplayDestination = null,
                otherParticipantPhoneNumber = null,
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.Editable,
            )
            viewModel.scaffoldUiState.test {
                assertEquals(false, awaitItem().canAddPeople)
                advanceUntilIdle()
                assertEquals(false, awaitItem().canAddPeople)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun scaffoldUiState_reflectsMetadataDeleteConfirmationVisibility() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(metadataDelegate = metadataDelegate.mock)

            viewModel.scaffoldUiState.test {
                assertEquals(false, awaitItem().isDeleteConversationConfirmationVisible)

                metadataDelegate.deleteConfirmationVisibleFlow.value = true
                advanceUntilIdle()
                assertEquals(true, awaitItem().isDeleteConversationConfirmationVisible)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
