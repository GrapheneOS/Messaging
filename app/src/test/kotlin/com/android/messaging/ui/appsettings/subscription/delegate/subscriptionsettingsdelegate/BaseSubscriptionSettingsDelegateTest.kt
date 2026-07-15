package com.android.messaging.ui.appsettings.subscription.delegate.subscriptionsettingsdelegate

import com.android.messaging.data.subscriptionsettings.model.SubscriptionSettingsData
import com.android.messaging.data.subscriptionsettings.repository.SubscriptionSettingsRepository
import com.android.messaging.domain.subscriptionsettings.usecase.SetSubscriptionPhoneNumber
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsDelegateImpl
import com.android.messaging.ui.appsettings.subscription.mapper.SubscriptionSettingsUiStateMapper
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseSubscriptionSettingsDelegateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected val repository = mockk<SubscriptionSettingsRepository>()
    protected val setSubscriptionPhoneNumber = mockk<SetSubscriptionPhoneNumber>()
    protected val mapper = mockk<SubscriptionSettingsUiStateMapper>()

    protected val settingsData = mockk<SubscriptionSettingsData>()
    protected val reloadedSettingsData = mockk<SubscriptionSettingsData>()

    protected val loadedState = SubscriptionSettingsUiState(
        isMultiSim = false,
        isLoaded = true,
        subscriptions = persistentListOf(),
    )
    protected val reloadedState = SubscriptionSettingsUiState(
        isMultiSim = true,
        isLoaded = true,
        subscriptions = persistentListOf(),
    )

    @Before
    fun setUpDefaultStubs() {
        every { repository.isMultiSim() } returns false
        every { repository.observeSubscriptionsChanged() } returns emptyFlow()
        coEvery { repository.getSubscriptionSettings() } returns settingsData
        every { mapper.map(settingsData) } returns loadedState
        every { mapper.map(reloadedSettingsData) } returns reloadedState
        coEvery { repository.setSubscriptionBooleanPref(any(), any(), any()) } just Runs
        coEvery { setSubscriptionPhoneNumber(any(), any()) } just Runs
    }

    protected fun createDelegate(): SubscriptionSettingsDelegateImpl {
        return SubscriptionSettingsDelegateImpl(
            repository = repository,
            setSubscriptionPhoneNumber = setSubscriptionPhoneNumber,
            mapper = mapper,
        )
    }

    protected fun TestScope.createBoundDelegate(): SubscriptionSettingsDelegateImpl {
        return createDelegate().also { delegate ->
            delegate.bind(backgroundScope)
            runCurrent()
        }
    }

    protected fun givenSecondLoadProducesReloadedState() {
        coEvery {
            repository.getSubscriptionSettings()
        } returnsMany listOf(settingsData, reloadedSettingsData)
    }

    protected fun givenSubscriptionsChangedSource(): MutableSharedFlow<Unit> {
        val source = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        every { repository.observeSubscriptionsChanged() } returns source
        return source
    }
}
