package com.android.messaging.ui.appsettings.general

import app.cash.turbine.test
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.appsettings.general.delegate.AppSettingsDelegate
import com.android.messaging.ui.appsettings.general.model.AppSettingsAction as Action
import com.android.messaging.ui.appsettings.general.model.AppSettingsNavEvent
import com.android.messaging.ui.appsettings.general.model.AppSettingsScreenEffect
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class AppSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_bindsAppSettingsDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = mockDelegate()

            createViewModel(delegate = delegate)

            verify(exactly = 1) {
                delegate.bind(any())
            }
        }
    }

    @Test
    fun uiState_exposesAppSettingsDelegateState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val state = AppSettingsUiState(
                isDefaultSmsApp = true,
                defaultSmsAppLabel = "Messaging",
            )
            val viewModel = createViewModel(delegate = mockDelegate(MutableStateFlow(state)))

            assertEquals(state, viewModel.uiState.value)
        }
    }

    @Test
    fun refreshState_refreshesAppSettingsDelegate() {
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
    fun onSendSoundChanged_delegatesToAppSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = mockDelegate()
            val viewModel = createViewModel(delegate = delegate)

            viewModel.onAction(Action.SendSoundChanged(enabled = false))

            verify(exactly = 1) {
                delegate.onSendSoundChanged(enabled = false)
            }
        }
    }

    @Test
    fun onDumpSmsChanged_delegatesToAppSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = mockDelegate()
            val viewModel = createViewModel(delegate = delegate)

            viewModel.onAction(Action.DumpSmsChanged(enabled = true))

            verify(exactly = 1) {
                delegate.onDumpSmsChanged(enabled = true)
            }
        }
    }

    @Test
    fun onDumpMmsChanged_delegatesToAppSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = mockDelegate()
            val viewModel = createViewModel(delegate = delegate)

            viewModel.onAction(Action.DumpMmsChanged(enabled = true))

            verify(exactly = 1) {
                delegate.onDumpMmsChanged(enabled = true)
            }
        }
    }

    @Test
    fun onDefaultSmsAppClick_whenDefault_emitsOpenManageDefaultApps() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onAction(Action.DefaultSmsAppClicked(isCurrentlyDefault = true))

                assertEquals(AppSettingsScreenEffect.OpenManageDefaultApps, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onDefaultSmsAppClick_whenNotDefault_emitsRequestDefaultSmsApp() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onAction(Action.DefaultSmsAppClicked(isCurrentlyDefault = false))

                assertEquals(AppSettingsScreenEffect.RequestDefaultSmsApp, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onNotificationsClick_emitsOpenNotificationSettings() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onAction(Action.NotificationsClicked)

                assertEquals(AppSettingsScreenEffect.OpenNotificationSettings, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onLicensesClick_emitsOpenLicensesNavEvent() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.navigationEvents.test {
                viewModel.onAction(Action.LicensesClicked)

                assertEquals(AppSettingsNavEvent.OpenLicenses, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private fun createViewModel(
        delegate: AppSettingsDelegate = mockDelegate(),
    ): AppSettingsViewModel {
        return AppSettingsViewModel(appSettingsDelegate = delegate)
    }

    private fun mockDelegate(
        stateFlow: MutableStateFlow<AppSettingsUiState> = MutableStateFlow(AppSettingsUiState()),
    ): AppSettingsDelegate {
        val delegate = mockk<AppSettingsDelegate>(relaxed = true)
        every { delegate.state } returns stateFlow
        return delegate
    }
}
