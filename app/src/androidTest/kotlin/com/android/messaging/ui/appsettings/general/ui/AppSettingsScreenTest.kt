package com.android.messaging.ui.appsettings.general.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.ConversationSwipeOption
import com.android.messaging.data.appsettings.model.ConversationSwipeSettings
import com.android.messaging.ui.appsettings.general.model.AppSettingsAction as Action
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
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

    private lateinit var onAction: (Action) -> Unit

    @Before
    fun setup() {
        onAction = mockk(relaxed = true)
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
            onAction(Action.DefaultSmsAppClicked(true))
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
            onAction(Action.NotificationsClicked)
        }
    }

    @Test
    fun sendSoundToggle_delegatesToScreenModel() {
        val appSettings = AppSettingsUiState(sendSoundEnabled = true)

        setContent(appSettings = appSettings)

        val title = composeTestRule.activity.getString(R.string.send_sound_pref_title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            onAction(Action.SendSoundChanged(false))
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
        scrollToText(text = debugTitle)
        composeTestRule.onNodeWithText(debugTitle).assertIsDisplayed()

        val dumpSmsTitle = composeTestRule.activity.getString(R.string.dump_sms_pref_title)
        scrollToText(text = dumpSmsTitle)
        composeTestRule.onNodeWithText(dumpSmsTitle).assertIsDisplayed()

        val dumpMmsTitle = composeTestRule.activity.getString(R.string.dump_mms_pref_title)
        scrollToText(text = dumpMmsTitle)
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
        scrollToText(text = title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            onAction(Action.DumpSmsChanged(true))
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
        scrollToText(text = title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            onAction(Action.DumpMmsChanged(true))
        }
    }

    @Test
    fun swipeSection_showsSelectedOptionForEachDirection() {
        val appSettings = AppSettingsUiState(
            conversationSwipeSettings = ConversationSwipeSettings(
                startToEnd = ConversationSwipeOption.Delete,
                endToStart = ConversationSwipeOption.ToggleRead,
            ),
        )

        setContent(appSettings = appSettings)

        val categoryTitle = composeTestRule.activity.getString(R.string.swipe_category_pref_title)
        scrollToText(text = categoryTitle)
        composeTestRule.onNodeWithText(categoryTitle).assertIsDisplayed()

        val startToEndTitle = composeTestRule.activity.getString(
            R.string.swipe_start_to_end_pref_title,
        )
        scrollToText(text = startToEndTitle)
        composeTestRule
            .onNode(hasText(startToEndTitle) and hasText(string(R.string.action_delete)))
            .assertIsDisplayed()

        val endToStartTitle = composeTestRule.activity.getString(
            R.string.swipe_end_to_start_pref_title,
        )
        scrollToText(text = endToStartTitle)
        composeTestRule
            .onNode(
                hasText(endToStartTitle) and hasText(string(R.string.swipe_action_toggle_read)),
            )
            .assertIsDisplayed()
    }

    @Test
    fun swipeStartToEndSelection_delegatesToScreenModelAndClosesDialog() {
        setContent()

        val title = composeTestRule.activity.getString(R.string.swipe_start_to_end_pref_title)
        scrollToText(text = title)
        composeTestRule.onNodeWithText(title).performClick()

        composeTestRule.onNode(isDialog()).assertIsDisplayed()
        composeTestRule
            .onNode(isSelectable() and hasText(string(R.string.swipe_action_toggle_read)))
            .assertIsSelected()
        composeTestRule
            .onNode(isSelectable() and hasText(string(R.string.action_delete)))
            .performClick()

        verify(exactly = 1) {
            onAction(
                Action.ConversationSwipeStartToEndOptionChanged(ConversationSwipeOption.Delete),
            )
        }
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun swipeEndToStartSelection_delegatesToScreenModel() {
        setContent()

        val title = composeTestRule.activity.getString(R.string.swipe_end_to_start_pref_title)
        scrollToText(text = title)
        composeTestRule.onNodeWithText(title).performClick()

        composeTestRule
            .onNode(isSelectable() and hasText(string(R.string.swipe_action_toggle_read)))
            .performClick()

        verify(exactly = 1) {
            onAction(
                Action.ConversationSwipeEndToStartOptionChanged(ConversationSwipeOption.ToggleRead),
            )
        }
    }

    @Test
    fun swipeOptionDialog_cancelClosesWithoutChangingOption() {
        setContent()

        val title = composeTestRule.activity.getString(R.string.swipe_end_to_start_pref_title)
        scrollToText(text = title)
        composeTestRule.onNodeWithText(title).performClick()
        composeTestRule.onNodeWithText(string(android.R.string.cancel)).performClick()

        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        verify(exactly = 0) {
            onAction(any<Action.ConversationSwipeEndToStartOptionChanged>())
        }
    }

    @Test
    fun licensesClick_delegatesToScreenModel() {
        setContent()

        val title = composeTestRule.activity.getString(R.string.menu_license)
        scrollToText(text = title)
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            onAction(Action.LicensesClicked)
        }
    }

    @Test
    fun privacySettings_clickDelegatesToCallback() {
        var privacyClicks = 0

        composeTestRule.setContent {
            AppTheme {
                AppSettingsScreen(
                    appSettings = AppSettingsUiState(),
                    onAction = onAction,
                    onNavigateBack = {},
                    onPrivacyClick = { privacyClicks += 1 },
                )
            }
        }

        val privacyTitle = composeTestRule.activity.getString(
            R.string.privacy_settings_activity_title,
        )
        composeTestRule.onNodeWithText(privacyTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(privacyTitle).performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, privacyClicks)
        }
    }

    @Test
    fun advancedSettings_shownWhenTopLevel() {
        var advancedClicks = 0

        composeTestRule.setContent {
            AppTheme {
                AppSettingsScreen(
                    appSettings = AppSettingsUiState(),
                    onAction = onAction,
                    onNavigateBack = {},
                    onPrivacyClick = {},
                    isTopLevel = true,
                    hasAdvancedSettings = true,
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
                    onAction = onAction,
                    onNavigateBack = {},
                    onPrivacyClick = {},
                    isTopLevel = false,
                )
            }
        }

        val advancedTitle = composeTestRule.activity.getString(R.string.advanced_settings)
        composeTestRule.onNodeWithText(advancedTitle).assertDoesNotExist()
    }

    private fun string(resId: Int): String {
        return composeTestRule.activity.getString(resId)
    }

    private fun scrollToText(text: String) {
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    private fun setContent(
        appSettings: AppSettingsUiState = AppSettingsUiState(),
    ) {
        composeTestRule.setContent {
            AppTheme {
                AppSettingsScreen(
                    appSettings = appSettings,
                    onAction = onAction,
                    onNavigateBack = {},
                    onPrivacyClick = {},
                )
            }
        }
    }
}
