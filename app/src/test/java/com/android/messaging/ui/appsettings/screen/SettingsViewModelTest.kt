package com.android.messaging.ui.appsettings.screen

import app.cash.turbine.test
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.appsettings.general.delegate.AppSettingsDelegate
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.screen.model.SettingsAction as Action
import com.android.messaging.ui.appsettings.screen.model.SettingsScreenEffect
import com.android.messaging.ui.appsettings.screen.model.SettingsUiState
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsDelegate
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsState
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
            val appDelegate = FakeAppSettingsDelegate()
            val subDelegate = FakeSubscriptionSettingsDelegate()

            createViewModel(
                appSettingsDelegate = appDelegate,
                subscriptionSettingsDelegate = subDelegate,
            )
            advanceUntilIdle()

            assertEquals(1, appDelegate.bindCalls)
            assertEquals(1, subDelegate.bindCalls)
        }
    }

    @Test
    fun uiState_combinesDelegateStates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appDelegate = FakeAppSettingsDelegate()
            val subDelegate = FakeSubscriptionSettingsDelegate()
            val viewModel = createViewModel(
                appSettingsDelegate = appDelegate,
                subscriptionSettingsDelegate = subDelegate,
            )

            val appState = AppSettingsUiState(
                isDefaultSmsApp = true,
                defaultSmsAppLabel = "Messaging",
                sendSoundEnabled = false,
            )
            val subscription = SubscriptionSettingsUiState(
                subId = 1,
                displayName = "SIM 1",
            )
            appDelegate.stateFlow.value = appState
            subDelegate.stateFlow.value = SubscriptionSettingsState(
                subscriptions = listOf(subscription),
                isMultiSim = false,
            )

            viewModel.uiState.test {
                assertEquals(SettingsUiState(), awaitItem())

                val mappedState = awaitItem()
                assertEquals(appState, mappedState.appSettings)
                assertEquals(listOf(subscription), mappedState.subscriptionSettings)
                assertEquals(false, mappedState.isMultiSim)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun refreshState_refreshesBothDelegates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appDelegate = FakeAppSettingsDelegate()
            val subDelegate = FakeSubscriptionSettingsDelegate()
            val viewModel = createViewModel(
                appSettingsDelegate = appDelegate,
                subscriptionSettingsDelegate = subDelegate,
            )

            viewModel.refreshState()

            assertEquals(1, appDelegate.refreshCalls)
            assertEquals(1, subDelegate.refreshCalls)
        }
    }

    @Test
    fun onSendSoundChanged_delegatesToAppSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appDelegate = FakeAppSettingsDelegate()
            val viewModel = createViewModel(appSettingsDelegate = appDelegate)

            viewModel.onAction(Action.SendSoundChanged(enabled = false))

            assertEquals(listOf(false), appDelegate.sendSoundChanges)
        }
    }

    @Test
    fun onDumpSmsChanged_delegatesToAppSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appDelegate = FakeAppSettingsDelegate()
            val viewModel = createViewModel(appSettingsDelegate = appDelegate)

            viewModel.onAction(Action.DumpSmsChanged(enabled = true))

            assertEquals(listOf(true), appDelegate.dumpSmsChanges)
        }
    }

    @Test
    fun onDumpMmsChanged_delegatesToAppSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val appDelegate = FakeAppSettingsDelegate()
            val viewModel = createViewModel(appSettingsDelegate = appDelegate)

            viewModel.onAction(Action.DumpMmsChanged(enabled = true))

            assertEquals(listOf(true), appDelegate.dumpMmsChanges)
        }
    }

    @Test
    fun onGroupMmsChanged_delegatesToSubscriptionSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val subDelegate = FakeSubscriptionSettingsDelegate()
            val viewModel = createViewModel(subscriptionSettingsDelegate = subDelegate)

            viewModel.onAction(Action.GroupMmsChanged(subId = 1, enabled = false))

            assertEquals(listOf(1 to false), subDelegate.groupMmsChanges)
        }
    }

    @Test
    fun onPhoneNumberChanged_delegatesToSubscriptionSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val subDelegate = FakeSubscriptionSettingsDelegate()
            val viewModel = createViewModel(subscriptionSettingsDelegate = subDelegate)

            viewModel.onAction(Action.PhoneNumberChanged(subId = 1, phoneNumber = "+1555000111"))

            assertEquals(listOf(1 to "+1555000111"), subDelegate.phoneNumberChanges)
        }
    }

    @Test
    fun onAutoRetrieveMmsChanged_delegatesToSubscriptionSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val subDelegate = FakeSubscriptionSettingsDelegate()
            val viewModel = createViewModel(subscriptionSettingsDelegate = subDelegate)

            viewModel.onAction(Action.AutoRetrieveMmsChanged(subId = 2, enabled = true))

            assertEquals(listOf(2 to true), subDelegate.autoRetrieveMmsChanges)
        }
    }

    @Test
    fun onAutoRetrieveMmsWhenRoamingChanged_delegatesToSubscriptionSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val subDelegate = FakeSubscriptionSettingsDelegate()
            val viewModel = createViewModel(subscriptionSettingsDelegate = subDelegate)

            viewModel.onAction(Action.AutoRetrieveMmsWhenRoamingChanged(subId = 1, enabled = true))

            assertEquals(listOf(1 to true), subDelegate.autoRetrieveMmsWhenRoamingChanges)
        }
    }

    @Test
    fun onDeliveryReportsChanged_delegatesToSubscriptionSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val subDelegate = FakeSubscriptionSettingsDelegate()
            val viewModel = createViewModel(subscriptionSettingsDelegate = subDelegate)

            viewModel.onAction(Action.DeliveryReportsChanged(subId = 1, enabled = true))

            assertEquals(listOf(1 to true), subDelegate.deliveryReportsChanges)
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
        appSettingsDelegate: AppSettingsDelegate = FakeAppSettingsDelegate(),
        subscriptionSettingsDelegate: SubscriptionSettingsDelegate =
            FakeSubscriptionSettingsDelegate(),
    ): SettingsViewModel {
        return SettingsViewModel(
            appSettingsDelegate = appSettingsDelegate,
            subscriptionSettingsDelegate = subscriptionSettingsDelegate,
        )
    }

    private class FakeAppSettingsDelegate : AppSettingsDelegate {
        val stateFlow = MutableStateFlow(AppSettingsUiState())
        override val state: StateFlow<AppSettingsUiState> = stateFlow

        var bindCalls = 0
        var refreshCalls = 0
        val sendSoundChanges = mutableListOf<Boolean>()
        val dumpSmsChanges = mutableListOf<Boolean>()
        val dumpMmsChanges = mutableListOf<Boolean>()

        override fun bind(scope: CoroutineScope) {
            bindCalls += 1
        }

        override fun refresh() {
            refreshCalls += 1
        }

        override fun onSendSoundChanged(enabled: Boolean) {
            sendSoundChanges += enabled
        }

        override fun onDumpSmsChanged(enabled: Boolean) {
            dumpSmsChanges += enabled
        }

        override fun onDumpMmsChanged(enabled: Boolean) {
            dumpMmsChanges += enabled
        }
    }

    private class FakeSubscriptionSettingsDelegate : SubscriptionSettingsDelegate {
        val stateFlow = MutableStateFlow(SubscriptionSettingsState())
        override val state: StateFlow<SubscriptionSettingsState> = stateFlow

        var bindCalls = 0
        var refreshCalls = 0
        val groupMmsChanges = mutableListOf<Pair<Int, Boolean>>()
        val phoneNumberChanges = mutableListOf<Pair<Int, String>>()
        val autoRetrieveMmsChanges = mutableListOf<Pair<Int, Boolean>>()
        val autoRetrieveMmsWhenRoamingChanges = mutableListOf<Pair<Int, Boolean>>()
        val deliveryReportsChanges = mutableListOf<Pair<Int, Boolean>>()

        override fun bind(scope: CoroutineScope) {
            bindCalls += 1
        }

        override fun refresh() {
            refreshCalls += 1
        }

        override fun onGroupMmsChanged(subId: Int, enabled: Boolean) {
            groupMmsChanges += subId to enabled
        }

        override fun onPhoneNumberChanged(subId: Int, phoneNumber: String) {
            phoneNumberChanges += subId to phoneNumber
        }

        override fun onAutoRetrieveMmsChanged(subId: Int, enabled: Boolean) {
            autoRetrieveMmsChanges += subId to enabled
        }

        override fun onAutoRetrieveMmsWhenRoamingChanged(subId: Int, enabled: Boolean) {
            autoRetrieveMmsWhenRoamingChanges += subId to enabled
        }

        override fun onDeliveryReportsChanged(subId: Int, enabled: Boolean) {
            deliveryReportsChanges += subId to enabled
        }
    }
}
