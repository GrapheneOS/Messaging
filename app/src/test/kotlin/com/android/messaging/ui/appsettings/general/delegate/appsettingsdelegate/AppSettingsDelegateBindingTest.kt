package com.android.messaging.ui.appsettings.general.delegate.appsettingsdelegate

import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AppSettingsDelegateBindingTest : BaseAppSettingsDelegateTest() {

    @Test
    fun initialState_isDefaultAndDoesNotLoadSettings() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createDelegate()
            runCurrent()

            assertEquals(AppSettingsUiState(), delegate.state.value)
            coVerify(exactly = 0) { repository.getAppSettings() }
            verify(exactly = 0) { mapper.map(appSettings = any()) }
        }
    }

    @Test
    fun bind_loadsSettingsAndMapsRepositoryDataIntoState() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createBoundDelegate()

            assertEquals(loadedState, delegate.state.value)
            coVerify(exactly = 1) { repository.getAppSettings() }
            verify(exactly = 1) { mapper.map(appSettings = settingsData) }
        }
    }

    @Test
    fun bind_calledTwice_ignoresSecondBinding() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createBoundDelegate()

            delegate.bind(scope = backgroundScope)
            runCurrent()

            coVerify(exactly = 1) { repository.getAppSettings() }
            verify(exactly = 1) { mapper.map(appSettings = settingsData) }
        }
    }

    @Test
    fun refresh_afterBind_reloadsAndRemapsState() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            givenSecondLoadProducesReloadedState()
            val delegate = createBoundDelegate()

            delegate.refresh()
            runCurrent()

            assertEquals(reloadedState, delegate.state.value)
            coVerify(exactly = 2) { repository.getAppSettings() }
            verify(exactly = 1) { mapper.map(appSettings = reloadedSettingsData) }
        }
    }

    @Test
    fun refresh_beforeBind_doesNotLoadSettings() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createDelegate()

            delegate.refresh()
            runCurrent()

            assertEquals(AppSettingsUiState(), delegate.state.value)
            coVerify(exactly = 0) { repository.getAppSettings() }
            verify(exactly = 0) { mapper.map(appSettings = any()) }
        }
    }
}
