package com.android.messaging.ui.appsettings.general.delegate.appsettingsdelegate

import com.android.messaging.data.appsettings.model.AppBooleanPref
import com.android.messaging.data.appsettings.model.ConversationSwipeOption
import com.android.messaging.data.appsettings.model.ConversationSwipePref
import com.android.messaging.data.appsettings.model.LinkOpeningMode
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
    fun onLinkOpeningModeChanged_writesModeThenRefreshesState() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            givenSecondLoadProducesReloadedState()
            val delegate = createBoundDelegate()

            delegate.onLinkOpeningModeChanged(mode = LinkOpeningMode.DoNotOpen)
            runCurrent()

            coVerifyOrder {
                repository.setLinkOpeningMode(mode = LinkOpeningMode.DoNotOpen)
                repository.getAppSettings()
            }
            assertEquals(reloadedState, delegate.state.value)
        }
    }

    @Test
    fun onConversationSwipeStartToEndOptionChanged_writesStartToEndPref() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createBoundDelegate()

            delegate.onConversationSwipeStartToEndOptionChanged(
                option = ConversationSwipeOption.Delete,
            )
            runCurrent()

            coVerify(exactly = 1) {
                repository.setConversationSwipeOption(
                    pref = ConversationSwipePref.START_TO_END,
                    option = ConversationSwipeOption.Delete,
                )
            }
        }
    }

    @Test
    fun onConversationSwipeEndToStartOptionChanged_writesEndToStartPref() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createBoundDelegate()

            delegate.onConversationSwipeEndToStartOptionChanged(
                option = ConversationSwipeOption.ToggleRead,
            )
            runCurrent()

            coVerify(exactly = 1) {
                repository.setConversationSwipeOption(
                    pref = ConversationSwipePref.END_TO_START,
                    option = ConversationSwipeOption.ToggleRead,
                )
            }
        }
    }

    @Test
    fun conversationSwipeOptionChange_writesPrefThenRefreshesState() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            givenSecondLoadProducesReloadedState()
            val delegate = createBoundDelegate()

            delegate.onConversationSwipeEndToStartOptionChanged(
                option = ConversationSwipeOption.Delete,
            )
            runCurrent()

            coVerifyOrder {
                repository.setConversationSwipeOption(
                    pref = ConversationSwipePref.END_TO_START,
                    option = ConversationSwipeOption.Delete,
                )
                repository.getAppSettings()
            }
            assertEquals(reloadedState, delegate.state.value)
        }
    }

    @Test
    fun conversationSwipeOptionChange_beforeBind_doesNotWriteOrLoad() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val delegate = createDelegate()

            delegate.onConversationSwipeStartToEndOptionChanged(
                option = ConversationSwipeOption.Archive,
            )
            delegate.onConversationSwipeEndToStartOptionChanged(
                option = ConversationSwipeOption.Archive,
            )
            runCurrent()

            coVerify(exactly = 0) {
                repository.setConversationSwipeOption(
                    pref = any(),
                    option = any(),
                )
            }
            coVerify(exactly = 0) { repository.getAppSettings() }
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
            delegate.onLinkOpeningModeChanged(mode = LinkOpeningMode.OpenOnTap)
            runCurrent()

            coVerify(exactly = 0) {
                repository.setBooleanPref(
                    pref = any(),
                    enabled = any(),
                )
            }
            coVerify(exactly = 0) { repository.setLinkOpeningMode(mode = any()) }
            coVerify(exactly = 0) { repository.getAppSettings() }
        }
    }
}
