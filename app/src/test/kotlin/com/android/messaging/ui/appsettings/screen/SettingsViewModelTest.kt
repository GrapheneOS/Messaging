package com.android.messaging.ui.appsettings.screen

import app.cash.turbine.test
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.appsettings.general.delegate.AppSettingsDelegate
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.screen.model.SettingsAction as Action
import com.android.messaging.ui.appsettings.screen.model.SettingsScreenEffect
import com.android.messaging.ui.appsettings.screen.model.SettingsUiState
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsDelegate
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_bindsAllDelegates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appDelegate = mockAppSettingsDelegate()
            val subDelegate = mockSubscriptionSettingsDelegate()

            createViewModel(
                appSettingsDelegate = appDelegate,
                subscriptionSettingsDelegate = subDelegate,
            )
            advanceUntilIdle()

            verify(exactly = 1) {
                appDelegate.bind(any())
            }
            verify(exactly = 1) {
                subDelegate.bind(any())
            }
        }
    }

    @Test
    fun uiState_combinesDelegateStates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appStateFlow = MutableStateFlow(AppSettingsUiState())
            val subStateFlow = MutableStateFlow(SubscriptionSettingsUiState())
            val appDelegate = mockAppSettingsDelegate(appStateFlow)
            val subDelegate = mockSubscriptionSettingsDelegate(subStateFlow)
            val viewModel = createViewModel(
                appSettingsDelegate = appDelegate,
                subscriptionSettingsDelegate = subDelegate,
            )

            val appState = AppSettingsUiState(
                isDefaultSmsApp = true,
                defaultSmsAppLabel = "Messaging",
                sendSoundEnabled = false,
            )
            val subscription = SubscriptionUiState(
                subId = 1,
                displayName = "SIM 1",
            )

            appStateFlow.value = appState
            subStateFlow.value = SubscriptionSettingsUiState(
                subscriptions = persistentListOf(subscription),
                isMultiSim = false,
            )

            viewModel.uiState.test {
                assertEquals(SettingsUiState(), awaitItem())

                val mappedState = awaitItem()
                assertEquals(appState, mappedState.appSettings)
                assertEquals(persistentListOf(subscription), mappedState.subscriptionSettings)
                assertEquals(false, mappedState.isMultiSim)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun refreshState_refreshesBothDelegates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appDelegate = mockAppSettingsDelegate()
            val subDelegate = mockSubscriptionSettingsDelegate()
            val viewModel = createViewModel(
                appSettingsDelegate = appDelegate,
                subscriptionSettingsDelegate = subDelegate,
            )

            viewModel.refreshState()

            verify(exactly = 1) {
                appDelegate.refresh()
            }
            verify(exactly = 1) {
                subDelegate.refresh()
            }
        }
    }

    @Test
    fun onSendSoundChanged_delegatesToAppSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appDelegate = mockAppSettingsDelegate()
            val viewModel = createViewModel(appSettingsDelegate = appDelegate)

            viewModel.onAction(Action.SendSoundChanged(enabled = false))

            verify(exactly = 1) {
                appDelegate.onSendSoundChanged(enabled = false)
            }
        }
    }

    @Test
    fun onDumpSmsChanged_delegatesToAppSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appDelegate = mockAppSettingsDelegate()
            val viewModel = createViewModel(appSettingsDelegate = appDelegate)

            viewModel.onAction(Action.DumpSmsChanged(enabled = true))

            verify(exactly = 1) {
                appDelegate.onDumpSmsChanged(enabled = true)
            }
        }
    }

    @Test
    fun onDumpMmsChanged_delegatesToAppSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appDelegate = mockAppSettingsDelegate()
            val viewModel = createViewModel(appSettingsDelegate = appDelegate)

            viewModel.onAction(Action.DumpMmsChanged(enabled = true))

            verify(exactly = 1) {
                appDelegate.onDumpMmsChanged(enabled = true)
            }
        }
    }

    @Test
    fun onGroupMmsChanged_delegatesToSubscriptionSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val subDelegate = mockSubscriptionSettingsDelegate()
            val viewModel = createViewModel(subscriptionSettingsDelegate = subDelegate)

            viewModel.onAction(Action.GroupMmsChanged(subId = 1, enabled = false))

            verify(exactly = 1) {
                subDelegate.onGroupMmsChanged(subId = 1, enabled = false)
            }
        }
    }

    @Test
    fun onPhoneNumberChanged_delegatesToSubscriptionSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val subDelegate = mockSubscriptionSettingsDelegate()
            val viewModel = createViewModel(subscriptionSettingsDelegate = subDelegate)

            viewModel.onAction(Action.PhoneNumberChanged(subId = 1, phoneNumber = "+1555000111"))

            verify(exactly = 1) {
                subDelegate.onPhoneNumberChanged(subId = 1, phoneNumber = "+1555000111")
            }
        }
    }

    @Test
    fun onAutoRetrieveMmsChanged_delegatesToSubscriptionSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val subDelegate = mockSubscriptionSettingsDelegate()
            val viewModel = createViewModel(subscriptionSettingsDelegate = subDelegate)

            viewModel.onAction(Action.AutoRetrieveMmsChanged(subId = 2, enabled = true))

            verify(exactly = 1) {
                subDelegate.onAutoRetrieveMmsChanged(subId = 2, enabled = true)
            }
        }
    }

    @Test
    fun onAutoRetrieveMmsWhenRoamingChanged_delegatesToSubscriptionSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val subDelegate = mockSubscriptionSettingsDelegate()
            val viewModel = createViewModel(subscriptionSettingsDelegate = subDelegate)

            viewModel.onAction(Action.AutoRetrieveMmsWhenRoamingChanged(subId = 1, enabled = true))

            verify(exactly = 1) {
                subDelegate.onAutoRetrieveMmsWhenRoamingChanged(subId = 1, enabled = true)
            }
        }
    }

    @Test
    fun onDeliveryReportsChanged_delegatesToSubscriptionSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val subDelegate = mockSubscriptionSettingsDelegate()
            val viewModel = createViewModel(subscriptionSettingsDelegate = subDelegate)

            viewModel.onAction(Action.DeliveryReportsChanged(subId = 1, enabled = true))

            verify(exactly = 1) {
                subDelegate.onDeliveryReportsChanged(subId = 1, enabled = true)
            }
        }
    }

    @Test
    fun onDefaultSmsAppClick_whenDefault_emitsOpenManageDefaultApps() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onAction(Action.DefaultSmsAppClicked(isCurrentlyDefault = true))

                assertEquals(SettingsScreenEffect.OpenManageDefaultApps, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onDefaultSmsAppClick_whenNotDefault_emitsRequestDefaultSmsApp() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onAction(Action.DefaultSmsAppClicked(isCurrentlyDefault = false))

                assertEquals(SettingsScreenEffect.RequestDefaultSmsApp, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onNotificationsClick_emitsOpenNotificationSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onAction(Action.NotificationsClicked)

                assertEquals(SettingsScreenEffect.OpenNotificationSettings, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onWirelessAlertsClick_emitsOpenWirelessAlerts() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onAction(Action.WirelessAlertsClicked(subId = 1))

                assertEquals(SettingsScreenEffect.OpenWirelessAlerts(subId = 1), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onLicensesClick_emitsOpenLicenses() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onAction(Action.LicensesClicked)

                assertEquals(SettingsScreenEffect.OpenLicenses, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private fun createViewModel(
        appSettingsDelegate: AppSettingsDelegate = mockAppSettingsDelegate(),
        subscriptionSettingsDelegate: SubscriptionSettingsDelegate =
            mockSubscriptionSettingsDelegate(),
    ): SettingsViewModel {
        return SettingsViewModel(
            appSettingsDelegate = appSettingsDelegate,
            subscriptionSettingsDelegate = subscriptionSettingsDelegate,
        )
    }

    private fun mockAppSettingsDelegate(
        stateFlow: MutableStateFlow<AppSettingsUiState> = MutableStateFlow(AppSettingsUiState()),
    ): AppSettingsDelegate {
        val delegate = mockk<AppSettingsDelegate>(relaxed = true)
        every { delegate.state } returns stateFlow
        return delegate
    }

    private fun mockSubscriptionSettingsDelegate(
        stateFlow: MutableStateFlow<SubscriptionSettingsUiState> =
            MutableStateFlow(SubscriptionSettingsUiState()),
    ): SubscriptionSettingsDelegate {
        val delegate = mockk<SubscriptionSettingsDelegate>(relaxed = true)
        every { delegate.state } returns stateFlow
        return delegate
    }
}
