package com.android.messaging.ui.conversation.messages.delegate.selection

import android.app.Activity
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageSelectionDelegateResendTest :
    BaseConversationMessageSelectionDelegateTest() {

    @Test
    fun resendAction_resendsSelectedMessageAndClearsSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canResendMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Resend,
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    harness.conversationsRepository.resendMessage(messageId = "message-1")
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageResendClick_resendsMessageWithoutSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.delegate.onMessageResendClick(messageId = "message-1")
                advanceUntilIdle()

                verify(exactly = 1) {
                    harness.conversationsRepository.resendMessage(messageId = "message-1")
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun resendAction_whenSmsIsNotCapable_emitsSmsDisabledMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness(
                actionRequirements = createActionRequirementsMock(
                    initialResult = ConversationActionRequirementsResult.SmsNotCapable,
                ),
            )

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canResendMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Resend,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShowMessage(
                            messageResId = R.string.sms_disabled,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun resendAction_whenPreferredSmsSimIsMissing_emitsNoPreferredSimMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness(
                actionRequirements = createActionRequirementsMock(
                    initialResult = ConversationActionRequirementsResult.NoPreferredSmsSim,
                ),
            )

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canResendMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Resend,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShowMessage(
                            messageResId = R.string.no_preferred_sim_selected,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun resendAction_whenDefaultSmsRoleIsMissing_promptsAndResendsAfterRoleRequestSucceeds() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val actionRequirements = createActionRequirementsMock(
                results = listOf(
                    ConversationActionRequirementsResult.MissingDefaultSmsRole,
                    ConversationActionRequirementsResult.Ready,
                ),
            )
            val harness = createHarness(actionRequirements = actionRequirements)

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canResendMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Resend,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.RequestDefaultSmsRole(isSending = true),
                        awaitItem(),
                    )
                    verify(exactly = 0) {
                        harness.conversationsRepository.resendMessage(any())
                    }

                    assertTrue(
                        harness.delegate.onDefaultSmsRoleRequestResult(
                            resultCode = Activity.RESULT_OK,
                        ),
                    )
                    advanceUntilIdle()

                    verify(exactly = 1) {
                        harness.conversationsRepository.resendMessage(messageId = "message-1")
                    }
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onDefaultSmsRoleRequestResult_withoutPendingResend_returnsFalse() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                assertFalse(
                    harness.delegate.onDefaultSmsRoleRequestResult(
                        resultCode = Activity.RESULT_OK,
                    ),
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onDefaultSmsRoleRequestResult_whenCanceled_clearsPendingResend() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val actionRequirements = createActionRequirementsMock(
                initialResult = ConversationActionRequirementsResult.MissingDefaultSmsRole,
            )
            val harness = createHarness(actionRequirements = actionRequirements)

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canResendMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Resend,
                    )
                    advanceUntilIdle()
                    awaitItem()

                    assertTrue(
                        harness.delegate.onDefaultSmsRoleRequestResult(
                            resultCode = Activity.RESULT_CANCELED,
                        ),
                    )
                    advanceUntilIdle()

                    assertFalse(
                        harness.delegate.onDefaultSmsRoleRequestResult(
                            resultCode = Activity.RESULT_OK,
                        ),
                    )
                    advanceUntilIdle()

                    verify(exactly = 0) {
                        harness.conversationsRepository.resendMessage(any())
                    }
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }
}
