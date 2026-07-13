package com.android.messaging.ui.appsettings.privacy.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isToggleable
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PrivacySettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var screenModel: SettingsScreenModel

    @Before
    fun setup() {
        screenModel = mockk(relaxed = true)
    }

    @Test
    fun youTubeLinkPreviewsToggle_displaysTitleSummaryAndDefaultsToOff() {
        setContent()

        val title = composeTestRule.activity.getString(
            R.string.youtube_link_previews_pref_title,
        )
        val summary = composeTestRule.activity.getString(
            R.string.youtube_link_previews_pref_summary,
        )
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(summary).assertIsDisplayed()
        composeTestRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun youTubeLinkPreviewsToggle_delegatesToScreenModel() {
        setContent()

        val title = composeTestRule.activity.getString(
            R.string.youtube_link_previews_pref_title,
        )
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.YouTubeLinkPreviewsChanged(true))
        }
    }

    @Test
    fun youTubeLinkPreviewsToggle_whenEnabled_delegatesDisable() {
        setContent(
            appSettings = AppSettingsUiState(youTubeLinkPreviewsEnabled = true),
        )

        val title = composeTestRule.activity.getString(
            R.string.youtube_link_previews_pref_title,
        )
        composeTestRule.onNodeWithText(title).performClick()

        verify(exactly = 1) {
            screenModel.onAction(Action.YouTubeLinkPreviewsChanged(false))
        }
    }

    private fun setContent(
        appSettings: AppSettingsUiState = AppSettingsUiState(),
    ) {
        composeTestRule.setContent {
            AppTheme {
                PrivacySettingsScreen(
                    appSettings = appSettings,
                    onAction = screenModel::onAction,
                    onNavigateBack = {},
                )
            }
        }
    }
}
