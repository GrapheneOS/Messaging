package com.android.messaging.ui.conversation.screen.viewmodel

import android.app.Activity
import android.content.Intent
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.domain.conversation.usecase.action.CreateDefaultSmsRoleRequest
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationViewModelDefaultSmsRoleTest : BaseConversationViewModelTest() {

    @Test
    fun onDefaultSmsRolePromptActionClick_emitsRoleRequestLaunchEffectWhenIntentIsAvailable() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val requestIntent = Intent("request-sms-role")
            val viewModel = createViewModel(
                createDefaultSmsRoleRequest = CreateDefaultSmsRoleRequest {
                    requestIntent
                },
            )

            viewModel.effects.test {
                viewModel.onDefaultSmsRolePromptActionClick()
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.LaunchDefaultSmsRoleRequest(
                        intent = requestIntent,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onDefaultSmsRolePromptActionClick_emitsErrorMessageWhenIntentIsUnavailable() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(
                createDefaultSmsRoleRequest = CreateDefaultSmsRoleRequest {
                    null
                },
            )

            viewModel.effects.test {
                viewModel.onDefaultSmsRolePromptActionClick()
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = R.string.activity_not_found_message,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onDefaultSmsRoleRequestResult_isHandledByDraftDelegateFirst() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            every {
                draftDelegate.mock.onDefaultSmsRoleRequestResult(
                    resultCode = Activity.RESULT_OK,
                )
            } returns true
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                messageSelectionDelegate = messageSelectionDelegate.mock,
            )

            viewModel.effects.test {
                viewModel.onDefaultSmsRoleRequestResult(resultCode = Activity.RESULT_OK)
                advanceUntilIdle()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                draftDelegate.mock.onDefaultSmsRoleRequestResult(
                    resultCode = Activity.RESULT_OK,
                )
            }
            verify(exactly = 0) {
                messageSelectionDelegate.mock.onDefaultSmsRoleRequestResult(any())
            }
        }
    }

    @Test
    fun onDefaultSmsRoleRequestResult_fallsBackToMessageSelectionDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            every {
                draftDelegate.mock.onDefaultSmsRoleRequestResult(
                    resultCode = Activity.RESULT_OK,
                )
            } returns false
            every {
                messageSelectionDelegate.mock.onDefaultSmsRoleRequestResult(
                    resultCode = Activity.RESULT_OK,
                )
            } returns true
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                messageSelectionDelegate = messageSelectionDelegate.mock,
            )

            viewModel.effects.test {
                viewModel.onDefaultSmsRoleRequestResult(resultCode = Activity.RESULT_OK)
                advanceUntilIdle()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                draftDelegate.mock.onDefaultSmsRoleRequestResult(
                    resultCode = Activity.RESULT_OK,
                )
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.onDefaultSmsRoleRequestResult(
                    resultCode = Activity.RESULT_OK,
                )
            }
        }
    }

    @Test
    fun onDefaultSmsRoleRequestResult_emitsSuccessToastForUnhandledResultOk() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            every {
                draftDelegate.mock.onDefaultSmsRoleRequestResult(
                    resultCode = Activity.RESULT_OK,
                )
            } returns false
            every {
                messageSelectionDelegate.mock.onDefaultSmsRoleRequestResult(
                    resultCode = Activity.RESULT_OK,
                )
            } returns false
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                messageSelectionDelegate = messageSelectionDelegate.mock,
            )

            viewModel.effects.test {
                viewModel.onDefaultSmsRoleRequestResult(resultCode = Activity.RESULT_OK)
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = R.string.toast_after_setting_default_sms_app,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onDefaultSmsRoleRequestLaunchFailed_emitsActivityNotFoundMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onDefaultSmsRoleRequestLaunchFailed()
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = R.string.activity_not_found_message,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
