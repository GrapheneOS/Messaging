package com.android.messaging.ui.conversation.composer.delegate.draft

import android.app.Activity
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.slot
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
internal class ConversationDraftDelegateActionRequirementsTest :
    BaseConversationDraftDelegateTest() {

    @Test
    fun onSendClick_whenSmsIsNotCapable_emitsSmsDisabledMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createBoundLoadedDelegateHarness(
                actionRequirements = createActionRequirementsMock(
                    initialResult = ConversationActionRequirementsResult.SmsNotCapable,
                ),
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                harness.delegate.effects.test {
                    harness.delegate.onSendClick()
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
    fun onSendClick_whenPreferredSmsSimIsMissing_emitsNoPreferredSimMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createBoundLoadedDelegateHarness(
                actionRequirements = createActionRequirementsMock(
                    initialResult = ConversationActionRequirementsResult.NoPreferredSmsSim,
                ),
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                harness.delegate.effects.test {
                    harness.delegate.onSendClick()
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
    fun onSendClick_whenDefaultSmsRoleIsMissing_promptsAndSendsAfterRoleRequestSucceeds() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val sendConversationDraft = createSendConversationDraftMock()
            val actionRequirements = createActionRequirementsMock(
                results = listOf(
                    ConversationActionRequirementsResult.MissingDefaultSmsRole,
                    ConversationActionRequirementsResult.Ready,
                ),
            )
            val harness = createBoundLoadedDelegateHarness(
                sendConversationDraft = sendConversationDraft,
                actionRequirements = actionRequirements,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                harness.delegate.effects.test {
                    harness.delegate.onSendClick()
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.RequestDefaultSmsRole(isSending = true),
                        awaitItem(),
                    )
                    verify(exactly = 0) {
                        @Suppress("UnusedFlow")
                        sendConversationDraft.invoke(
                            conversationId = any(),
                            draft = any(),
                            ignoreMessageSizeLimit = any(),
                        )
                    }

                    assertTrue(
                        harness.delegate.onDefaultSmsRoleRequestResult(
                            resultCode = Activity.RESULT_OK,
                        ),
                    )
                    advanceUntilIdle()

                    val sentDraft = slot<ConversationDraft>()
                    verify(exactly = 1) {
                        @Suppress("UnusedFlow")
                        sendConversationDraft.invoke(
                            conversationId = any(),
                            draft = capture(sentDraft),
                            ignoreMessageSizeLimit = any(),
                        )
                    }
                    assertEquals("Hello", sentDraft.captured.messageText)
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onDefaultSmsRoleRequestResult_withoutPendingSend_returnsFalse() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createBoundLoadedDelegateHarness()

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
    fun onDefaultSmsRoleRequestResult_whenCanceled_clearsPendingSendWithoutSending() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val sendConversationDraft = createSendConversationDraftMock()
            val actionRequirements = createActionRequirementsMock(
                initialResult = ConversationActionRequirementsResult.MissingDefaultSmsRole,
            )
            val harness = createBoundLoadedDelegateHarness(
                sendConversationDraft = sendConversationDraft,
                actionRequirements = actionRequirements,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                harness.delegate.effects.test {
                    harness.delegate.onSendClick()
                    advanceUntilIdle()
                    awaitItem()

                    assertFalse(
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
                        @Suppress("UnusedFlow")
                        sendConversationDraft.invoke(
                            conversationId = any(),
                            draft = any(),
                            ignoreMessageSizeLimit = any(),
                        )
                    }
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }
}
