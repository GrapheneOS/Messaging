package com.android.messaging.ui.conversation.screen.viewmodel

import app.cash.turbine.test
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
internal class ConversationViewModelEffectRelayTest : BaseConversationViewModelTest() {

    @Test
    fun mediaPickerEffects_areExposedAsScreenEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val viewModel = createViewModel(
                mediaPickerDelegate = mediaPickerDelegate.mock,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                mediaPickerDelegate.effectsFlow.emit(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = 123,
                    ),
                )

                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = 123,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun messageSelectionEffects_areExposedAsScreenEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            val viewModel = createViewModel(
                messageSelectionDelegate = messageSelectionDelegate.mock,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                messageSelectionDelegate.effectsFlow.emit(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = 456,
                    ),
                )

                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = 456,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun draftEffects_areExposedAsScreenEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                draftDelegate.effectsFlow.emit(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = 789,
                    ),
                )

                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = 789,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun metadataEffects_areExposedAsScreenEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(
                metadataDelegate = metadataDelegate.mock,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                metadataDelegate.effectsFlow.emit(ConversationScreenEffect.CloseConversation)

                assertEquals(ConversationScreenEffect.CloseConversation, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
