package com.android.messaging.ui.appsettings.general.delegate.appsettingsdelegate

import com.android.messaging.data.appsettings.model.AppBooleanPref
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AppSettingsDelegatePreferenceTest : BaseAppSettingsDelegateTest() {

    @Test
    fun onSendSoundChanged_writesSendSoundPref() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createBoundDelegate()

            delegate.onSendSoundChanged(enabled = false)
            runCurrent()

            coVerify(exactly = 1) {
                repository.setBooleanPref(
                    pref = AppBooleanPref.SEND_SOUND,
                    enabled = false,
                )
            }
        }
    }

    @Test
    fun onDumpSmsChanged_writesDumpSmsPref() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createBoundDelegate()

            delegate.onDumpSmsChanged(enabled = true)
            runCurrent()

            coVerify(exactly = 1) {
                repository.setBooleanPref(
                    pref = AppBooleanPref.DUMP_SMS,
                    enabled = true,
                )
            }
        }
    }

    @Test
    fun onDumpMmsChanged_writesDumpMmsPref() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createBoundDelegate()

            delegate.onDumpMmsChanged(enabled = false)
            runCurrent()

            coVerify(exactly = 1) {
                repository.setBooleanPref(
                    pref = AppBooleanPref.DUMP_MMS,
                    enabled = false,
                )
            }
        }
    }

    @Test
    fun booleanPrefChange_writesPrefThenRefreshesState() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            givenSecondLoadProducesReloadedState()
            val delegate = createBoundDelegate()

            delegate.onDumpSmsChanged(enabled = true)
            runCurrent()

            coVerifyOrder {
                repository.setBooleanPref(
                    pref = AppBooleanPref.DUMP_SMS,
                    enabled = true,
                )
                repository.getAppSettings()
            }
            assertEquals(reloadedState, delegate.state.value)
        }
    }

    @Test
    fun booleanPrefChange_beforeBind_doesNotWriteOrLoad() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createDelegate()

            delegate.onSendSoundChanged(enabled = false)
            delegate.onDumpSmsChanged(enabled = true)
            delegate.onDumpMmsChanged(enabled = true)
            runCurrent()

            coVerify(exactly = 0) {
                repository.setBooleanPref(
                    pref = any(),
                    enabled = any(),
                )
            }
            coVerify(exactly = 0) { repository.getAppSettings() }
        }
    }
}
