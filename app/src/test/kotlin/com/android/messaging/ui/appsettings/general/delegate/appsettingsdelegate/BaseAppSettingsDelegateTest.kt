package com.android.messaging.ui.appsettings.general.delegate.appsettingsdelegate

import com.android.messaging.data.appsettings.model.AppSettings
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.appsettings.general.delegate.AppSettingsDelegateImpl
import com.android.messaging.ui.appsettings.general.mapper.AppSettingsUiStateMapper
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseAppSettingsDelegateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected val repository = mockk<AppSettingsRepository>()
    protected val mapper = mockk<AppSettingsUiStateMapper>()

    protected val settingsData = mockk<AppSettings>()
    protected val reloadedSettingsData = mockk<AppSettings>()

    protected val loadedState = AppSettingsUiState(
        isDefaultSmsApp = true,
        defaultSmsAppLabel = "Messaging",
        sendSoundEnabled = false,
        isDebugEnabled = true,
        dumpSmsEnabled = false,
        dumpMmsEnabled = true,
    )
    protected val reloadedState = AppSettingsUiState(
        isDefaultSmsApp = false,
        defaultSmsAppLabel = "Other SMS",
        sendSoundEnabled = true,
        isDebugEnabled = false,
        dumpSmsEnabled = true,
        dumpMmsEnabled = false,
    )

    @Before
    fun setUpDefaultStubs() {
        coEvery { repository.getAppSettings() } returns settingsData
        every { mapper.map(appSettings = settingsData) } returns loadedState
        every { mapper.map(appSettings = reloadedSettingsData) } returns reloadedState
        coEvery {
            repository.setBooleanPref(
                pref = any(),
                enabled = any(),
            )
        } just Runs
    }

    protected fun createDelegate(): AppSettingsDelegateImpl {
        return AppSettingsDelegateImpl(
            repository = repository,
            mapper = mapper,
        )
    }

    protected fun TestScope.createBoundDelegate(): AppSettingsDelegateImpl {
        return createDelegate().also { delegate ->
            delegate.bind(scope = backgroundScope)
            runCurrent()
        }
    }

    protected fun givenSecondLoadProducesReloadedState() {
        coEvery { repository.getAppSettings() } returnsMany listOf(
            settingsData,
            reloadedSettingsData,
        )
    }
}
