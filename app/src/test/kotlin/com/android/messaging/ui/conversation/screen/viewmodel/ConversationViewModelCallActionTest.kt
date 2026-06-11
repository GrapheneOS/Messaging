package com.android.messaging.ui.conversation.screen.viewmodel

import app.cash.turbine.test
import com.android.messaging.domain.conversation.usecase.telephony.IsDeviceVoiceCapable
import com.android.messaging.domain.conversation.usecase.telephony.IsEmergencyPhoneNumber
import com.android.messaging.testutil.TEST_CALL_ACTION_PHONE_NUMBER
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.screen.ConversationViewModel
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationViewModelCallActionTest : BaseConversationViewModelTest() {

    @Test
    fun scaffoldUiState_allowsCallForVoiceCapableOneOnOneNonEmergencyConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createCallActionViewModel(
                metadataState = createOneOnOneMetadataState(),
                isDeviceVoiceCapable = true,
            )

            viewModel.scaffoldUiState.test {
                assertEquals(true, awaitItem().canCall)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun scaffoldUiState_hidesCallForNonVoiceCapableDevice() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createCallActionViewModel(
                metadataState = createOneOnOneMetadataState(),
                isDeviceVoiceCapable = false,
            )

            viewModel.scaffoldUiState.test {
                assertFalse(awaitItem().canCall)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun scaffoldUiState_hidesCallForEmergencyConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createCallActionViewModel(
                metadataState = createOneOnOneMetadataState(
                    phoneNumber = EMERGENCY_PHONE_NUMBER,
                ),
                isDeviceVoiceCapable = true,
            )

            viewModel.scaffoldUiState.test {
                assertFalse(awaitItem().canCall)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun scaffoldUiState_checksEmergencyPhoneNumberBeforeEnablingCallAction() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val isEmergencyPhoneNumber = mockk<IsEmergencyPhoneNumber>()
            every {
                isEmergencyPhoneNumber.invoke(phoneNumber = TEST_CALL_ACTION_PHONE_NUMBER)
            } returns false
            val viewModel = createViewModel(
                metadataDelegate = metadataDelegate.mock,
                isDeviceVoiceCapable = IsDeviceVoiceCapable { true },
                isEmergencyPhoneNumber = isEmergencyPhoneNumber,
            )

            metadataDelegate.stateFlow.value = createOneOnOneMetadataState()

            viewModel.scaffoldUiState.test {
                assertEquals(false, awaitItem().canCall)
                advanceUntilIdle()
                assertEquals(true, awaitItem().canCall)
                cancelAndIgnoreRemainingEvents()
            }

            verify(atLeast = 1) {
                isEmergencyPhoneNumber.invoke(phoneNumber = TEST_CALL_ACTION_PHONE_NUMBER)
            }
        }
    }

    @Test
    fun onCallClick_doesNotEmitCallEffectForEmergencyConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createCallActionViewModel(
                metadataState = createOneOnOneMetadataState(
                    phoneNumber = EMERGENCY_PHONE_NUMBER,
                ),
                isDeviceVoiceCapable = true,
            )

            viewModel.effects.test {
                viewModel.onCallClick()
                advanceUntilIdle()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onCallClick_emitsCallEffectForNonEmergencyConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createCallActionViewModel(
                metadataState = createOneOnOneMetadataState(),
                isDeviceVoiceCapable = true,
            )

            viewModel.effects.test {
                viewModel.onCallClick()
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.PlacePhoneCall(
                        phoneNumber = TEST_CALL_ACTION_PHONE_NUMBER,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onCallClick_checksEmergencyPhoneNumberBeforeEmittingCallEffect() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            metadataDelegate.stateFlow.value = createOneOnOneMetadataState()
            val isEmergencyPhoneNumber = mockk<IsEmergencyPhoneNumber>()
            every {
                isEmergencyPhoneNumber.invoke(phoneNumber = TEST_CALL_ACTION_PHONE_NUMBER)
            } returns false
            val viewModel = createViewModel(
                metadataDelegate = metadataDelegate.mock,
                isEmergencyPhoneNumber = isEmergencyPhoneNumber,
            )

            viewModel.effects.test {
                viewModel.onCallClick()
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.PlacePhoneCall(
                        phoneNumber = TEST_CALL_ACTION_PHONE_NUMBER,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }

            verify(atLeast = 1) {
                isEmergencyPhoneNumber.invoke(phoneNumber = TEST_CALL_ACTION_PHONE_NUMBER)
            }
        }
    }

    private fun createCallActionViewModel(
        metadataState: ConversationMetadataUiState,
        isDeviceVoiceCapable: Boolean,
    ): ConversationViewModel {
        val metadataDelegate = createMetadataDelegateMock()
        metadataDelegate.stateFlow.value = metadataState
        return createViewModel(
            metadataDelegate = metadataDelegate.mock,
            isDeviceVoiceCapable = IsDeviceVoiceCapable {
                isDeviceVoiceCapable
            },
            isEmergencyPhoneNumber = IsEmergencyPhoneNumber { phoneNumber ->
                phoneNumber == EMERGENCY_PHONE_NUMBER
            },
        )
    }
}
