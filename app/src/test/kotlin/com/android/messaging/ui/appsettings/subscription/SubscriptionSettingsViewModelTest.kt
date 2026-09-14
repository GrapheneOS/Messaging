package com.android.messaging.ui.appsettings.subscription

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.domain.subscriptionsettings.usecase.IsValidSelfPhoneNumber
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsDelegate
import com.android.messaging.ui.appsettings.subscription.model.PhoneNumberDialogUiState
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsAction as Action
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsNavEvent
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsScreenEffect
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SubscriptionSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_bindsSubscriptionDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = mockDelegate()

            createViewModel(delegate = delegate)

            verify(exactly = 1) {
                delegate.bind(any())
            }
        }
    }

    @Test
    fun uiState_exposesSubscriptionMatchingSeededSubId() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val target = SubscriptionUiState(subId = SubId(2), displayName = "SIM 2")
            val stateFlow = MutableStateFlow(
                SubscriptionSettingsUiState(
                    isLoaded = true,
                    subscriptions = persistentListOf(
                        SubscriptionUiState(subId = SubId(1), displayName = "SIM 1"),
                        target,
                    ),
                ),
            )
            val viewModel = createViewModel(delegate = mockDelegate(stateFlow), subId = 2)

            assertEquals(target, viewModel.uiState.value)
        }
    }

    @Test
    fun onGroupMmsChanged_delegatesWithSeededSubId() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = mockDelegate()
            val viewModel = createViewModel(delegate = delegate, subId = 3)

            viewModel.onAction(Action.GroupMmsChanged(enabled = false))

            verify(exactly = 1) {
                delegate.onGroupMmsChanged(subId = SubId(3), enabled = false)
            }
        }
    }

    @Test
    fun onPhoneNumberConfirmed_delegatesWithSeededSubId() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = mockDelegate()
            val viewModel = createViewModel(delegate = delegate, subId = 1)

            viewModel.onAction(Action.PhoneNumberClicked)
            viewModel.onAction(Action.PhoneNumberConfirmed(phoneNumber = "+1555000111"))

            verify(exactly = 1) {
                delegate.onPhoneNumberChanged(subId = SubId(1), phoneNumber = "+1555000111")
            }
            assertEquals(
                "a number that was stored leaves nothing to correct, so the dialog closes",
                PhoneNumberDialogUiState(),
                viewModel.phoneNumberDialogState.value,
            )
        }
    }

    @Test
    fun onWirelessAlertsClick_emitsOpenWirelessAlerts() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onAction(Action.WirelessAlertsClicked)

                assertEquals(SubscriptionSettingsScreenEffect.OpenWirelessAlerts, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun whenSubscriptionRemovedAfterLoad_emitsCloseNavEvent() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val target = SubscriptionUiState(subId = SubId(1), displayName = "SIM 1")
            val stateFlow = MutableStateFlow(
                SubscriptionSettingsUiState(
                    isLoaded = true,
                    subscriptions = persistentListOf(target),
                ),
            )
            val viewModel = createViewModel(delegate = mockDelegate(stateFlow), subId = 1)

            viewModel.navigationEvents.test {
                stateFlow.value = SubscriptionSettingsUiState(
                    isLoaded = true,
                    subscriptions = persistentListOf(),
                )

                assertEquals(SubscriptionSettingsNavEvent.Close, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun refreshState_refreshesSubscriptionDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = mockDelegate()
            val viewModel = createViewModel(delegate = delegate)

            viewModel.refreshState()

            verify(exactly = 1) {
                delegate.refresh()
            }
        }
    }

    @Test
    fun onPhoneNumberConfirmed_whenNotANumber_marksTheDialogInvalidAndKeepsItOpen() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = mockDelegate()
            val viewModel = createViewModel(delegate = delegate, isValidPhoneNumber = false)
            viewModel.onAction(Action.PhoneNumberClicked)

            viewModel.onAction(Action.PhoneNumberConfirmed(phoneNumber = "DROP TABLE messages"))

            assertEquals(
                PhoneNumberDialogUiState(isVisible = true, isInvalid = true),
                viewModel.phoneNumberDialogState.value,
            )
            verify(exactly = 0) {
                delegate.onPhoneNumberChanged(any(), any())
            }
        }
    }

    @Test
    fun onPhoneNumberErrorDismissed_clearsTheRejection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(isValidPhoneNumber = false)
            viewModel.onAction(Action.PhoneNumberClicked)
            viewModel.onAction(Action.PhoneNumberConfirmed(phoneNumber = "DROP TABLE messages"))

            viewModel.onAction(Action.PhoneNumberErrorDismissed)

            assertEquals(
                PhoneNumberDialogUiState(isVisible = true, isInvalid = false),
                viewModel.phoneNumberDialogState.value,
            )
        }
    }

    @Test
    fun onPhoneNumberDialogDismissed_closesTheDialogAndForgetsTheRejection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(isValidPhoneNumber = false)
            viewModel.onAction(Action.PhoneNumberClicked)
            viewModel.onAction(Action.PhoneNumberConfirmed(phoneNumber = "DROP TABLE messages"))

            viewModel.onAction(Action.PhoneNumberDialogDismissed)

            assertEquals(PhoneNumberDialogUiState(), viewModel.phoneNumberDialogState.value)
        }
    }

    private fun createViewModel(
        delegate: SubscriptionSettingsDelegate = mockDelegate(),
        subId: Int = 1,
        isValidPhoneNumber: Boolean = true,
    ): SubscriptionSettingsViewModel {
        return SubscriptionSettingsViewModel(
            subscriptionSettingsDelegate = delegate,
            isValidSelfPhoneNumber = { _, _ -> isValidPhoneNumber },
            savedStateHandle = SavedStateHandle(
                mapOf(SUBSCRIPTION_SETTINGS_SUB_ID_ARG to subId),
            ),
        )
    }

    private fun mockDelegate(
        stateFlow: MutableStateFlow<SubscriptionSettingsUiState> =
            MutableStateFlow(SubscriptionSettingsUiState()),
    ): SubscriptionSettingsDelegate {
        val delegate = mockk<SubscriptionSettingsDelegate>(relaxed = true)
        every { delegate.state } returns stateFlow
        return delegate
    }
}
