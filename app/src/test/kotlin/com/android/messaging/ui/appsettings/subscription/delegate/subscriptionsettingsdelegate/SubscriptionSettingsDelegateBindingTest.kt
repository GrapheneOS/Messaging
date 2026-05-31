package com.android.messaging.ui.appsettings.subscription.delegate.subscriptionsettingsdelegate

import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionSettingsDelegateBindingTest : BaseSubscriptionSettingsDelegateTest() {

    @Test
    fun initialState_reflectsMultiSimFromRepositoryAndIsNotLoaded() {
        every { repository.isMultiSim() } returns true

        val delegate = createDelegate()

        assertEquals(
            SubscriptionSettingsUiState(isMultiSim = true),
            delegate.state.value,
        )
    }

    @Test
    fun construction_readsMultiSimOnceAndDoesNotLoadSettings() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        createDelegate()
        runCurrent()

        verify(exactly = 1) { repository.isMultiSim() }
        coVerify(exactly = 0) { repository.getSubscriptionSettings() }
    }

    @Test
    fun bind_loadsSettingsAndMapsRepositoryDataIntoState() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val delegate = createBoundDelegate()

        assertEquals(loadedState, delegate.state.value)
        coVerify(exactly = 1) { repository.getSubscriptionSettings() }
        verify(exactly = 1) { mapper.map(settingsData) }
    }

    @Test
    fun bind_calledTwice_ignoresSecondBinding() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val delegate = createBoundDelegate()

        delegate.bind(backgroundScope)
        runCurrent()

        verify(exactly = 1) {
            @Suppress("UnusedFlow")
            repository.observeSubscriptionsChanged()
        }
        coVerify(exactly = 1) { repository.getSubscriptionSettings() }
    }

    @Test
    fun bind_whenSubscriptionsChange_reloadsAndRemapsState() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        givenSecondLoadProducesReloadedState()
        val subscriptionsChanged = givenSubscriptionsChangedSource()
        val delegate = createBoundDelegate()

        subscriptionsChanged.tryEmit(Unit)
        runCurrent()

        assertEquals(reloadedState, delegate.state.value)
        coVerify(exactly = 2) { repository.getSubscriptionSettings() }
    }

    @Test
    fun refresh_afterBind_reloadsAndRemapsState() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        givenSecondLoadProducesReloadedState()
        val delegate = createBoundDelegate()

        delegate.refresh()
        runCurrent()

        assertEquals(reloadedState, delegate.state.value)
        coVerify(exactly = 2) { repository.getSubscriptionSettings() }
    }

    @Test
    fun refresh_beforeBind_doesNotLoadSettings() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val delegate = createDelegate()

        delegate.refresh()
        runCurrent()

        coVerify(exactly = 0) { repository.getSubscriptionSettings() }
        assertEquals(SubscriptionSettingsUiState(isMultiSim = false), delegate.state.value)
    }
}
