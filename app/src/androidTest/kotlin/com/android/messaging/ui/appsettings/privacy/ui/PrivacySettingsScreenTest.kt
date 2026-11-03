package com.android.messaging.ui.appsettings.privacy.ui

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.LinkOpeningMode
import com.android.messaging.ui.appsettings.general.model.AppSettingsAction as Action
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PrivacySettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var onAction: (Action) -> Unit

    @Before
    fun setup() {
        onAction = mockk(relaxed = true)
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
            onAction(Action.YouTubeLinkPreviewsChanged(true))
        }
    }

    @Test
    fun linkOpeningItem_showsCurrentMode() {
        setContent(appSettings = AppSettingsUiState(linkOpeningMode = LinkOpeningMode.DoNotOpen))

        composeTestRule.onNodeWithText(string(R.string.link_opening_pref_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.link_opening_off)).assertIsDisplayed()
    }

    @Test
    fun linkOpeningDialog_selectsCurrentModeAndConfirmsNewMode() {
        setContent()

        composeTestRule.onNodeWithText(string(R.string.link_opening_pref_title)).performClick()
        composeTestRule.onNode(linkOpeningOption(titleResId = R.string.link_opening_ask))
            .assertIsSelected()
        composeTestRule.onNode(linkOpeningOption(titleResId = R.string.link_opening_open))
            .performClick()
        composeTestRule.onNodeWithText(string(android.R.string.ok)).performClick()

        verify(exactly = 1) {
            onAction(Action.LinkOpeningModeChanged(LinkOpeningMode.OpenOnTap))
        }
        composeTestRule.onNodeWithText(string(android.R.string.ok)).assertDoesNotExist()
    }

    @Test
    fun linkOpeningDialog_cancelKeepsMode() {
        setContent()

        composeTestRule.onNodeWithText(string(R.string.link_opening_pref_title)).performClick()
        composeTestRule.onNode(linkOpeningOption(titleResId = R.string.link_opening_off))
            .performClick()
        composeTestRule.onNodeWithText(string(android.R.string.cancel)).performClick()

        verify(exactly = 0) { onAction(any<Action.LinkOpeningModeChanged>()) }
        composeTestRule.onNodeWithText(string(android.R.string.cancel)).assertDoesNotExist()
    }

    private fun linkOpeningOption(@StringRes titleResId: Int): SemanticsMatcher {
        return isSelectable() and hasText(string(titleResId))
    }

    private fun string(@StringRes resId: Int): String {
        return composeTestRule.activity.getString(resId)
    }

    private fun setContent(appSettings: AppSettingsUiState = AppSettingsUiState()) {
        composeTestRule.setContent {
            AppTheme {
                PrivacySettingsScreen(
                    appSettings = appSettings,
                    onAction = onAction,
                    onNavigateBack = {},
                )
            }
        }
    }
}
