package com.android.messaging.ui.appsettings.general.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.common.test.helpers.targetContext
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
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

        val title = targetContext.getString(R.string.sms_disabled_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onAction(Action.DefaultSmsAppClicked(true))
            }
        }
    }

    @Test
    fun notificationsClick_delegatesToScreenModel() {
        setContent()

        val title = targetContext.getString(
            R.string.notifications_enabled_conversation_pref_title,
        )
        composeTestRule.onNodeWithText(title).performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onAction(Action.NotificationsClicked)
            }
        }
    }

    @Test
    fun sendSoundToggle_delegatesToScreenModel() {
        val appSettings = AppSettingsUiState(sendSoundEnabled = true)

        setContent(appSettings = appSettings)

        val title = targetContext.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onAction(Action.SendSoundChanged(false))
            }
        }
    }

    @Test
    fun debugSection_hiddenWhenDebugDisabled() {
        val appSettings = AppSettingsUiState(isDebugEnabled = false)

        setContent(appSettings = appSettings)

        val debugTitle = targetContext.getString(R.string.debug_category_pref_title)
        composeTestRule.onNodeWithText(debugTitle).assertDoesNotExist()

        val dumpSmsTitle = targetContext.getString(R.string.dump_sms_pref_title)
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

        val debugTitle = targetContext.getString(R.string.debug_category_pref_title)
        composeTestRule.onNodeWithText(debugTitle).assertIsDisplayed()

        val dumpSmsTitle = targetContext.getString(R.string.dump_sms_pref_title)
        composeTestRule.onNodeWithText(dumpSmsTitle).assertIsDisplayed()

        val dumpMmsTitle = targetContext.getString(R.string.dump_mms_pref_title)
        composeTestRule.onNodeWithText(dumpMmsTitle).assertIsDisplayed()
    }

    @Test
    fun dumpSmsToggle_delegatesToScreenModel() {
        val appSettings = AppSettingsUiState(
            isDebugEnabled = true,
            dumpSmsEnabled = false,
        )

        setContent(appSettings = appSettings)

        val title = targetContext.getString(R.string.dump_sms_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onAction(Action.DumpSmsChanged(true))
            }
        }
    }

    @Test
    fun dumpMmsToggle_delegatesToScreenModel() {
        val appSettings = AppSettingsUiState(
            isDebugEnabled = true,
            dumpMmsEnabled = false,
        )

        setContent(appSettings = appSettings)

        val title = targetContext.getString(R.string.dump_mms_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onAction(Action.DumpMmsChanged(true))
            }
        }
    }

    @Test
    fun licensesClick_delegatesToScreenModel() {
        setContent()

        val title = targetContext.getString(R.string.menu_license)
        composeTestRule.onNodeWithText(title).performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onAction(Action.LicensesClicked)
            }
        }
    }

    @Test
    fun advancedSettings_shownWhenTopLevel() {
        var advancedClicks = 0

        setContent(
            isTopLevel = true,
            onAdvancedClick = { advancedClicks += 1 },
        )

        val advancedTitle = targetContext.getString(R.string.advanced_settings)
        composeTestRule.onNodeWithText(advancedTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(advancedTitle).performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, advancedClicks)
        }
    }

    @Test
    fun advancedSettings_hiddenWhenNotTopLevel() {
        setContent(isTopLevel = false)

        val advancedTitle = targetContext.getString(R.string.advanced_settings)
        composeTestRule.onNodeWithText(advancedTitle).assertDoesNotExist()
    }

    private fun setContent(
        appSettings: AppSettingsUiState = AppSettingsUiState(),
        isTopLevel: Boolean = false,
        onAdvancedClick: (() -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            AppTheme {
                AppSettingsScreen(
                    appSettings = appSettings,
                    onAction = screenModel::onAction,
                    onNavigateBack = {},
                    isTopLevel = isTopLevel,
                    onAdvancedClick = onAdvancedClick,
                )
            }
        }
    }
}
