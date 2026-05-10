package com.android.messaging.ui.appsettings.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.messaging.R
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.screen.model.SettingsUiState
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val fakeUiStateFlow = MutableStateFlow(createSingleSimState())
    private lateinit var screenModel: SettingsScreenModel

    @Before
    fun setup() {
        screenModel = mockk(relaxed = true)
        every { screenModel.uiState } returns fakeUiStateFlow
    }

    @Test
    fun singleSim_skipsMainScreen_showsAppSettings() {
        fakeUiStateFlow.value = createSingleSimState()

        setScreenContent()

        val generalTitle = composeTestRule.activity.getString(R.string.settings_activity_title)
        composeTestRule.onNodeWithText(generalTitle).assertIsDisplayed()

        val sendSoundTitle = composeTestRule.activity.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()
    }

    @Test
    fun multiSim_showsMainScreen_withSubscriptions() {
        fakeUiStateFlow.value = createMultiSimState()

        setScreenContent()

        val settingsTitle = composeTestRule.activity.getString(R.string.settings_activity_title)
        composeTestRule.onNodeWithText(settingsTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText("SIM 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("SIM 2").assertIsDisplayed()
    }

    @Test
    fun multiSim_generalSettingsClick_navigatesToAppSettings() {
        fakeUiStateFlow.value = createMultiSimState()

        setScreenContent()

        val generalSettings = composeTestRule.activity.getString(R.string.general_settings)
        composeTestRule.onNodeWithText(generalSettings).performClick()
        composeTestRule.waitForIdle()

        val sendSoundTitle = composeTestRule.activity.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()
    }

    @Test
    fun lifecycleResume_refreshesState() {
        fakeUiStateFlow.value = createSingleSimState()
        lateinit var lifecycleOwner: TestLifecycleOwner

        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(
                initialState = Lifecycle.State.STARTED,
            )
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                AppTheme {
                    SettingsScreen(
                        onNavigateBack = {},
                        screenModel = screenModel,
                    )
                }
            }
        }

        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = Lifecycle.State.RESUMED)
        }
        composeTestRule.waitForIdle()

        verify(atLeast = 1) {
            screenModel.refreshState()
        }
    }

    @Test
    fun singleSim_showsAdvancedSettings() {
        fakeUiStateFlow.value = createSingleSimState()

        setScreenContent()

        val advancedTitle = composeTestRule.activity.getString(R.string.advanced_settings)
        composeTestRule.onNodeWithText(advancedTitle).assertIsDisplayed()
    }

    @Test
    fun singleSim_disablingLastSubscription_hidesAdvancedSettings() {
        fakeUiStateFlow.value = createSingleSimState()

        setScreenContent()

        val advancedTitle = composeTestRule.activity.getString(R.string.advanced_settings)
        composeTestRule.onNodeWithText(advancedTitle).assertIsDisplayed()

        fakeUiStateFlow.value = createSingleSimState().copy(
            subscriptionSettings = persistentListOf(),
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(advancedTitle).assertDoesNotExist()
    }

    @Test
    fun noActiveSubscriptions_hidesAdvancedSettings() {
        fakeUiStateFlow.value = createSingleSimState().copy(
            subscriptionSettings = persistentListOf(),
        )

        setScreenContent()

        val sendSoundTitle = composeTestRule.activity.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()

        val advancedTitle = composeTestRule.activity.getString(R.string.advanced_settings)
        composeTestRule.onNodeWithText(advancedTitle).assertDoesNotExist()
    }

    @Test
    fun multiSim_topLevelIntent_showsAppSettingsDirectly() {
        fakeUiStateFlow.value = createMultiSimState()

        setScreenContent(isTopLevelIntent = true)

        val sendSoundTitle = composeTestRule.activity.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText("SIM 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("SIM 2").assertDoesNotExist()
    }

    @Test
    fun multiSim_disablingOpenedSubscription_navigatesBackToMain() {
        fakeUiStateFlow.value = createMultiSimState()

        setScreenContent(
            intentSubId = 2,
            intentSubTitle = "SIM 2",
        )

        val phoneNumberTitle = composeTestRule.activity.getString(
            R.string.mms_phone_number_pref_title,
        )
        composeTestRule.onNodeWithText(phoneNumberTitle).assertIsDisplayed()

        fakeUiStateFlow.value = createMultiSimState().copy(
            subscriptionSettings = createMultiSimState().subscriptionSettings
                .filter { it.subId == 1 }
                .toImmutableList(),
        )
        composeTestRule.waitForIdle()

        val mainTitle = composeTestRule.activity.getString(R.string.settings_activity_title)
        composeTestRule.onNodeWithText(mainTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText("SIM 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("SIM 2").assertDoesNotExist()
    }

    @Test
    fun singleSim_disablingOpenedSubscription_navigatesBackToAppSettings() {
        fakeUiStateFlow.value = createSingleSimState()

        setScreenContent(
            intentSubId = 1,
            intentSubTitle = "Advanced Settings",
        )

        val phoneNumberTitle = composeTestRule.activity.getString(
            R.string.mms_phone_number_pref_title,
        )
        composeTestRule.onNodeWithText(phoneNumberTitle).assertIsDisplayed()

        fakeUiStateFlow.value = createSingleSimState().copy(
            subscriptionSettings = persistentListOf(),
        )
        composeTestRule.waitForIdle()

        val sendSoundTitle = composeTestRule.activity.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(phoneNumberTitle).assertDoesNotExist()
    }

    @Test
    fun multiSim_disablingAllSubscriptions_navigatesToAppSettings() {
        fakeUiStateFlow.value = createMultiSimState()

        setScreenContent(
            intentSubId = 2,
            intentSubTitle = "SIM 2",
        )

        val phoneNumberTitle = composeTestRule.activity.getString(
            R.string.mms_phone_number_pref_title,
        )
        composeTestRule.onNodeWithText(phoneNumberTitle).assertIsDisplayed()

        fakeUiStateFlow.value = createMultiSimState().copy(
            isMultiSim = false,
            subscriptionSettings = persistentListOf(),
        )
        composeTestRule.waitForIdle()

        val sendSoundTitle = composeTestRule.activity.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(phoneNumberTitle).assertDoesNotExist()
    }

    @Test
    fun multiSim_disablingOtherSubscription_keepsCurrentSubscriptionScreen() {
        fakeUiStateFlow.value = createMultiSimState()

        setScreenContent(
            intentSubId = 1,
            intentSubTitle = "SIM 1",
        )

        val phoneNumberTitle = composeTestRule.activity.getString(
            R.string.mms_phone_number_pref_title,
        )
        composeTestRule.onNodeWithText(phoneNumberTitle).assertIsDisplayed()

        fakeUiStateFlow.value = createMultiSimState().copy(
            subscriptionSettings = createMultiSimState().subscriptionSettings
                .filter { it.subId == 1 }
                .toImmutableList(),
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(phoneNumberTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText("SIM 1").assertIsDisplayed()
    }

    private fun setScreenContent(
        intentSubId: Int = 0,
        intentSubTitle: String? = null,
        isTopLevelIntent: Boolean = false,
    ) {
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    intentSubId = intentSubId,
                    intentSubTitle = intentSubTitle,
                    isTopLevelIntent = isTopLevelIntent,
                    screenModel = screenModel,
                )
            }
        }
    }

    private fun createSingleSimState(): SettingsUiState {
        return SettingsUiState(
            appSettings = AppSettingsUiState(
                isDefaultSmsApp = true,
                defaultSmsAppLabel = "Messaging",
                sendSoundEnabled = true,
            ),
            subscriptionSettings = persistentListOf(
                SubscriptionSettingsUiState(
                    subId = 1,
                    displayName = "Advanced Settings",
                    displayDetail = "+1234567890",
                ),
            ),
            isMultiSim = false,
            areSubscriptionsLoaded = true,
        )
    }

    private fun createMultiSimState(): SettingsUiState {
        return SettingsUiState(
            appSettings = AppSettingsUiState(
                isDefaultSmsApp = true,
                defaultSmsAppLabel = "Messaging",
                sendSoundEnabled = true,
            ),
            subscriptionSettings = persistentListOf(
                SubscriptionSettingsUiState(
                    subId = 1,
                    displayName = "SIM 1",
                    displayDetail = "+1234567890",
                ),
                SubscriptionSettingsUiState(
                    subId = 2,
                    displayName = "SIM 2",
                    displayDetail = "+0987654321",
                ),
            ),
            isMultiSim = true,
            areSubscriptionsLoaded = true,
        )
    }

    private class TestLifecycleOwner(
        initialState: Lifecycle.State,
    ) : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)

        init {
            lifecycleRegistry.currentState = initialState
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        fun moveTo(state: Lifecycle.State) {
            lifecycleRegistry.currentState = state
        }
    }
}
