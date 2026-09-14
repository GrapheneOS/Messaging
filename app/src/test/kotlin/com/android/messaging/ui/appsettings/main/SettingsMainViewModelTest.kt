package com.android.messaging.ui.appsettings.main

import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsDelegate
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
class SettingsMainViewModelTest {

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
    fun uiState_exposesSubscriptionDelegateState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val state = SubscriptionSettingsUiState(
                isMultiSim = true,
                isLoaded = true,
                subscriptions = persistentListOf(
                    SubscriptionUiState(subId = SubId(1), displayName = "SIM 1"),
                ),
            )
            val viewModel = createViewModel(delegate = mockDelegate(MutableStateFlow(state)))

            assertEquals(state, viewModel.uiState.value)
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

    private fun createViewModel(
        delegate: SubscriptionSettingsDelegate = mockDelegate(),
    ): SettingsMainViewModel {
        return SettingsMainViewModel(subscriptionSettingsDelegate = delegate)
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
