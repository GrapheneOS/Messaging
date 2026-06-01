package com.android.messaging.ui.appsettings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.testutil.TestLifecycleOwner
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.screen.model.SettingsUiState
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionUiState
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
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeUiStateFlow = MutableStateFlow(createSingleSimState())
    private lateinit var screenModel: SettingsScreenModel
    private lateinit var effectHandler: SettingsEffectHandler

    @Before
    fun setup() {
        screenModel = mockk(relaxed = true)
        effectHandler = mockk(relaxed = true)
        every { screenModel.uiState } returns fakeUiStateFlow
    }

    @Test
    fun singleSim_skipsMainScreen_showsAppSettings() {
        fakeUiStateFlow.value = createSingleSimState()

        setContent()

        val generalTitle = targetContext.getString(R.string.settings_activity_title)
        composeTestRule.onNodeWithText(generalTitle).assertIsDisplayed()

        val sendSoundTitle = targetContext.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()
    }

    @Test
    fun multiSim_showsMainScreen_withSubscriptions() {
        fakeUiStateFlow.value = createMultiSimState()

        setContent()

        val settingsTitle = targetContext.getString(R.string.settings_activity_title)
        composeTestRule.onNodeWithText(settingsTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText("SIM 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("SIM 2").assertIsDisplayed()
    }

    @Test
    fun multiSim_generalSettingsClick_navigatesToAppSettings() {
        fakeUiStateFlow.value = createMultiSimState()

        setContent()

        val generalSettings = targetContext.getString(R.string.general_settings)
        composeTestRule.onNodeWithText(generalSettings).performClick()
        composeTestRule.waitForIdle()

        val sendSoundTitle = targetContext.getString(R.string.send_sound_pref_title)
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

        setContent(lifecycleOwner = lifecycleOwner)

        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = Lifecycle.State.RESUMED)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.refreshState()
            }
        }
    }

    @Test
    fun singleSim_showsAdvancedSettings() {
        fakeUiStateFlow.value = createSingleSimState()

        setContent()

        val advancedTitle = targetContext.getString(R.string.advanced_settings)
        composeTestRule.onNodeWithText(advancedTitle).assertIsDisplayed()
    }

    @Test
    fun singleSim_disablingLastSubscription_hidesAdvancedSettings() {
        fakeUiStateFlow.value = createSingleSimState()

        setContent()

        val advancedTitle = targetContext.getString(R.string.advanced_settings)
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

        setContent()

        val sendSoundTitle = targetContext.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()

        val advancedTitle = targetContext.getString(R.string.advanced_settings)
        composeTestRule.onNodeWithText(advancedTitle).assertDoesNotExist()
    }

    @Test
    fun multiSim_topLevelIntent_showsAppSettingsDirectly() {
        fakeUiStateFlow.value = createMultiSimState()

        setContent(isTopLevelIntent = true)

        val sendSoundTitle = targetContext.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText("SIM 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("SIM 2").assertDoesNotExist()
    }

    @Test
    fun multiSim_disablingOpenedSubscription_navigatesBackToMain() {
        fakeUiStateFlow.value = createMultiSimState()

        setContent(
            intentSubId = 2,
            intentSubTitle = "SIM 2",
        )

        val phoneNumberTitle = targetContext.getString(
            R.string.mms_phone_number_pref_title,
        )
        composeTestRule.onNodeWithText(phoneNumberTitle).assertIsDisplayed()

        fakeUiStateFlow.value = createMultiSimState().copy(
            subscriptionSettings = createMultiSimState().subscriptionSettings
                .filter { it.subId == 1 }
                .toImmutableList(),
        )
        composeTestRule.waitForIdle()

        val mainTitle = targetContext.getString(R.string.settings_activity_title)
        composeTestRule.onNodeWithText(mainTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText("SIM 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("SIM 2").assertDoesNotExist()
    }

    @Test
    fun singleSim_disablingOpenedSubscription_navigatesBackToAppSettings() {
        fakeUiStateFlow.value = createSingleSimState()

        setContent(
            intentSubId = 1,
            intentSubTitle = "Advanced Settings",
        )

        val phoneNumberTitle = targetContext.getString(
            R.string.mms_phone_number_pref_title,
        )
        composeTestRule.onNodeWithText(phoneNumberTitle).assertIsDisplayed()

        fakeUiStateFlow.value = createSingleSimState().copy(
            subscriptionSettings = persistentListOf(),
        )
        composeTestRule.waitForIdle()

        val sendSoundTitle = targetContext.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(phoneNumberTitle).assertDoesNotExist()
    }

    @Test
    fun multiSim_disablingAllSubscriptions_navigatesToAppSettings() {
        fakeUiStateFlow.value = createMultiSimState()

        setContent(
            intentSubId = 2,
            intentSubTitle = "SIM 2",
        )

        val phoneNumberTitle = targetContext.getString(
            R.string.mms_phone_number_pref_title,
        )
        composeTestRule.onNodeWithText(phoneNumberTitle).assertIsDisplayed()

        fakeUiStateFlow.value = createMultiSimState().copy(
            isMultiSim = false,
            subscriptionSettings = persistentListOf(),
        )
        composeTestRule.waitForIdle()

        val sendSoundTitle = targetContext.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(sendSoundTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(phoneNumberTitle).assertDoesNotExist()
    }

    @Test
    fun multiSim_disablingOtherSubscription_keepsCurrentSubscriptionScreen() {
        fakeUiStateFlow.value = createMultiSimState()

        setContent(
            intentSubId = 1,
            intentSubTitle = "SIM 1",
        )

        val phoneNumberTitle = targetContext.getString(
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

    private fun setContent(
        intentSubId: Int = 0,
        intentSubTitle: String? = null,
        isTopLevelIntent: Boolean = false,
        lifecycleOwner: LifecycleOwner? = null,
    ) {
        composeTestRule.setContent {
            val content = @Composable {
                AppTheme {
                    SettingsScreen(
                        effectHandler = effectHandler,
                        onNavigateBack = {},
                        intentSubId = intentSubId,
                        intentSubTitle = intentSubTitle,
                        isTopLevelIntent = isTopLevelIntent,
                        screenModel = screenModel,
                    )
                }
            }

            if (lifecycleOwner == null) {
                content()
            } else {
                CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                    content()
                }
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
                SubscriptionUiState(
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
                SubscriptionUiState(
                    subId = 1,
                    displayName = "SIM 1",
                    displayDetail = "+1234567890",
                ),
                SubscriptionUiState(
                    subId = 2,
                    displayName = "SIM 2",
                    displayDetail = "+0987654321",
                ),
            ),
            isMultiSim = true,
            areSubscriptionsLoaded = true,
        )
    }
}
