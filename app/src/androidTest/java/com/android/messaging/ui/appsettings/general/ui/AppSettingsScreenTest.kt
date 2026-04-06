package com.android.messaging.ui.appsettings.general.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.screen.SettingsScreenModel
import com.android.messaging.ui.appsettings.screen.model.SettingsAction as Action
import com.android.messaging.ui.core.AppTheme
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppSettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var screenModel: SettingsScreenModel

    @Before
    fun setup() {
        screenModel = mockk(relaxed = true)
    }

    @Test
    fun defaultSmsAppItem_displaysLabel() {
        val appSettings = AppSettingsUiState(
            isDefaultSmsApp = true,
            defaultSmsAppLabel = "Messaging",
        )

        setContent(appSettings = appSettings)

        composeTestRule.onNodeWithText("Messaging").assertIsDisplayed()
    }

    @Test
    fun defaultSmsAppClick_delegatesToScreenModel() {
        val appSettings = AppSettingsUiState(
            isDefaultSmsApp = true,
            defaultSmsAppLabel = "Messaging",
        )

        setContent(appSettings = appSettings)

        val title = composeTestRule.activity.getString(R.string.sms_disabled_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.DefaultSmsAppClicked(true))
        }
    }

    @Test
    fun notificationsClick_delegatesToScreenModel() {
        setContent()

        val title = composeTestRule.activity.getString(
            R.string.notifications_enabled_conversation_pref_title,
        )
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.NotificationsClicked)
        }
    }

    @Test
    fun sendSoundToggle_delegatesToScreenModel() {
        val appSettings = AppSettingsUiState(sendSoundEnabled = true)

        setContent(appSettings = appSettings)

        val title = composeTestRule.activity.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.SendSoundChanged(false))
        }
    }

    @Test
    fun debugSection_hiddenWhenDebugDisabled() {
        val appSettings = AppSettingsUiState(isDebugEnabled = false)

        setContent(appSettings = appSettings)

        val debugTitle = composeTestRule.activity.getString(R.string.debug_category_pref_title)
        composeTestRule.onNodeWithText(debugTitle).assertDoesNotExist()

        val dumpSmsTitle = composeTestRule.activity.getString(R.string.dump_sms_pref_title)
        composeTestRule.onNodeWithText(dumpSmsTitle).assertDoesNotExist()
    }

    @Test
    fun debugSection_shownWhenDebugEnabled() {
        val appSettings = AppSettingsUiState(
            isDebugEnabled = true,
            dumpSmsEnabled = false,
            dumpMmsEnabled = false,
        )

        setContent(appSettings = appSettings)

        val debugTitle = composeTestRule.activity.getString(R.string.debug_category_pref_title)
        composeTestRule.onNodeWithText(debugTitle).assertIsDisplayed()

        val dumpSmsTitle = composeTestRule.activity.getString(R.string.dump_sms_pref_title)
        composeTestRule.onNodeWithText(dumpSmsTitle).assertIsDisplayed()

        val dumpMmsTitle = composeTestRule.activity.getString(R.string.dump_mms_pref_title)
        composeTestRule.onNodeWithText(dumpMmsTitle).assertIsDisplayed()
    }

    @Test
    fun dumpSmsToggle_delegatesToScreenModel() {
        val appSettings = AppSettingsUiState(
            isDebugEnabled = true,
            dumpSmsEnabled = false,
        )

        setContent(appSettings = appSettings)

        val title = composeTestRule.activity.getString(R.string.dump_sms_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.DumpSmsChanged(true))
        }
    }

    @Test
    fun dumpMmsToggle_delegatesToScreenModel() {
        val appSettings = AppSettingsUiState(
            isDebugEnabled = true,
            dumpMmsEnabled = false,
        )

        setContent(appSettings = appSettings)

        val title = composeTestRule.activity.getString(R.string.dump_mms_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.DumpMmsChanged(true))
        }
    }

    @Test
    fun licensesClick_delegatesToScreenModel() {
        setContent()

        val title = composeTestRule.activity.getString(R.string.menu_license)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.LicensesClicked)
        }
    }

    @Test
    fun advancedSettings_shownWhenTopLevel() {
        var advancedClicks = 0

        composeTestRule.setContent {
            AppTheme {
                AppSettingsScreen(
                    appSettings = AppSettingsUiState(),
                    screenModel = screenModel,
                    onNavigateBack = {},
                    isTopLevel = true,
                    onAdvancedClick = { advancedClicks += 1 },
                )
            }
        }

        val advancedTitle = composeTestRule.activity.getString(R.string.advanced_settings)
        composeTestRule.onNodeWithText(advancedTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(advancedTitle).performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, advancedClicks)
        }
    }

    @Test
    fun advancedSettings_hiddenWhenNotTopLevel() {
        composeTestRule.setContent {
            AppTheme {
                AppSettingsScreen(
                    appSettings = AppSettingsUiState(),
                    screenModel = screenModel,
                    onNavigateBack = {},
                    isTopLevel = false,
                    onAdvancedClick = null,
                )
            }
        }

        val advancedTitle = composeTestRule.activity.getString(R.string.advanced_settings)
        composeTestRule.onNodeWithText(advancedTitle).assertDoesNotExist()
    }

    private fun setContent(
        appSettings: AppSettingsUiState = AppSettingsUiState(),
    ) {
        composeTestRule.setContent {
            AppTheme {
                AppSettingsScreen(
                    appSettings = appSettings,
                    screenModel = screenModel,
                    onNavigateBack = {},
                )
            }
        }
    }
}
