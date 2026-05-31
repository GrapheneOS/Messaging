package com.android.messaging.ui.appsettings.subscription.delegate.subscriptionsettingsdelegate

import com.android.messaging.data.subscriptionsettings.model.SubscriptionBooleanPref
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionSettingsDelegatePreferenceTest : BaseSubscriptionSettingsDelegateTest() {

    @Test
    fun onAutoRetrieveMmsChanged_writesAutoRetrieveMmsPref() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val delegate = createBoundDelegate()

        delegate.onAutoRetrieveMmsChanged(subId = 5, enabled = true)
        runCurrent()

        coVerify(exactly = 1) {
            repository.setSubscriptionBooleanPref(
                subId = 5,
                pref = SubscriptionBooleanPref.AUTO_RETRIEVE_MMS,
                enabled = true,
            )
        }
    }

    @Test
    fun onAutoRetrieveMmsWhenRoamingChanged_writesRoamingPref() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val delegate = createBoundDelegate()

        delegate.onAutoRetrieveMmsWhenRoamingChanged(subId = 5, enabled = false)
        runCurrent()

        coVerify(exactly = 1) {
            repository.setSubscriptionBooleanPref(
                subId = 5,
                pref = SubscriptionBooleanPref.AUTO_RETRIEVE_MMS_WHEN_ROAMING,
                enabled = false,
            )
        }
    }

    @Test
    fun onDeliveryReportsChanged_writesDeliveryReportsPref() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val delegate = createBoundDelegate()

        delegate.onDeliveryReportsChanged(subId = 5, enabled = true)
        runCurrent()

        coVerify(exactly = 1) {
            repository.setSubscriptionBooleanPref(
                subId = 5,
                pref = SubscriptionBooleanPref.DELIVERY_REPORTS,
                enabled = true,
            )
        }
    }

    @Test
    fun onGroupMmsChanged_writesGroupMmsPref() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val delegate = createBoundDelegate()

        delegate.onGroupMmsChanged(subId = 5, enabled = false)
        runCurrent()

        coVerify(exactly = 1) {
            repository.setSubscriptionBooleanPref(
                subId = 5,
                pref = SubscriptionBooleanPref.GROUP_MMS,
                enabled = false,
            )
        }
    }

    @Test
    fun booleanPrefChange_writesPrefThenRefreshesState() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        givenSecondLoadProducesReloadedState()
        val delegate = createBoundDelegate()

        delegate.onGroupMmsChanged(subId = 5, enabled = true)
        runCurrent()

        coVerifyOrder {
            repository.setSubscriptionBooleanPref(
                subId = 5,
                pref = SubscriptionBooleanPref.GROUP_MMS,
                enabled = true,
            )
            repository.getSubscriptionSettings()
        }
        assertEquals(reloadedState, delegate.state.value)
    }

    @Test
    fun booleanPrefChange_beforeBind_doesNotWriteOrLoad() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val delegate = createDelegate()

        delegate.onGroupMmsChanged(subId = 5, enabled = true)
        runCurrent()

        coVerify(exactly = 0) {
            repository.setSubscriptionBooleanPref(any(), any(), any())
        }
        coVerify(exactly = 0) { repository.getSubscriptionSettings() }
    }

    @Test
    fun onPhoneNumberChanged_afterBind_setsNumberThenRefreshesState() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        givenSecondLoadProducesReloadedState()
        val delegate = createBoundDelegate()

        delegate.onPhoneNumberChanged(subId = 3, phoneNumber = "+15550001")
        runCurrent()

        coVerifyOrder {
            setSubscriptionPhoneNumber(subId = 3, phoneNumber = "+15550001")
            repository.getSubscriptionSettings()
        }
        assertEquals(reloadedState, delegate.state.value)
    }

    @Test
    fun onPhoneNumberChanged_beforeBind_doesNotSetNumber() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val delegate = createDelegate()

        delegate.onPhoneNumberChanged(subId = 3, phoneNumber = "+15550001")
        runCurrent()

        coVerify(exactly = 0) { setSubscriptionPhoneNumber(any(), any()) }
    }
}
