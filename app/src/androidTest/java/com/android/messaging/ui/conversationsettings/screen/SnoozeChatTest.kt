package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction
import com.android.messaging.ui.conversationsettings.screen.support.ConversationSettingsTestBase
import com.android.messaging.ui.conversationsettings.screen.support.oneToOneState
import io.mockk.verify
import org.junit.Test

internal class SnoozeChatTest : ConversationSettingsTestBase() {

    @Test
    fun whenNotSnoozed_clickOpensChooser_confirmDispatchesSelectedOption() {
        renderScreen(oneToOneState(isSnoozed = false))

        composeTestRule
            .onNodeWithText(string(R.string.snooze_chat_setting_title))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(string(R.string.snooze_chat_option_one_hour))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string(R.string.snooze_chat_option_eight_hours))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(string(R.string.snooze_chat_dialog_confirm))
            .performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            screenModel.onAction(
                ConversationSettingsAction.SnoozeOptionSelected(SnoozeOption.EightHours),
            )
        }
        verify(exactly = 0) {
            screenModel.onAction(ConversationSettingsAction.UnsnoozeClicked)
        }
    }

    @Test
    fun whenSnoozed_clickDispatchesUnsnooze_andSkipsChooser() {
        renderScreen(oneToOneState(isSnoozed = true))

        composeTestRule
            .onNodeWithText(string(R.string.unsnooze_chat_setting_title))
            .performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            screenModel.onAction(ConversationSettingsAction.UnsnoozeClicked)
        }

        composeTestRule
            .onNodeWithText(string(R.string.snooze_chat_dialog_confirm))
            .assertDoesNotExist()

        verify(exactly = 0) {
            screenModel.onAction(
                match { it is ConversationSettingsAction.SnoozeOptionSelected },
            )
        }
    }

    @Test
    fun chooserCancel_doesNotDispatchSnooze() {
        renderScreen(oneToOneState(isSnoozed = false))

        composeTestRule
            .onNodeWithText(string(R.string.snooze_chat_setting_title))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(string(android.R.string.cancel))
            .performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 0) {
            screenModel.onAction(
                match { it is ConversationSettingsAction.SnoozeOptionSelected },
            )
        }
    }
}
